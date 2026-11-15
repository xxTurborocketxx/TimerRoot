# User Manual

This software is free and open source, by Coolapk @散花神木 (Sanhua Shenmu).
This fork adds app-side safety and status features (one-run-per-boot lock,
live jailbreak status).

## Run rules

- **Reboot the phone** before every full run;
- **One run per boot**: after any run the button switches to "Reboot
  required (already ran)" and a second tap shows an explanation instead of
  starting again. A second attempt in the same boot cannot succeed — the
  exploit leaves a zombie behind and consumes its one-shot state — and in
  the worst case it panics the kernel;
- Leftover ic-timer processes after a run **cannot be kill -9'd** — only a
  reboot clears them;
- Taking over KSU requires the matching manager APK to be installed first
  (the manager's own kernel module is loaded via late-load).

## What success looks like

- The status card shows **"Jailbreak active this boot"** and the button
  turns red — root is live until the next reboot;
- The log contains the root proof (`LPE-PROOF`, `uid=0(root)`,
  `module resident`) and the stage display ends on "12/12 Done";
- The manager (KernelSU / KSU-Next) opens automatically when the run
  finishes and shows **Working — Jailbreak mode**. A "manager autostart
  skipped" line only means the handoff's own start attempt did not fire —
  the app opens the manager itself afterwards; open it yourself only if
  it does not appear;
- SELinux is back to **enforcing** after the run (the manager may show a
  stale value until it is restarted).

## Built-in offsets (model + version match)

- PD2506 / V2506 (Y500): 16.1.15.0, 16.1.13.2, 16.1.17.0
- PD2516 / V2516 (Y500 Pro): 16.1.11.0
- PD2307 / PD2329 / V2307 (iQOO 12): 16.2.11, 16.2.20.3, 16.2.20.7, 16.2.21.1
- PD2417 / V2417 (Z9 Turbo+): 16.2.14.1
- PD2324 / V2324A / V2324HA / V2324H / PD2324J (X100 series): 16.2.12.0, 16.2.13.1
- PD2352 / V2352: 16.2.17.0
- PD2425 / V2425: 16.1.17.10, 16.1.16.2
- DPD2329 / PD2329: 16.2.9.2
- PD2339 / V2339: 16.2.13.1
- PD2309 / V2309: 16.2.15.0
- PD2445 / V2445: 16.1.16.0
- Motorola Edge 50 Ultra (ctwo): A171SUVH.42-20-1 (kernel 6.1.157)

Models in this list have all the object addresses needed for the KSU handoff
built in — no parsing required.

Motorola Edge 50 Ultra (ctwo, GKI kernel 6.1.157): build A171SUVH.42-20-1 is
fully built in — the tool works straight after a factory reset, with no
ic-syms.txt present. After an OTA / firmware update the built-in entry no
longer matches the new build: delete a stale ic-syms.txt and tap "Parse
offsets" once with the new boot.img.

## Parse offsets (builds outside the list)

Tap "Parse offsets" and pick the model's **boot.img** or a **full OTA
package (.zip)**:

- For a full package, the boot partition is extracted automatically from
  `payload.bin` (zip64 + zstd decompression), and the device and version
  inside the package are compared against the current phone first; a
  mismatch is refused outright;
- `init_boot` / `vendor_boot` contain no kernel and are refused;
- A kernel image that is neither 6.1.145 nor 6.1.157 (Edge 50 Ultra GKI) is also refused;
- On success, `/data/local/tmp/ic-syms.txt` is generated; afterwards tap
  one-tap jailbreak.

## Safe mode

When checked, all KSU modules are disabled during privilege escalation
(the kernel module itself is still loaded via ksud late-load; existing
KSU modules stay off).
