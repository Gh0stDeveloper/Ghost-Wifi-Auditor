# Ghost WiFi Auditor — Development Roadmap

**Project:** [Gh0stDeveloper/Ghost-Wifi-Auditor](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor)  
**Owner / Developer:** Ghost Developer  
**Working branch:** `main`  
**Baseline:** `0.3.0-alpha` M2 USB discovery development (2026-11-19; CI and physical OTG tests required)  
**Status (2026-11-19):** M1 v0.2 source committed; GitHub Actions [run #44558588666](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44558588666) passed **unit tests**, **assembleDebug** and artifact upload for code commit `539ffe2`. No real-device QA or signed-release build has been performed yet.

> This document is a development plan, not a claim that planned functions already work. "Compiled" does not mean "tested on device." No unsupported driver or monitor capability is to be labeled as available.

## Current verified scope — M0

- [x] Android Studio project with Kotlin / Jetpack Compose and original dragon design.
- [x] Graphite AMOLED UI: Home, Audit, Devices, Settings, nested About.
- [x] Root detection through an on-demand `su -c id`-style probe (result is for the current session only).
- [x] Inspection of advertised monitor modes using `iw` (not a functional monitor test).
- [x] Android USB Host enumeration and VID/PID display (not an external Wi-Fi driver).
- [x] Device and adapter references with compatibility disclaimers.
- [x] Native C++/JNI recognition of PCAP / PCAPNG signatures (not a packet parser).
- [x] Links to official Telegram channels and the GitHub source from About.
- [x] R8 configuration and optional release signing environment-variable support.
- [x] GitHub Actions `assembleDebug` success and uploaded `ghost-wifi-debug` artifact, run [44498823633](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44498823633).
- [x] User installed the M1 Debug APK on Redmi 14C and confirmed that root check changes correctly between no superuser and granted root (manual feedback; other phones not yet tested).
- [ ] Install and run the M1 Debug APK from [run #44558588666](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44558588666) on real test devices.
- [ ] Run the signed **release** pipeline using configured repository secrets and validate signature continuity.
- [ ] Add automated test coverage; no `src/test` or `src/androidTest` test suite is present at this baseline.

## M1 — Reliable diagnostics and device matrix (next milestone)

**Goal:** Trustworthy status for every Android model, rather than merely rendering a static compatibility card.

- [x] Create a typed diagnostic model: `SUPPORTED / UNSUPPORTED / UNKNOWN / ERROR`, with evidence and timestamp.
- [x] Detect root *at runtime*, with no assumption that temporary root survives a reboot. Handle permission denial, missing `su`, timeouts and package-manager differences.
- [x] Check Android release, build fingerprint, SKU, kernel, driver, firmware and interface names without exposing device identifiers in exported reports.
- [x] Discover actual `iw` location and surface stderr/exit codes; distinguish "monitor advertised" from "operational".
- [ ] Probe monitor capabilities **without mutating active Wi-Fi interfaces**. A real activation test must be user-approved, limited and reversible on supported hardware.
- [x] Keep a versioned local JSON device database with evidence links, date, driver/build constraints and status `unverified` by default. Support a separate `other device` profile.
- [x] Implement per-model root guidance: temporary / persistent / no root; never auto-run exploits or firmware flash commands.

**Acceptance (partial):** the parser distinguishes monitor advertised / not advertised / unknown and test cases cover malformed/missing data. Stored root verification is deliberately avoided so it cannot survive app recreation or reboot. Real-device acceptance on Redmi, Samsung, Honor is still pending. A real monitor activation test remains out of scope until a safe reversible design is verified.

## M2 — Multi-adapter USB OTG support

**Goal:** Detect arbitrary adapters safely; never assume a product name implies a chipset.

- [x] USB attach/detach lifecycle via foreground receiver, explicit user-granted USB permission when requested, manufacturer/product metadata and VID:PID (requires hardware QA).
- [x] Versioned USB chipset reference catalog in app assets, with evidence URLs, driver names, firmware and hardware revision warnings. Existing adapter cards provide photos and attribution.
- [x] Show loaded-module *evidence* separately from USB permission; explain OTG/power limitations and unknown adapters. Confirming a driver is bound to a specific USB interface remains pending.
- [x] Gate capture availability: all USB adapters remain unverified until a real controller + firmware + capture path are validated. Never load unknown `.ko` modules.
- [ ] Add compatibility tests using actual AR9271 and MT7612U hardware as available.

**Acceptance (pending real device QA):** app detects attach/detach and distinguishes "USB recognized" from "Wi-Fi capture supported", including for an unknown adapter. CI tests cover VID/PID mapping and conservative driver-status wording. Real physical AR9271/MT7612U and powered hub checks are not yet completed.

## M3 — Passive discovery and authorized capture engine

**Goal:** Real audit sessions where the hardware and permissions actually support capture.

- [ ] Separate a clearly scoped session controller from UI; foreground-service and Android notification lifecycle.
- [ ] Scope and authorization confirmation before any active operation; no unauthorised network intrusion.
- [ ] Capture only with validated monitor-capable adapter/internal driver or an approved Linux backend.
- [ ] Provide session start/stop, duration, channel, capture size, destination and clean cancellation.
- [ ] Parse classic PCAP and PCAPNG block/record structures in C++/JNI with bounds checks, malformed-file handling and tests.
- [ ] Store capture sessions locally, permit import/export and protect captured content from unintentional sharing.

**Acceptance:** captures from an explicitly authorised lab environment can be opened, verified, stopped and exported without crashing. Unsupported devices show a clear limitation, not a fake success.

## M4 — Remote Linux / SSH backend

**Goal:** Make the Android app a working secure control panel for compatible Linux hardware.

- [ ] SSH pairing using keys, known-host verification and confirmation of new host keys.
- [ ] Minimal, audited Linux-side agent; no arbitrary shell command execution through UI.
- [ ] Detect available wireless interfaces, controllers and installed analysis tools on the Linux host.
- [ ] Show remote session state, reconnect, progress and error messages.
- [ ] Only request user-approved capture operations within the defined scope; never embed keys or passwords in the APK.

**Acceptance:** connect to a controlled test host, enumerate its supported interfaces, run a permitted passive capture, download it and disconnect safely.

## M5 — Reports, history, internationalization

**Goal:** Useful, understandable results rather than raw console output.

- [ ] Searchable audit history, session labels, timestamps, device profile and selected adapter.
- [ ] Report export in JSON and PDF, with sensitive identifiers redacted by default.
- [ ] Structured observations: encryption advertised, WPS advertised (not a confirmed vulnerability), signal strength, channel and adapter capabilities.
- [ ] Spanish and English translations initially; add other languages through Android resources when quality assured.
- [ ] Optional offline mode, accessibility, scalable typography and light/dark theme settings (AMOLED default).
- [ ] About: Ghost Developer, version/build, official channel, community, repository, third-party acknowledgments and licenses.

**Acceptance:** results reproduce consistently from a saved session, export cleanly and remain understandable offline.

## M6 — Security hardening, QA and signed release

- [ ] Unit tests for shell parsing, USB chipset mapping and PCAP/PCAPNG.
- [ ] Compose UI tests and instrumentation on Xiaomi Redmi 14C, Samsung, Honor and additional Android hardware.
- [ ] Validate USB attach/detach, root revoke/reboot, airplane mode, sleep/background and battery-optimization behavior.
- [ ] Review root subprocess isolation, argument handling, file input validation, privacy/security and secure storage.
- [ ] CI: static checks, unit tests, debug build, signed release, artifact retention and checksums.
- [ ] Reuse the **Ghost WiFi Auditor-specific** persistent keystore via private GitHub Secrets. Never commit the keystore, passwords or signing credentials; validate signing identity after updates.
- [ ] Release notes, user documentation, troubleshooting and version migration checks.
- [ ] Verify supported scenarios on real devices before claiming stable `1.0.0`.

**Acceptance for 1.0.0:** Every advertised operation works in at least one documented hardware configuration, runs have predictable error handling, the APK is persistently signed, CI is green and real-device QA is recorded. Hardware-dependent features stay unavailable where unsupported.

## High-priority product recommendations

1. **Compatibility assistant** showing root/session status, driver capability and exact missing dependency.
2. **Adapter profiles** with real chipset and revision detection and visual references.
3. **Guided capture sessions** with explicit scope, status and export.
4. **SSH mode** for smartphones without native monitor support.
5. **PCAP viewer and structured report**, not just recognition of a file signature.
6. **Diagnostic export** that excludes Wi-Fi keys, private credentials and personally identifying device data by default.
7. **Update and issue links** to GitHub Releases / Issues; do not introduce forced updates or embedded secrets.

## Non-goals

- Claiming a root manager installs kernel monitor support.
- Blind module flashing or proprietary firmware command probing.
- Automated attacks against third-party networks, password guessing or undocumented exploitation.
- Claiming universal Android compatibility.

## Delivery discipline

Every release must include: **version/build, branch/commit, what changed (screen/module), what remained pending, tests run, CI status, limitations and exact devices/firmware used in QA**. This prevents confusing visual previews with completed functionality.


## M2 QA checklist for tester (v0.3.0-alpha)

- Run app with **no** USB dongle: Devices → USB OTG must say no device detected.
- Attach an adapter via USB-C OTG: USB ID, name, interface count, permission status and chipset candidate should appear; never show “monitor supported” from VID/PID alone.
- Tap **Solicitar permiso**: Android displays USB permission dialog; after approval, app refreshes to “Permiso USB concedido”. Denial must not crash.
- Disconnect/reconnect: list updates and permission is rechecked; reopening Devices and manual refresh also work.
- Test unknown USB HID or storage: shown as unknown chipset; must not falsely identify it as a Wi-Fi adapter.
- Inspect app after phone reboot: root state must require a fresh check, as with M1.
- R8-minified debug build passes `testDebugUnitTest` and `assembleDebug` and emits `mapping.txt`.
- Verify APK signature: GitHub runner’s ordinary debug key is **not persistent** across workflow runs. For install-over updates, configure the Ghost-specific private keystore Secrets; do not commit keys.
