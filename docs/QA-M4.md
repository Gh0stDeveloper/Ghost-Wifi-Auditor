# Ghost WiFi Auditor M4 — QA and acceptance (v0.5.0-alpha)

M4 source integrates a real SSH/SFTP client and Python agent. CI tests can validate code correctness but cannot prove a Wi-Fi monitor capture works until hardware and server are available.

## Automated checks

- Python `unittest` verifies `iw dev` parsing, interface sanitization, capture eligibility, duration bounds and safe cleanup.
- Existing C++ PCAP/PCAPNG parser tests and Android unit tests remain active.
- Android `assembleDebug` must pass with R8 mapping and artifact upload.
- Keep signing secrets private; do not embed credentials or keystores.

## Physical acceptance tests, deferred

1. Configure Linux OpenSSH server in an authorized lab. Install the bundled agent at `/usr/local/bin/ghost-wifi-agent`.
2. Obtain and verify SSH server public host key via a trusted local channel. Paste exact known_hosts entry into Android app.
3. Reject connection with incorrect host key; accept only the verified key.
4. Test username/password, SSH key with or without passphrase, wrong password, wrong port and offline server.
5. Verify a Linux system without monitor interface lists interfaces but disables capture.
6. On real compatible Linux hardware, configure monitor mode outside the Android app and validate `iw dev`.
7. Confirm authorization in UI; run 5-second capture. Agent should enforce max 30 seconds, 500 packets, 32 MiB.
8. Validate SFTP transfer to M3 imported history and analysis of PCAP on Android.
9. Verify remote cleanup after successful download; transfer failure must not show a fabricated success.
10. Test app minimized/closed, connection loss, USB disconnect and low storage. No continuous background capture support is claimed.

## Known limitations

- USB OTG and internal Android capture engines are still pending validation/development.
- Linux agent does not install drivers or configure monitor mode.
- Linux user requires appropriate existing permissions to run tcpdump.
- No persistent password/key storage or automatic key trust.
- No 24/7 background daemon or automatic reconnection; each SSH operation establishes a new authenticated session.

## Security and privacy

Only perform captures on networks you own or have permission to assess. PCAP may include sensitive data. The phone only stores imported files locally and exports upon explicit user action. No packet injection, password attacks or unknown host acceptance.
