# User Manual

This software is free and open source, by Coolapk @散花神木 (Sanhua Shenmu).

## Run rules

- **Reboot the phone** before every full run;
- Leftover ic-timer processes after a run **cannot be kill -9'd** — only a
  reboot clears them;
- Taking over KSU requires the matching manager APK to be installed first
  (the manager's own kernel module is loaded via late-load).

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

Models in this list have all the object addresses needed for the KSU handoff
built in — no parsing required.

## Parse offsets (builds outside the list)

Tap "Parse offsets" and pick the model's **boot.img** or a **full OTA
package (.zip)**:

- For a full package, the boot partition is extracted automatically from
  `payload.bin` (zip64 + zstd decompression), and the device and version
  inside the package are compared against the current phone first; a
  mismatch is refused outright;
- `init_boot` / `vendor_boot` contain no kernel and are refused;
- A kernel image that is not 6.1.145 is also refused;
- On success, `/data/local/tmp/ic-syms.txt` is generated; afterwards tap
  one-tap jailbreak.

## Safe mode

When checked, all KSU modules are disabled during privilege escalation
(done through the ksud handoff script bundled with the manager).
