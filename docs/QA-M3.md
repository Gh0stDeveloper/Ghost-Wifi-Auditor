# Ghost WiFi Auditor — M3 QA (v0.4.0-alpha)

Source branch: `main`, implementation commit `cda2761`. GitHub Actions [GitHub Actions #44567816041](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44567816041) succeeded (native C++, Android unit tests, R8 Debug build, mapping and APK artifact); this does NOT establish device/hardware compatibility.

## Fixes from user feedback

- **System UI overlap:** root Scaffold applies `safeDrawingPadding()` to avoid drawing app header underneath Android status bar (clock/battery/cellular) and app bottom bar underneath system navigation bar.
- **Equipos:** removed the editable "Otro dispositivo no listado" device profile. USB custom-adapter selection remains available under Ajustes.

## Offline features

- Android OpenDocument import of user-selected PCAP or PCAPNG into app-private storage; 128 MiB file limit / 256 MiB total.
- Native C++ parses PCAP records and PCAPNG section/interface/packet blocks; rejects malformed/truncated captures.
- Counts 802.11 management/control/data frames and Ethernet. Only metadata is included in JSON reports, not payload, private keys or Wi-Fi credentials.
- List, delete, export JSON, export original capture. Exports require explicit SAF user action.

## Tests to conduct on handset

1. Verify upper header does not overlap clock/battery/telephony indicators on Android 15/16, both portrait and after reopening app.
2. Verify bottom navigation is not beneath gesture navigation bar.
3. Open Equipos: no manual **phone profile** form; device catalog search and USB panel remain.
4. Audit → import valid PCAP, view format and packet counts; export JSON and compare count with source.
5. Audit → import valid PCAPNG with multiple interfaces/blocks; verify noncrash.
6. Import malformed/truncated file: should show an error, not save a valid session.
7. Open historical sessions, export raw capture, delete and confirm no longer appears after app restart.
8. Verify unsupported Wi-Fi device displays live capture as unavailable.
9. Check app doesn't request broad storage permission or modify Wi-Fi interfaces.

## Hardware limitations

No adapter currently available for QA. Code neither installs drivers nor implements live 802.11 sniffing. Root is not evidence of capture support. Live capture requires physical compatible chipset, kernel driver, firmware and a separately verified backend.

## Signing

The CI Debug APK is R8-minified and may be signed with a different ephemeral Debug certificate from earlier versions. The project-specific keystore must be configured as four private GitHub Secrets to produce reproducible update-compatible builds. Never publish a .p12 or its credentials.
