#!/usr/bin/env python3
"""Ghost WiFi Auditor Linux agent, v1: fixed commands, no arbitrary remote shell.

Install this file at /usr/local/bin/ghost-wifi-agent (chmod 0755).
Requires: python3; iw and ip for diagnostics; tcpdump for authorized capture.
Runs as the SSH user, with only privileges that user already has.
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import re
import shutil
import socket
import subprocess
import sys
import time
import uuid

VERSION = "1.0"
IFACE = re.compile(r"^[A-Za-z][A-Za-z0-9_.:-]{0,14}$")
CAPTURE_ID = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
CAPTURE_DIR = pathlib.Path.home() / ".local" / "share" / "ghost-wifi-auditor" / "captures"
MAX_SECONDS = 30
MAX_CAPTURE_BYTES = 32 * 1024 * 1024


def parse_iw(output: str) -> list[dict[str, str]]:
    interfaces: list[dict[str, str]] = []
    current: dict[str, str] | None = None
    for line in output.splitlines():
        stripped = line.strip()
        if stripped.startswith("Interface "):
            if current:
                interfaces.append(current)
            name = stripped.split(" ", 1)[1].strip()
            current = {"name": name, "type": "unknown"}
        elif current and stripped.startswith("type "):
            current["type"] = stripped.split(" ", 1)[1].strip()
    if current:
        interfaces.append(current)
    return [i for i in interfaces if IFACE.fullmatch(i["name"])][:64]


def iw_interfaces() -> list[dict[str, str]]:
    if not shutil.which("iw"):
        return []
    try:
        result = subprocess.run(
            ["iw", "dev"], text=True, capture_output=True, check=False,
            timeout=8, stdin=subprocess.DEVNULL,
        )
    except (OSError, subprocess.TimeoutExpired):
        return []
    return parse_iw(result.stdout) if result.returncode == 0 else []


def list_info() -> dict:
    return {
        "ok": True,
        "version": VERSION,
        "hostname": socket.gethostname()[:80],
        "kernel": os.uname().release[:120],
        "interfaces": iw_interfaces(),
        "tcpdump": bool(shutil.which("tcpdump")),
    }


def create_capture(interface: str, seconds: int) -> dict:
    if not IFACE.fullmatch(interface) or seconds not in range(5, MAX_SECONDS + 1):
        return {"ok": False, "error": "Invalid interface or capture duration"}
    available = {i["name"] for i in iw_interfaces() if i["type"] == "monitor"}
    if interface not in available:
        return {"ok": False, "error": "Interface is not in confirmed monitor mode"}
    if not shutil.which("tcpdump"):
        return {"ok": False, "error": "tcpdump is not installed"}
    CAPTURE_DIR.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(CAPTURE_DIR, 0o700)
    capture_id = str(uuid.uuid4())
    file = CAPTURE_DIR / (capture_id + ".pcap")
    # Never use shell=True. Restrict snaplen and packet count and set a duration.
    command = [
        "tcpdump", "-i", interface, "-s", "256", "-U", "-c", "500",
        "-w", str(file),
    ]
    try:
        proc = subprocess.Popen(
            command, stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True,
        )
        try:
            _out, err = proc.communicate(timeout=seconds)
        except subprocess.TimeoutExpired:
            proc.terminate()
            try:
                _out, err = proc.communicate(timeout=4)
            except subprocess.TimeoutExpired:
                proc.kill()
                _out, err = proc.communicate(timeout=3)
        # A timer-based termination can return nonzero; accept only a valid PCAP with data.
        if not file.exists() or file.stat().st_size <= 24:
            return {"ok": False, "error": "No packets captured or insufficient permissions"}
        if file.stat().st_size > MAX_CAPTURE_BYTES:
            file.unlink(missing_ok=True)
            return {"ok": False, "error": "Capture exceeded 32 MiB"}
        os.chmod(file, 0o600)
        return {"ok": True, "id": capture_id, "path": str(file), "bytes": file.stat().st_size}
    except (OSError, subprocess.TimeoutExpired) as error:
        return {"ok": False, "error": "Capture unavailable: " + type(error).__name__}
    finally:
        # Keep only successful, bounded captures. Do not leave empty PCAP files behind.
        if file.exists() and (file.stat().st_size <= 24 or file.stat().st_size > MAX_CAPTURE_BYTES):
            file.unlink(missing_ok=True)


def cleanup(capture_id: str) -> dict:
    if not CAPTURE_ID.fullmatch(capture_id):
        return {"ok": False, "error": "Invalid capture identifier"}
    file = CAPTURE_DIR / (capture_id + ".pcap")
    file.unlink(missing_ok=True)
    return {"ok": True, "removed": True}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Ghost WiFi Auditor remote agent")
    commands = parser.add_subparsers(dest="action", required=True)
    commands.add_parser("list")
    capture = commands.add_parser("capture")
    capture.add_argument("--interface", required=True)
    capture.add_argument("--seconds", type=int, required=True)
    delete = commands.add_parser("cleanup")
    delete.add_argument("--id", required=True)
    args = parser.parse_args(argv)
    if args.action == "list":
        result = list_info()
    elif args.action == "capture":
        result = create_capture(args.interface, args.seconds)
    else:
        result = cleanup(args.id)
    print(json.dumps(result, separators=(",", ":"), ensure_ascii=True))
    return 0 if result.get("ok") else 1


if __name__ == "__main__":
    sys.exit(main())
