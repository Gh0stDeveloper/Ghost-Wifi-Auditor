#!/usr/bin/env python3
"""M6 static security regression checks (not a substitute for a security audit)."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def main() -> None:
    manifest = read("app/src/main/AndroidManifest.xml")
    require('android:allowBackup="false"' in manifest, "Android backups must be disabled")
    require('android:usesCleartextTraffic="false"' in manifest, "Cleartext must be disabled")
    require('android:exported="false"' in manifest, "Permission receiver must be private")

    remote = read(
        "app/src/main/java/com/ghostdeveloper/ghostwifiauditor/RemoteLinuxClient.kt"
    )
    require("ssh.loadKnownHosts(known)" in remote, "Missing pinned known_hosts")
    require("PromiscuousVerifier(" not in remote, "Unsafe SSH host verifier")
    require("AppPreferences.load(context).offlineOnly" in remote,
            "Offline mode must be enforced in network layer")
    require("RemoteCommandPolicy.captureCommand" in remote,
            "SSH remote capture must use tested command policy")
    require("RemoteCommandPolicy.isAcceptedCapturePath" in remote,
            "SFTP path validation must be enforced")
    require("Respuesta del agente demasiado grande" in remote,
            "SSH response read must have a size limit")
    require("RemoteCommandPolicy.cleanupCommand(id)" in remote,
            "Remote capture cleanup must run even after failures")

    ui = read(
        "app/src/main/java/com/ghostdeveloper/ghostwifiauditor/RemoteLinuxScreen.kt"
    )
    require("var password by remember {" in ui, "Passwords must stay in memory only")
    require("var passphrase by remember {" in ui, "Passphrases must stay in memory only")
    require("var password by rememberSaveable" not in ui,
            "Do not persist passwords in SavedState")

    capture = read(
        "app/src/main/java/com/ghostdeveloper/ghostwifiauditor/CaptureRepository.kt"
    )
    require("private const val MAX_CAPTURE_BYTES" in capture,
            "Local capture size limit missing")
    require("fun reportJson(includeIdentifiers: Boolean = false)" in capture,
            "Report must redact identifiers by default")

    gradle = read("app/build.gradle.kts")
    require(gradle.count("isMinifyEnabled = true") >= 2,
            "R8 must be active in both build variants")
    require("GHOST_WIFI_STORE_FILE" in gradle, "Persistent signing not supported")

    for private_file in ROOT.rglob("*"):
        if private_file.is_file() and private_file.suffix.lower() in (
            ".p12", ".jks", ".keystore", ".pfx"
        ):
            raise AssertionError(f"Keystore must not be committed: {private_file}")

    print("M6 static security regression checks passed")


if __name__ == "__main__":
    try:
        main()
    except AssertionError as error:
        print(f"SECURITY REGRESSION: {error}", file=sys.stderr)
        raise SystemExit(1)
