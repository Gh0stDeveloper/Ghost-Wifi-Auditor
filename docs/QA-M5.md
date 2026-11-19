# Ghost WiFi Auditor — M5 QA and release checklist

**Version:** 0.6.0-alpha  
**Scope:** report/history/privacy, bilingual primary UI, theme/text scaling, offline-only mode.  
**Hardware tests:** deferred to M6 by the project owner.

## Automation result — PASS

[GitHub Actions #44587539143](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44587539143) (code commit `72a5674`) completed successfully: Python agent tests, C++ PCAP/PCAPNG tests, Android unit tests, R8 Debug APK build, nonempty mapping.txt and artifact upload. Signed release **was skipped**, as repository secrets `GHOST_WIFI_*` are not configured.

## Automated checks

- `HistorySearchTest`: empty query, case-insensitive metadata match, annotation sanitization.
- Existing `DiagnosticParsersTest`, `UsbProfilesTest`, Python agent tests and native C++ PCAP tests.
- `testDebugUnitTest`, `assembleDebug` with R8 and mapping file.
- Signed release requires the project's existing four `GHOST_WIFI_*` secrets; do not generate/replace keystore in public source.

## Functional device checks (deferred to M6)

1. Switch Spanish ↔ English without losing history; verify language on primary screens, About and SSH; track remaining technical strings.
2. Switch AMOLED/graphite/light while checking contrast, system insets and keyboard navigation.
3. Change text scale 90–130%; check layout for cutoffs; honor Android system font scale and accessibility preferences.
4. Enable offline-only mode: SSH action disabled and remote adapter images not loaded; PCAP import, analysis, history and reports still work without Internet.
5. Import synthetic PCAP and PCAPNG; create a session label/note, restart the app and confirm persistence.
6. Search by label, original filename, source and note; sort newest/oldest/packet count; delete and verify no longer appears.
7. Export JSON and PDF with identifiers disabled: original filename, model, selected adapter, labels and notes must not be present.
8. Explicitly enable identifiers and re-export; verify only intended sensitive metadata is included.
9. Export original PCAP and verify the warning about raw, potentially identifying packet content.
10. Open generated PDF in multiple readers; verify no clipped text, invalid pages or incorrect glyphs. Android PdfDocument output is **not visually validated by GitHub CI**.
11. Confirm WPS, advertised encryption, channel and RSSI display NOT_MEASURED rather than invented results.
12. Reopen older M3/M4 histories (without notes/device fields) and ensure migration-safe defaults.

## Excluded from M5

- WPA2/WPA3 handshake validation, password cracking, active Wi-Fi attacks or claims of discovered vulnerabilities.
- Complete live-monitor capture compatibility on Android internal WLAN or any USB adapter.
- Background autonomous capture and arbitrary remote shell commands.
- Persisting SSH secrets in local preferences.

## Remaining for stable 1.0.0

M6 real-device QA, signed release with persistent keystore, security review, complete translations/attributions and validated hardware-specific capture workflows.
