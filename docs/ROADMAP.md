# Ghost WiFi Auditor — Development Roadmap

**Project:** [Gh0stDeveloper/Ghost-Wifi-Auditor](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor)  
**Owner / Developer:** Ghost Developer  
**Working branch:** `main`  
**Baseline:** `0.7.0-alpha` M6 security and QA hardening (automated CI pending, physical QA deferred)  
**Status (2026-11-19):** M1 v0.2 passed CI, and user confirmed root checks on a Redmi 14C. **M2 v0.3** CI [run #44563903907](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44563903907) passed unit tests, R8-minified Debug build, R8 mapping check and APK artifact upload (commit `5406aaa`). Persistent signing secrets were not configured; signed-release steps were skipped. Physical USB adapter QA is pending.

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
- [ ] Scope and authorization confirmation before any **live capture** operation; currently no live capture is offered.
- [ ] Capture only with validated monitor-capable adapter/internal driver or an approved Linux backend.
- [ ] Provide session start/stop, duration, channel, capture size, destination and clean cancellation.
- [x] Parse classic PCAP and PCAPNG records/blocks via streaming C++/JNI, with bounds checks, truncated-file handling and standalone native tests.
- [x] Store **imported offline** capture sessions privately; allow user-initiated import, report export, raw export and deletion. Live-capture sessions not implemented.

**Acceptance:** captures from an explicitly authorised lab environment can be opened, verified, stopped and exported without crashing. Unsupported devices show a clear limitation, not a fake success.

## M4 — Remote Linux / SSH backend

**Goal:** Make the Android app a working secure control panel for compatible Linux hardware.

- [x] Host-key pinning through a user-supplied, independently verified known_hosts entry; password or private-key SSH login, no auto-trust (real-host QA pending).
- [x] Minimal Python Linux agent with a strict list/capture/cleanup command allowlist and parameter limits; no arbitrary command UI.
- [x] Read-only Linux iw interface diagnostic plus kernel, hostname and tcpdump presence through SSH (pending real-host QA).
- [x] Show verified per-operation SSH connection results, status and error messages. Continuous SSH session / automatic reconnect not implemented.
- [x] Require explicit authorization and confirmed monitor interface for bounded capture; SFTP download and M3 history import. Credentials are not embedded or persisted (pending real-host QA).

**Acceptance:** connect to a controlled test host, enumerate its supported interfaces, run a permitted passive capture, download it and disconnect safely.

## M5 — Reports, history, internationalization

**Goal:** Useful, understandable results rather than raw console output.

- [x] Searchable audit history, editable labels/notes, timestamps, origin, local device description and selected adapter metadata (physical QA pending).
- [x] User-initiated JSON/PDF export with identifying fields hidden by default; original PCAP export remains unredacted and explicitly labelled (visual PDF QA pending).
- [x] Structured report distinguishes measured packet counts from NOT_MEASURED encryption/WPS/channel/RSSI; no fabricated security findings. Parsing actual wireless parameters remains pending a validated scanning/capture backend.
- [x] Spanish/English selector and translation of primary screens; full technical-text localization and Android resource audit remain pending.
- [x] Offline-only setting for SSH/remote photos, scalable typography and AMOLED/graphite/light themes. Device accessibility and visual QA still pending.
- [x] About lists Ghost Developer, version, official channel/community, repository and third-party libraries; license verification and detailed notices before release pending.

**Acceptance:** results reproduce consistently from a saved session, export cleanly and remain understandable offline.

## M6 — Security hardening and release preparation (0.7.0-alpha)

**Goal:** reduce security defects and establish repeatable verification. The app is **not** called stable until manual device tests and signing continuity pass.

- [x] Dedicated input policy and JUnit tests for SSH identity, interface, capture duration, remote ID, path and size.
- [x] Limit SSH output and local diagnostic reads, require offline-only enforcement in network layer and clean up the remote PCAP on failures.
- [x] Add signature certificate SHA-256 to About, without exposing keystores.
- [x] Add Android instrumentation smoke-test source (compilation in CI; execution on real hardware/emulator later).
- [x] Add native parser malformed-input corpus and undefined-behavior sanitizer job.
- [x] Add static security regression checks, Android Lint, R8 artifact SHA256SUMS, APK signer verification and optional signer certificate pinning.
- [x] Document threat model, signing process, hardware test matrix and release criteria.
- [ ] Validate on Redmi 14C, Samsung, Honor and other Android hardware.
- [ ] Verify Linux SSH, SFTP and real monitor interface with owned compatible equipment.
- [ ] Configure **Ghost WiFi Auditor's original** persistent keystore in GitHub Actions Secrets; test update-compatible signed APK.
- [ ] Accessibility, offline lifecycle, visual review, screenshots and final third-party license audit.

**M6 result:** a stronger alpha build, **not** a certified Wi-Fi audit engine nor release 1.0.

## M7 — Operational wireless capture backends

**Goal:** perform **real, explicitly authorized captures**, never simulated results.

- [ ] Linux/USB capture backend capability negotiation and preflight reports (driver, firmware, interface, permissions).
- [ ] Controlled session lifecycle: start/stop, timing, progress, cancellation, storage limits and cleanup.
- [ ] Implement validated Android internal/USB capture only on specific supported kernel/chipset combinations, with safe fallback to remote Linux.
- [ ] Detect lack of monitor mode and explain the hardware dependency; root alone is insufficient.
- [ ] QA using actual monitor-capable hardware and named driver/firmware versions.

## M8 — Structured wireless security assessment

**Goal:** Wifite-style **guided analysis UI**, grounded in real evidence rather than automatic password retrieval.

- [ ] Parse network beacon/probe frames and security capabilities from authorized captures: SSID/BSSID, bands, channel, RSN/WPA2/WPA3 and WPS advertisement where observable.
- [ ] Assess configuration risks (legacy protections, WPS exposure, outdated encryption) without equating them to exploitable weaknesses.
- [ ] Identify the **presence and completeness** of relevant authentication exchanges when captured, without extracting passwords or claiming successful credential compromise.
- [ ] Guided scope selection and human-readable reports with conservative evidence and privacy controls.
- [ ] Compare results against known-good configuration policies for the operator's own network.

## M9 — Hardware interoperability and production release

**Goal:** verifiable 1.0 release for **documented supported hardware**, not universal Android compatibility.

- [ ] End-to-end Xiaomi Redmi 14C, Samsung, Honor, Linux and USB adapter test matrix; regression/performance/thermal testing.
- [ ] Full Spanish/English localization audit, accessibility, user guidance and errors.
- [ ] Configure and verify original project signing key with stable SHA-256 certificate, signed release APK and upgrade migration.
- [ ] Sign-off on packet privacy, dependencies, licensing, crash recovery, build reproducibility and threat model.
- [ ] Publish 1.0 only for backend/hardware configurations validated by evidence.

**After M6: 3 proposed milestones remain (M7–M9).** Full feature parity with Wifite2's attack automation is **not** included in the stable-scope promise; that requires a separate design, safety review and hardware-specific testing.

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

## M2 build and signing observations (2026-11-19)

- [x] `testDebugUnitTest`: passed on GitHub Actions [#44563903907](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44563903907).
- [x] `assembleDebug` with R8 minification and resource shrinking: passed.
- [x] Debug `mapping.txt`: verified nonempty in CI.
- [x] Debug APK: artifact uploaded (`ghost-wifi-debug`).
- [ ] **Persistent release signing:** no `GHOST_WIFI_*` signing secrets configured; release job correctly skipped artifact generation.
- [ ] **Upgrade signature compatibility:** M1 and M2 Debug certificates differ, so direct app update is not possible. New APK must not be advertised as update-compatible until signed with the original project keystore.
- [ ] Physical OTG adapter connected/disconnected, user-granted permission, power/driver check and unknown VID:PID manual tests.

## M3 integration observations (2026-11-19)

- [x] GitHub Actions [GitHub Actions #44567816041](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44567816041) passed native C++ tests, Android unit tests, R8-minified Debug build, R8 mapping and artifact upload for commit `cda2761`.

- [x] UI status-bar/navigation-bar fix: root `Scaffold` uses `safeDrawingPadding` and consumes insets.
- [x] Removed **manually-added local phone** profile from Equipos; compatible model catalog remains read-only and versioned.
- [x] Native streaming parser for PCAP and PCAPNG, packet categories, bounds and invalid-file errors.
- [x] Private imported capture history and explicit JSON/raw export, with file limits.
- [ ] Physical Android 16 testing for bars and import/history/export; tests in CI are not a substitute for device testing.
- [ ] Live capturing from internal or OTG interfaces: requires validated chipset driver, session service and hardware. No fake capture state.
- [ ] M4 Linux SSH backend and remote captured-file workflow.

## M4 source integration (2026-11-19)

- [x] Android screen for Linux host, port, known_hosts public key, username, password or selected private key, SSH diagnostics and capture.
- [x] Strict host-key pinning, fixed agent command paths, bounded session/transfer, no arbitrary shell entry.
- [x] Python agent at `linux/ghost_wifi_agent.py` with list/capture/cleanup and unit tests (CI integration).
- [x] Capture transferred over SFTP and imported into M3 private PCAP history.
- [x] CI Python tests, C++ tests, Android unit tests, R8-minified Debug build and APK artifact via [GitHub Actions #44583206158](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44583206158).
- [ ] Android device smoke test and signed release with original persistent key; GitHub Secrets not configured.
- [ ] Physical test against a real Linux SSH server with compatible monitor interface and sudo/capabilities configured safely.
- [ ] Test network interruptions, SFTP failures, host key changes, large transfers and Android lifecycle.

## M5 source integration (2026-11-19)

- [x] `AppPreferences.kt`: language, theme, font scale, offline/identifier-export policy.
- [x] `CaptureRepository.kt`: persisted labels/notes, source/device/adapter metadata, JSON v2 report privacy defaults.
- [x] `PdfReportExporter.kt`: local Android PDF reports with explicit export consent.
- [x] `HistorySearch.kt` and tests: searchable metadata and sanitized labels.
- [x] UI updates: history search/sort/edit, PDF export, status NOT_MEASURED, language/theme/text-scale controls.
- [x] Android Debug R8 tests, `mapping.txt`, APK artifact: [GitHub Actions #44587539143](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44587539143) (commit `72a5674`).
- [ ] Real-device visual, PDF rendering, translation, privacy and accessibility tests; deferred to M6.
