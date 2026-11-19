# Ghost WiFi Auditor — M2 QA checklist (0.3.0-alpha)

**Code commit:** `5406aaad1e86e13b239494ac5e5770f83c98e677`  
**Android Actions:** https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions/runs/44563903907

## Automated checks

| Test | Result |
|---|---|
| Root/monitor parser unit tests | PASS |
| USB VID/PID matching and module-state unit tests | PASS |
| Android Debug `assembleDebug` | PASS |
| R8 `mapping.txt` exists | PASS |
| APK artifact uploaded | PASS |
| Persistent release signing | NOT RUN — private secrets missing |
| Tests on physical Wi-Fi USB adapter | NOT RUN |

## Manual checks (pending)

1. Open **Equipos → USB OTG** without anything connected: should display empty state.
2. Connect a USB-C OTG adapter or powered hub: enumerate USB ID, manufacturer/product, class/interfaces.
3. Confirm that unknown VID:PID stays **unknown** instead of being classified as Wi-Fi.
4. Try AR9271 `0CF3:9271` or MT7612U `0E8D:7612` (if available): should show **candidate chipset**, not capture support.
5. Tap **Solicitar permiso**, approve: label changes to permission granted. Denial must not crash.
6. Disconnect/reconnect: list updates automatically while view visible; also verify **Actualizar USB**.
7. Confirm that even with root and USB permission, capture remains **NO VERIFICADA** until actual driver/firmware/capture tests.
8. If the adapter resets or fails intermittently, retry with a powered OTG hub; USB Host permission alone does not supply power.
9. Run again on Redmi 14C after temporary root activation. Do not assume root persists across reboot.
10. Repeat on Samsung and Honor devices, collecting only non-sensitive diagnostics.

## Signature note

M1 and M2 CI Debug APK files were signed with **different ephemeral debug certificates**. Installing M2 over the existing M1 fails Android signature verification. To test this Debug build you may need to uninstall M1 (this erases M1 app data). For consistent future upgrades, configure the original project-specific keystore as private GitHub Actions Secrets, rebuild, and retain the same key for future versions. Do **not** upload credentials or the keystore to this public repository.

## No modification to these subsystems

M2 does **not** implement Wi-Fi monitor mode, driver installation, firmware flashing, active packet injection, WPS attacks or SSH. M3/M4 remain separate milestones.
