# TimerRoot — IC Timer LPE (CVE-2026-98260)

A vivo / iQOO / Motorola privilege-escalation tool for the POSIX CPU timer
UAF (CVE-2026-98260): from the shell domain to uid 0 via the Shizuku user
service, optionally handing root over to a KernelSU-family manager
(late-load of the manager's bundled `ksud` / kernel module).

The phone app is **TimerRoot**: one tap, no PC attached, no bootloader
unlock needed on the Motorola Edge 50 Ultra.

> **Fork note:** this repository extends the upstream vivo/iQOO tool
> (by Coolapk **@散花神木 (Sanhua Shenmu)**) with **Motorola Edge 50 Ultra**
> support, an English UI and additional app-side safety features.

## Discussion & support

- **Questions, feedback, device reports:** post in the
  [XDA discussion thread](https://xdaforums.com/t/tool-timerroot-one-tap-root-for-motorola-edge-50-ultra-no-bootloader-unlock-vivo-iqoo.4808957/) —
  that's the preferred place; I check it regularly and other users can help there too.
- **Bugs in the app or core:** open a
  [GitHub issue](https://github.com/xxTurborocketxx/ic-timer-lpe/issues)
  with the app log (`/sdcard/Download/TimerRoot-log-*.txt` or `ic-app-*.log`) attached.

For anything else (usage help, other firmware builds, feature ideas), the XDA
thread is the better place — issues are mainly for reproducible bugs.

## How to use

### 1. Install

- Download the APK from the [Releases](../../releases) page (built
  automatically for every `v*` tag), or build it from source (see
  [Build](#build)).
- Install [Shizuku](https://shizuku.rikka.app/) (Play Store or GitHub) and
  start it **on-device via Wireless debugging** (Developer options →
  Wireless debugging → pair, then start). Shizuku must be restarted this way
  after every reboot — TimerRoot notices a stopped Shizuku and opens the
  app for you.
- Grant TimerRoot the Shizuku permission.
- Optional: install a **ROOT manager** and pick it in the app's dropdown
  (see the [manager list](#root-managers)). Without a manager the run
  still escalates to shell-domain uid 0 and prints the proof — it just
  does not hand root over to a manager.
- If your model is not in the [supported list](#supported-devices),
  get the firmware's `boot.img` or a full OTA package ready and see
  [Parse offsets](#parse-offsets-builds-outside-the-list).

### 2. Run

1. **Reboot the phone.** A fresh boot is required before every full run —
   the exploit consumes its one-shot state per boot.
2. Open TimerRoot, pick the ROOT manager (or leave it on
   *Auto-detect*), and tap **One-tap jailbreak**.
3. A preflight check warns when the system is not quiet — 1-min load
   ≥ 6, boot younger than 150 s, or the load could not be measured —
   and asks you to wait. A quiet window measurably improves the
   success rate.
4. The button shows the live stage (e.g. `7/12 zero-write vr.ko`) and
   ends on `Done` / `Failed`.

### 3. What success looks like

- The status card shows **“Jailbreak active this boot”** (live, no app
  restart needed) and the button turns red;
- The log contains the root proof (`LPE-PROOF`, `uid=0(root)`, and — with a
  manager — `module resident`);
- The ROOT manager **opens automatically** after the run and shows
  **Working — Jailbreak mode**;
- A **“Root is live”** notification appears when the run finishes (tap it
  to open the manager);
- SELinux is back to enforcing after the run.

Root lives until the next reboot. **One run per boot**: after a run the
button switches to “Reboot required (already ran)”, and a second attempt
in the same boot cannot succeed (leftover core processes cannot be
`kill -9`'d; only a reboot clears them).

## Supported devices

Built-in offsets — model + firmware version match — no parsing required:

| Model | Versions |
| --- | --- |
| PD2506 / V2506 (Y500) | 16.1.15.0, 16.1.13.2, 16.1.17.0 |
| PD2516 / V2516 (Y500 Pro) | 16.1.11.0 |
| PD2307 / PD2329 / V2307 (iQOO 12) | 16.2.11, 16.2.20.3, 16.2.20.7, 16.2.21.1 |
| PD2417 / V2417 (Z9 Turbo+) | 16.2.14.1 |
| PD2324 / V2324 (X100 series) | 16.2.12.0, 16.2.13.1 |
| PD2352 / V2352 | 16.2.17.0 |
| PD2425 / V2425 | 16.1.17.10, 16.1.16.2 |
| DPD2329 / PD2329 | 16.2.9.2 |
| PD2339 / V2339 | 16.2.13.1 |
| PD2309 / V2309 | 16.2.15.0 |
| PD2445 / V2445 | 16.1.16.0 |
| Motorola Edge 50 Ultra (`ctwo`) | GKI `6.1.157-android14-11`, build `36e2a-a4560` (`CTWOMOTO` profile) |

### Parse offsets (builds outside the list)

Tap **Parse offsets** and pick the model's `boot.img` or a **full OTA
package (.zip)**:

- Full packages are extracted automatically (`payload.bin`, zip64 + zstd),
  and the package's device/version is compared against the phone first;
  mismatches are refused;
- `init_boot` / `vendor_boot` contain no kernel and are refused;
- Kernel images other than 6.1.145 or 6.1.157 (Edge 50 Ultra GKI) are
  refused;
- On success `/data/local/tmp/ic-syms.txt` is generated — then run the
  one-tap jailbreak as usual.

### ROOT managers

The dropdown (and the handoff script) knows these managers — install one,
pick it or use *Auto-detect*:

KernelSU, KernelSU-Next, SukiSU Ultra, BakaSU, ReSukiSU (old package
name), SakiSU, FinaSU, 7kimisu (two package ids), vivoKSU.

With **safe mode** checked, KSU modules stay disabled during the run (the
manager's kernel module is still late-loaded).

## How it works

The core (`core/timer98260.c`, static C99, no root needed at start):

1. **Trace oracle** — the de-tagged `__tracepoint_sys_exit` address is
   derived from the timer call site: on 6.1, `do_timer_create` sits at an
   image offset < 2 MiB and the KASLR slide is 2 MiB-aligned, so
   `slide = site0 - (0xffffffc008000000 + low21)` — no kallsyms needed;
   when kallsyms is readable the absolute address is used directly;
2. **Cred leak → page spray → hit read-back → write primitive**;
3. **Shell or KSU endgame** — shell-domain uid 0 with proof commands, or
   a fake-cred full-caps root that hands over to the ROOT manager
   (`ksu_handoff.sh`, `ksu` mode).

Per-kernel constants live in `core/profiles.h` (selected via
`-DIC_PROFILE_*`); anchor tables in `core/anchors.h`; the object symbols
needed for fake cred in `core/extra_syms.h` (exact values for verified
builds — including the Edge 50 Ultra GKI 6.1.157, where the layout
differs from the vivo builds — take precedence over the derived ones).
The exploit writes, then **verifies** (read-back of uid, peek protocol on
the fake path) and retries on a miss (≤ 8 times).

## The app

`apk-gradle/` — Kotlin + Jetpack Compose, three tabs (Home / Log / About),
UI ported from [Shark](https://github.com/Yunnijian/Shark) (GPL-3.0):

- Status card, one-tap jailbreak with live stage display, ROOT manager
  dropdown, safe-mode switch, in-app manual, parse-offsets,
  clean-KSU, diagnostics;
- **Quiet-window preflight** before every run (load / boot age, fail-closed);
- **Live status**: “Jailbreak active this boot” updates as soon as the run
  ends — no app restart needed;
- **Auto-opens**: the app opens Shizuku for you when it is not running,
  and the ROOT manager after a successful run;
- **Readable log**: every line is time-stamped and color-coded (green
  proof / red failure / grey info), tapping a line copies it, the share
  button saves the log to Download (`TimerRoot-log-*.txt`) or the share
  sheet, a trash button clears it, and **run start / end banners** frame
  each attempt with its duration;
- **One-run-per-boot lock**: reruns in the same boot are blocked with an
  explanation dialog;
- Offset preflight: `ic-timer info` (read-only) before a run; a
  missing-offsets dialog points to parse-offsets;
- Parse offsets: boot.img or full OTA (zip64 + payload + zstd), boot
  partition auto-extraction, device/version cross-checks.

The backend is a Shizuku user service (`IcService`); `IC_SYMS_FILE` /
`IC_SAFE_MODE` / `IC_KSU` behave as in the old 0.12 tool.

## Edge 50 Ultra port (this fork)

Extends the tool to the **Motorola Edge 50 Ultra** (`ctwo`): Android 17
userspace on a stock, **bootloader-locked** GKI kernel
`6.1.157-android14-11`. What changed:

- **`CTWOMOTO` profile** in `core/profiles.h`: static addresses,
  cred/slab geometry and the rmtfs ioctl call-site data for the Motorola
  GKI build, plus built-in offsets (`ctwo_4220_links`) — the verified
  build works straight after a factory reset, no `ic-syms.txt` needed;
- **Direct late-load payload**: the payload drives the KernelSU late-load
  itself (`ksud late-load --kmi android14-6.1 --allow-shell`) and clears
  stale `/data/adb/ksud` state first, so the manager's Superuser tab no
  longer hangs after a run (KernelSU-Next #1716 workaround);
- **Proof-marker success detection**: a run counts as successful once the
  root-proof markers appear (`uid=0(root)`, `LPE-PROOF`, module resident);
- **English UI**, launcher name **TimerRoot**.

Verified on device: shell → uid 0 proof, then the KernelSU-Next manager
**“Working [Jailbreak mode]”** via late-loaded GKI LKM — no bootloader
unlock required.

## Repo layout

| Path | Purpose |
| --- | --- |
| `apk-gradle/` | TimerRoot app (Gradle / Kotlin / Compose). Shizuku user service `IcService`; assets: `ic-timer` core binary, `run_core.sh`, `ksu_handoff.sh`, `safemode.sh`, `cleanup.sh`, manual |
| `core/` | The 98260 core: `timer98260.c` (main body), `profiles.h` / `anchors.h` / `extra_syms.h`, `ic_syms.c` (on-device symbol parser), `tools/` host-side generators, `build.sh` (NDK) |
| `tool/` | Host-side helpers: `kallsyms_py.py`, `parse_offsets.py`, `syms_host.c`, patched aircompressor sources |
| `device/` | Device-side development scripts (manual runs, offset collection, vote helpers) |
| `docs/` | Technique documents (translated) |
| `.github/workflows/` | `build-apk` (CI: unit tests + debug/release builds on push), `release` (signed `TimerRoot-<tag>.apk` on tag push) |

## Build

```sh
core/build.sh /tmp/t98260 ctwomoto    # NDK r27, API 29 (API 24 TLS
                                      # alignment is rejected by bionic)
cp /tmp/t98260 apk-gradle/app/src/main/assets/ic-timer
cd apk-gradle && ./gradlew :app:assembleRelease   # AGP 9.4.1 / Kotlin 2.4.20
```

Profile selectors: `pd2571` (default, vivo 6.1.145), `qemu_iqoo12` (QEMU
test image), `ctwomoto` (Motorola Edge 50 Ultra GKI 6.1.157).

`core/tools/gen_anchors.py <Image>` regenerates the anchor tables from a
kernel image; `core/tools/verify_tables.py` cross-checks them against
parse results. Tagging a release (`v*`) makes the `release` workflow
build and publish **`TimerRoot-<tag>.apk`** automatically — signed with
the project keystore when the repository secrets are configured,
otherwise debug-signed.

## Verification

- QEMU (iQOO 12 6.1.145 image): shell path `ROOT_SHELL_OK`; fake path
  `uid=0 euid=0` + payload execution;
- Real devices: PD2329 / PD2307 / PD2425 shell-domain uid 0; PD2425
  `late-load rc=0`, kernelsu module resident;
- Motorola Edge 50 Ultra (`ctwo`): uid 0 proof + KernelSU-Next late-load,
  `kernelsu` module resident (bootloader locked).

## Known limitations

- Filesystem layout and `struct cred` offsets of untested builds are
  derived from known builds; on an untested build a failure shows up as
  `fork/exec errno=12` (ENOMEM) — it will not panic;
- Upstream profiles fit 6.1.145 kernels; this fork adds the Edge 50
  Ultra's 6.1.157 GKI build;
- One run per boot; a second attempt in the same boot is refused.

## Credits & license

Upstream tool by Coolapk **@散花神木 (Sanhua Shenmu)** (GPL-3.0 core,
MIT 98260 core, see `NOTICE`). UI ported from
[Shark](https://github.com/Yunnijian/Shark) (GPL-3.0) — this project as a
whole is distributed under **GPL-3.0**; see `LICENSE` and `NOTICE`.

Edge 50 Ultra port and app safety/status features by this fork.

## Disclaimer

This tool is for **security research and testing on devices you own**
only. Before using it, confirm you are authorized to operate the target
device; use on unauthorized devices or for any illegal purpose is
strictly forbidden.

- This software is provided "as is", without any express or implied
  warranty;
- The privilege-escalation process involves a kernel write primitive and
  can cause the device to reboot, suffer data corruption, or brick.
  Back up before running and make sure you have a recovery path (stock
  boot.img / full OTA);
- Any consequence of using this tool is the user's own responsibility;
- Obey local laws and regulations as well as the device vendor's terms
  of use.
