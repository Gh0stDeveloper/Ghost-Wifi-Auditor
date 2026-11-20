<p align="center"><img src="assets/ghost-dragon.svg" alt="Ghost WiFi Auditor logo" width="130" /></p>

# Ghost WiFi Auditor
### Android wireless diagnostics and authorized security analysis

[![Android CI](https://img.shields.io/github/actions/workflow/status/Gh0stDeveloper/Ghost-Wifi-Auditor/android.yml?label=Android%20CI&logo=githubactions)](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions)

**Project under construction**

## Overview

Ghost WiFi Auditor is an Android application for wireless-network diagnostics, device capability inspection, and the analysis of capture files in authorized testing environments. It also explores integration with external hardware and controlled Linux environments.

The application is designed to distinguish verified hardware capabilities from features that require physical testing.

## Features and development areas

- Android hardware and Wi-Fi capability diagnostics.
- Device/kernel information and adapter compatibility checks.
- PCAP and PCAPNG file inspection.
- Structured reports, local history, and an Android interface.
- SSH/SFTP-based integration with a separate authorized Linux host.
- Defensive configuration validation and bounded remote operations.
- Automated Python, native C++, and Android tests.

## Technology

Kotlin · Jetpack Compose · Android SDK · C++/JNI · Python · SSH/SFTP · GitHub Actions

## Compatibility

Monitor mode, packet capture, and external adapter support depend on the exact Android device, kernel, firmware, chipset, and permissions. Detecting an adapter does not prove it can perform a live capture. Physical-device compatibility must be validated independently.

This project does **not** claim to recover wireless passwords, exploit access points, or enable monitor mode on arbitrary Android devices.

## Build

Open the Android project with Android Studio and use the repository's Gradle configuration. For reproducible build and test results, see [GitHub Actions](https://github.com/Gh0stDeveloper/Ghost-Wifi-Auditor/actions).

## Responsible use

Only analyze networks, devices, and captures that you own or are explicitly authorized to assess. Public source availability does not establish redistribution rights where no explicit license is provided.

---

**Ghost WiFi Auditor — wireless diagnostics with hardware-aware reporting.**
