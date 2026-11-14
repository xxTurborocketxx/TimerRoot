# core/ — 6.1 kernel refactor baseline (vendored, MIT)

`timer98260.c` comes from **molanp/CVE-2026-98260-vivo-s60-poc** (MIT); its
header notes it is adapted from **woshigenm/CVE-2026-98260** (MIT) and other
ports. The original copyright notice is preserved; see `LICENSE.molanp`.

Why it was chosen as the refactor baseline:

- End-to-end verified on 6.1.145 (timer UAF → fake cred → SELinux permissive
  → full caps → ksud late-load);
- Single file, no external dependencies; includes tracefs event-ID
  self-discovery, linear-mapping base voting, a two-phase socket spray
  (phase 1 before exec, phase 2 triggered by the victim page being freed),
  `scan_page_alloc_ours` hit verification (pfn + spray-thread tid + order),
  and verify-and-repair read-back;
- Two endgames, shell and fake cred; the fake path plugs directly into the
  KernelSU late-load.

`tools/derive_offsets.py` / `tools/disasm_kernel.py` come from the same
repository and are used to derive the target device's offsets from
boot.img/Image.

## Build

```sh
CC=$ANDROID_NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/aarch64-linux-android24-clang
$CC -O2 -static -pthread -Wall -Wextra core/timer98260.c -o t98260
```

## TODO (refactor-98260 branch)

1. Extract the hard-coded PD2571 (6.1.145) offsets in `timer98260.c` into a
   profile file selected by `uname -r` (format following dada-cve's
   kernel_profiles, but content rewritten from this repository's own data);
2. Merge in this repository's existing vivo 6.1 offset tables and KSU
   handoff script;
3. Switch the APK side (Shizuku deploy/run) to the new core;
4. 5.10 support as second priority: reuse the 5.10 offsets and busy-poll
   killer already tested in this repository.
