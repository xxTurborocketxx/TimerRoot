# Changelog

Release bodies are generated from this file: the "What's new" section of a
tagged release is the section below its version heading, published verbatim
by the release workflow. Newest first.

## v0.30

Stable cut of the nightly-line betas (v0.29.1, v0.29.2); the notes-tap
re-check from the first beta was reverted by design.

- **Scrollable release notes**: longer "What's new" bodies scroll instead
  of clipping.
- **Versioned notes title**: the dialog shows "What's new in v0.30", so
  releases stay distinguishable.
- **Bold bullet lead-ins**: each bullet starts with a bold lead-in for
  quick scanning.
- **Direct install**: with the Shizuku service running, the updater
  installs a downloaded update silently, the way adb installs an APK -
  no confirmation dialog, no unknown-sources detour. If the shell
  install fails, the system installer opens as before.

## v0.29

Stable cut of the two beta features from the nightly line (beta-tested as
v0.28.1).

- **Out-of-scope confirm dialog**: running on a device or kernel that is
  not known-supported now asks for one explicit confirmation per app
  session before the core starts ("Out of scope - confirm to run"). "Run
  anyway" re-enters the preflight, so the busy-load warning still applies;
  "Not today" cancels the run. The startup device notice stays; the
  exploit chain, the one-run-per-boot lock and the preflight hard gates
  are unchanged.
- **In-app changelog polish**: What's new now shows only the release's
  What's-new section (about block and commit list removed), refreshes on
  every tap, renders structured bullets, and heads the dialog with the
  latest release and its channel.
- CI: the continuous-build artifact is now named TimerRoot-apk.

## v0.28

- boot.img check relaxed: on a fingerprint mismatch TimerRoot now compares
  kernel version banners and accepts images from sibling builds of the same
  GKI kernel (a kernel mismatch or an unreadable banner still aborts the
  parse, fail-closed).
- The updater and all in-app project links now point at the TimerRoot
  repository natively; the update channel no longer relies on GitHub
  redirects.
- CI hardening: unit tests must pass before a release is published, the
  release workflow refuses to publish without the signing secrets (no more
  debug-signed releases), and pull requests are built and tested.
- README: the "it will not panic" claim is now an honest risk statement.

## v0.27.1

First release on the **nightly** development line. Content-identical to
v0.27, serving as the beta-channel smoke test:

- Version bump to 0.27.1 (versionCode 223), no functional changes.
- If you are on the stable channel, the updater correctly hides this
  release; toggle "Beta channel" in the update settings to see it.

## v0.27

- **Beta channel**: new toggle in the update settings. Opt in to prerelease
  updates; off by default (stable only).
- **Channel labels**: update dialog, up-to-date toast and About now show
  `(stable)` / `(beta)`, derived from the GitHub prerelease flag.
- **TimerRoot rename**: repo, NOTICE and in-app legal texts renamed from
  ic-timer-lpe.
- **Prerelease-aware CI**: 3-segment tags (e.g. `v0.27.1`) now publish as
  pre-releases automatically; 2-segment tags as stable.

## v0.26.2

- **In-app changelog**: a "What's new" entry in the About tab shows the
  latest release's notes; the update dialog now shows what changed before
  you install.
- Release notes come from the GitHub release body, rendered as plain text
  with tappable links.

The update installs in place over 0.26.0/0.26.1 (same signature) - no
uninstall, data and the one-run-per-boot lock are preserved.

## Older versions

v0.22.0 - v0.26.1 predate this changelog; see the
[releases page](https://github.com/xxTurborocketxx/TimerRoot/releases) and
the compare links in each release.
