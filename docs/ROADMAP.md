# Roadmap

## M0 – Android design and diagnostic base (this delivery)
Graphite AMOLED UI; original dragon icon; four primary tabs plus nested About; on-demand `su` and `iw` checks; USB VID/PID enumeration; conditional device compatibility; adapter profile options; About credit; persistent release keystore created separately; R8; native PCAP header recognition.

## M1 – Device matrix
Track model, SKU, Android build, kernel, driver, firmware, evidence URL, tested date, verified test result, and whether adapter is required. Store in editable versioned JSON; support user-submitted profiles without claiming verified support.

## M2 – USB adapters
USB permission flow, vendor/product database, chip/driver compatibility matrix and power warnings, with manual override for unknown products. No module loading without separate reviewed integration.

## M3 – Authorized wireless captures
Only when verified driver and monitor capability present; separate capture service, consent and scope checks, PCAPNG parser and session history. Keep test/feature gating explicit.

## M4 – Linux remote
Authenticated SSH pairing, host fingerprint pinning, narrowly scoped agent, read-only discovery and explicit user-approved capture operations; no embedded credentials in APK.

## M5 – Reports and localization
PDF/JSON reports, encrypted session storage, accessibility, translations, crash diagnostics and instrumented UI tests across Xiaomi, Samsung, Honor and additional Android devices.
