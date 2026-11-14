# 0.12 refactor plan (branch refactor-98260, not merged into main)

This branch is only for the refactor and is **not merged into main**. main
sits at 0.11.67.

## Reference implementations and what to borrow

| Source | Key approach |
| --- | --- |
| `kzktor/ic-timer-lpe` main | Existing A/B/candidate chain, slide, mcast, KSU handoff |
| [kpgc10kai/TimerRoot](https://github.com/kpgc10kai/TimerRoot) (6.6/6.18) | BTF-derived offsets; at most 12 attempts; trace validates `free_seen=1 + burst hit=1` before reaping; rootd socket |
| `whooslizi/infinitycore-x200pro` | v2 verify-and-repair (read-back rewrite via /proc/self/status); v4 pre-zombie free watcher (another core drains the exec core's raw ring, matches by pfn, releases phase 2 on a hit); v5 all-core watcher + harvest; one attempt per boot |
| `molanp/CVE-2026-98260-vivo-s60-poc` | Two-phase spray: phase 1 before exec, phase 2 triggered by the victim pfn being freed; `scan_page_alloc_ours` validates both pfn and spray-thread tid; raw scan layout type@i-8 / pid@i-4 / pfn@i / order@i+8 |
| `nisu123f/dada-cve-AN00` | 5.10.236-android12-9 profile: `task->cred=0x780`, `real_cred=0x778`, `comm=0x790`, cred `copy_size=168`, `caps@40`, `user/user_ns/group_info @ 128/136/144`, `init_cred` and other offline addresses |

## Local 5.10 test findings (PD2219 / 5.10.233-android12-9)

- An 8 KiB SOCK_STREAM `unixstr` send is an order-1 page allocation each
  time, the same order as `posix_timers_cache` (264 B / 31 obj / 2 pages);
  order-3 comes from the DGRAM/kmalloc-8k path.
- Queue layout from disassembly: task SCHED `rb_root@tsk+0x758`,
  `cached leftmost@tsk+0x760`; `signal=*(tsk+0x7d8)`, signal SCHED
  `rb_root@sig+0x150`, `cached leftmost@sig+0x158`.
- `de_thread()`'s `posix_cpu_timers_exit()` `rb_erase`s the node and zeroes
  the cached leftmost — in the normal flow the queue is already empty when
  reaping.
- Only when the killer, at the instant the exec thread takes over the leader
  pid (`tgkill(tgid, original tid)` returns ESRCH), immediately `SIGKILL`s
  the **whole group** does the node survive until `release_task`
  (independently reproduced 6 times with 2 hits; at `__exit_signal`
  `root == cached leftmost == node address`).
- The existing `ic_killer` polls with `sched_yield()` and only `tgkill`s the
  leader — it never enters the window.

## New architecture (draft)

1. **probe / slide**: keep the existing implementation.
2. **Trace calibration**: the raw scan switches to matching by event type +
   field layout (the S60 `type@i-8 / pid@i-4 / pfn@i / order@i+8`), no
   longer relying on call_site/ptr pairs; the goal is to also get the
   victim's V and pfn on 5.10.
3. **Victim**: a single fork; a non-leader thread creates a
   `CLOCK_THREAD_CPUTIME_ID` (3600 s); the thread suspends before execve
   waiting for a go signal (x200pro's approach).
4. **Killer**: busy-poll the exec thread's original tid; on ESRCH →
   `SIGKILL` the whole group; victim does `setpgid(0,0)`.
5. **Two-phase spray**: phase 1 starts before the exec go signal; a free
   watcher on another core drains the exec core's raw ring and releases
   phase 2 as soon as it sees the victim pfn's `mm_page_free`.
6. **Hit verification**: `scan_page_alloc_ours` (pfn + spray-thread tid +
   order==1); on a miss keep the zombie and retry (one attempt per boot).
7. **Write primitive**: zero-writes (selinux_state-7 / panic_on_warn) and
   cred writes (`parent = task+0x780-8, tmp = init_cred`, then write
   0x778).
8. **Verify-and-repair**: after every write, read back the Uid/Gid quadruple
   from `/proc/self/status`; if non-zero, re-fire.
9. **Landing**: execve with the real cred, start rootd; KSU late-load
   reuses the existing handoff script.

## Branch strategy

- `main`: 0.11.67, stays as is;
- `refactor-98260`: this refactor branch, **not merged into main**; to be
  decided separately once verified.

## Milestone 1 (2026-11-06): full chain on QEMU, iQOO 12 6.1.145

- `core/profiles.h` landed: two entries, PD2571 and
  `IC_PROFILE_QEMU_IQOO12`; selected via `-DIC_PROFILE_<NAME>`, built
  directly by `core/build.sh OUT profile`.
- QEMU kernel: `testdata/download_kernel.Image`
  (6.1.145-android14-11-maybe-dirty, iQOO 12 16.2.11 boot.img).
- Re-derived from the Image: kallsyms (selinux_state/panic_on_warn/
  root_user/init_user_ns/ucounts/groups/TP), the `do_timer_create`
  allocation return site `0x1a12ac`, the `dup_task_struct` return site
  `0xad8d4`, BTF structure offsets.
- **Lesson from BTF/runtime-kernel mismatch**: measured `cred_jar/
  object_size` is 184 (`prepare_creds` first memcpy's 0xb0 then appends one
  qword), while the Image's BTF says 176 (GKI baseline). Cache sizes follow
  `/sys/kernel/slab/*/object_size`; uid is still at +4, user at +0x80
  (verified by disassembling `__sys_setuid`).
- Minimal init `harness/qemu_core_init.c`: mounts proc/sys/dev/tracefs, can
  run as `ic_uid0=1` or drop to uid 2000, runs `/exploit` and uses the
  console as the log.
- Two environment traps: NDK API24 static binaries' 8-byte TLS alignment →
  bionic refuses exec, switched to API29; in the bare initramfs, uid 2000's
  NOFILE is too small, the socket pool (6144 fds) squeezes later pipes into
  EMFILE, so the init raises NOFILE to 65536 before dropping privileges.
- Result: `free_seen=1 burst hit=1` → reap → zero-write `cred+4` →
  child `setuid(0)=0`, `resuid=0/0/2000`; only `/system/bin/sh` does not
  exist in the initramfs (exec errno=2).

## Milestone 2 (2026-11-06): same chain under the enforcing shell domain

- The same `IC_PROFILE_QEMU_IQOO12` core, placed into the shellboot harness
  (uid 2000, `u:r:shell:s0`, policy enforcing), result:
  `free_seen=1 burst hit=1` → reap → zero-write `cred+4` →
  child `setuid(0)=0`, `resuid=0/0/2000`.
- Previously the harness init mounted tracefs root-only, so the shell
  domain could not write `enable` and the exploit died within 2 seconds at
  page-event discovery. With `ic_tracefs_mount=1` it now `chmod_tree`s the
  whole tree to 0666/0777 (the on-device init achieves the same with
  gid=shell).
- Log channel: the init's env for the payload already contains
  `IC_LOG_PATH=/dev/ic-out` (a console clone with the null_device label,
  writable from the shell domain); the core redirects stdout/stderr at
  startup per `IC_LOG_PATH` (defaulting to `/dev/ic-out`).
- The only step not completed is that `/system/bin/sh` does not exist in the
  initramfs (ENOENT); on a real device this step is the landing.

## Milestone 3 (2026-11-06): hit verification + landing proof

- Realized that `free_seen=1 burst hit=1` only proves the spray caught the
  page, not that the `rb_erase` during reap really reached our target (when
  the killer loses the race the node has already been removed normally by
  de_thread, the plant still reports ok but not a single byte was written).
  The shell path now reads back the Uid from `/proc/<child>/status` after
  every plant and re-fires on a miss, up to 8 times; it only continues once
  verification passes, avoiding "all green but uid still 2000".
- Fixed a false negative in the success gate: `rep.euid` is a snapshot from
  before the wake-up (always 2000); the gate now looks at `rep.reuid` after
  setuid(0).
- Offline landing: placed a stand-in `/system/bin/sh` in the initramfs and,
  in the enforcing shell domain, achieved
  `[qsh] uid=0 euid=0 resuid=0/0/0 ctx=u:r:shell:s0 ... ROOT_SHELL_OK`.
  On a real device this step is exec'ing the real shell / the KSU handoff.

## Milestone 4 (2026-11-06): fake-cred layout differences and two-level read-back

- This kernel's `struct cred` starts with a 4-byte `usage` (`__sys_setuid`'s
  write points 0x4/0xc/0x14/0x1c, `cap_capable`'s cap_effective@0x38,
  `commit_creds`'s user@0x80 / user_ns@0x88 / ucounts@0x90 all match),
  whereas PD2571 has 8-byte usage (uid@8, caps@0x2c).
- The fake cred sits directly on the timer node: the node's
  `__rb_parent_color` (writes V+0x78) overlaps this layout's uid, so on a
  miss the child reads uid = high half of the pointer = -128; after a hit
  the uid is 0xffffff80, distinguishable from the caller's uid.
- Both fake_cred and fake_uid levels therefore switched to reading back the
  Uid from `/proc/<child>/status` before deciding to re-fire (8 attempts
  each); fake_cred treats "uid is no longer the caller's uid" as proof the
  pointer replacement succeeded.
- The offline fake full chain is not stably green yet (the shell path is
  stable); each attempt takes about 40 s under QEMU and needs a longer
  budget; real devices are not bound by this throughput limit.

## Milestone 5 (2026-11-06): fake/KSU path green offline

- The earlier fake_cred 8/8 misses were **verification looking at the wrong
  pointer**: `/proc/<pid>/status`'s Uid comes from `task->real_cred`
  (0x830), but the fake plant swaps `task->cred` (0x838); the two fork
  apart before fork normalization, so read-back always saw 2000.
- Switched to a peek protocol: in fake/fullshell/diag modes,
  wait_child_main reports `getuid()/geteuid()` once per byte received and
  only continues after receiving `G`; the parent reads it via
  `poke_child()`. fake_cred treats "uid != caller uid" as replacement
  success; fake_uid treats uid==0 as the zero-write landing.
- New fake_euid stage: the fake node's `rb_left` overlaps cred+0x10
  (sgid/euid), the euid is left with the target pointer's high half
  (0xffffff80), requiring one more zero-write at fc.A+0x14.
- The trailing `G` also produces an 8-byte peek reply that must be
  poll/read away first, or it sticks to the child report and fails the
  magic check.
- QEMU 6.1.145 + enforcing shell domain result:
  `fake cred A=...` → `fake_uid` → `fake_euid`, all first-try →
  child `uid=0 euid=0` → stand-in `/system/bin/sh`
  `[qsh] uid=0 euid=0 resuid=0/0/0 ... ROOT_SHELL_OK`.
