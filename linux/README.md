# Ghost WiFi Auditor — Linux remote agent (M4)

**Scope:** SSH-authenticated remote diagnostics and optional bounded, passive captures from a *pre-existing* Linux monitor interface. This is **not** a tool for enabling monitor mode on unsupported Android drivers.

## Requirements

- Linux desktop or computer under your control, OpenSSH server, Python 3.9+, `iw`.
- For live capture: `tcpdump` and an **already configured and functioning** monitor-mode Wi-Fi interface. Capture permissions must be explicitly granted at OS level; the agent does not use `sudo` or attempt privilege escalation.
- The phone and Linux server must have network reachability (same private LAN is recommended).
- Android APK v0.5.0-alpha from M4; project version may change following CI corrections.

## Install the agent (on your Linux computer)

```sh
sudo install -m 0755 linux/ghost_wifi_agent.py /usr/local/bin/ghost-wifi-agent
ghost-wifi-agent list
```

To connect via SSH, configure a **non-admin** account allowed to execute the installed agent and read its own capture files. Do not expose SSH to the internet unnecessarily. Prefer a LAN/VPN and public-key authentication.

## Host key verification

On the Linux machine, view the public host key using a trusted local session:

```sh
cat /etc/ssh/ssh_host_ed25519_key.pub
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256
```

For SSH on port 22, in the Android app paste a verified known_hosts line:

```text
192.168.1.25 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAA...replace-with-real-key
```

For SSH on another port, use `[192.168.1.25]:2222 ssh-ed25519 ...`.

**Do not blindly trust `ssh-keyscan` output or disable host verification.** Verify the host key through a separate trusted channel. The Android app does not store SSH passwords or imported private keys persistently.

## Commands and authorization

The agent accepts exactly:

- `ghost-wifi-agent list`: read-only interface discovery and server status.
- `ghost-wifi-agent capture --interface mon0 --seconds 10`: bounded capture **only** if `iw dev` reports the interface as `monitor`. Requires explicit user authorization in the Android UI. Max 30 s, 500 packets, snaplen 256 bytes, max 32 MiB.
- `ghost-wifi-agent cleanup --id <uuid>`: delete a capture file from the agent's own directory.

No arbitrary commands are exposed from Android; inputs are strictly validated and no shell is used for subprocess launch. Captures are saved privately under `~/.local/share/ghost-wifi-auditor/captures/` with restrictive permissions, transferred over SFTP and imported into the M3 PCAP history.

**Limitations:** SSH authentication, host pinning and remote capture are not a substitute for hardware QA. A configured Linux monitor interface and privilege to run `tcpdump` are required. Until tested on an actual host, only CI/protocol-level integration can be claimed.

## Troubleshooting

- SSH fingerprint mismatch: re-check the server public key directly on Linux, matching exact hostname and port.
- Permission denied: check user login and SSH password/key authorization. The app does not store secrets.
- Agent unavailable: install it at the fixed path; check `ghost-wifi-agent list` from the Linux shell.
- No monitor interfaces: install/configure a **supported** Linux Wi-Fi adapter and driver separately; the app cannot invent monitor support.
- TCPdump fails: confirm Linux user privileges and the interface; never grant blanket passwordless root to an untrusted account.
- SFTP fails: ensure OpenSSH SFTP subsystem is enabled, sufficient free space, and file is owned by the SSH user.
