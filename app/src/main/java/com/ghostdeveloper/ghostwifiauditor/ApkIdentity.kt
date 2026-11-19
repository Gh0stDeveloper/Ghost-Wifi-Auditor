package com.ghostdeveloper.ghostwifiauditor

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Reports the installed APK signing identity without reading keystores or private material.
 * This fingerprint is public, useful for diagnosing incompatible app updates.
 */
object ApkIdentity {
    fun isDebuggable(context: Context): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    @Suppress("DEPRECATION")
    fun certificateSha256(context: Context): String {
        return runCatching {
            val manager = context.packageManager
            val packageName = context.packageName
            val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = manager.getPackageInfo(
                    packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signing = info.signingInfo ?: return@runCatching "NOT_AVAILABLE"
                if (signing.hasMultipleSigners()) signing.apkContentsSigners
                else signing.signingCertificateHistory
            } else {
                manager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            }
            val cert = signers?.firstOrNull()?.toByteArray()
                ?: return@runCatching "NOT_AVAILABLE"
            val bytes = MessageDigest.getInstance("SHA-256").digest(cert)
            bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }.getOrDefault("NOT_AVAILABLE")
    }
}
