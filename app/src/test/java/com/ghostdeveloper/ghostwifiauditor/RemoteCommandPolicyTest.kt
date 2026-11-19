package com.ghostdeveloper.ghostwifiauditor

import org.junit.Assert.*
import org.junit.Test

class RemoteCommandPolicyTest {
    private val exampleKey = "A".repeat(80)
    private fun valid(port: Int = 22) {
        val host = "192.0.2.10"
        val label = if (port == 22) host else "[$host]:$port"
        RemoteCommandPolicy.validateConnection(
            host, port, "auditor", "$label ssh-ed25519 $exampleKey", true)
    }

    @Test fun acceptsIndependentlyVerifiedHostEntry() {
        valid()
        valid(2222)
    }

    @Test fun rejectsUntrustedOrMismatchedHostEntry() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteCommandPolicy.validateConnection(
                "192.0.2.10", 22, "auditor", "192.0.2.11 ssh-ed25519 $exampleKey", true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteCommandPolicy.validateConnection(
                "192.0.2.10", 22, "auditor", "192.0.2.10 ssh-ed25519 $exampleKey", false)
        }
    }

    @Test fun forbidsShellMetacharactersAndInvalidDuration() {
        for (name in listOf("mon0;id", "mon0/other", "wlan0 && id", "-i", "a".repeat(16))) {
            assertThrows(IllegalArgumentException::class.java) {
                RemoteCommandPolicy.captureCommand(name, 10)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteCommandPolicy.captureCommand("mon0", 60)
        }
        assertEquals(
            "/usr/local/bin/ghost-wifi-agent capture --interface mon0 --seconds 10",
            RemoteCommandPolicy.captureCommand("mon0", 10))
    }

    @Test fun remotePathsCannotEscapePrivateCaptureDirectory() {
        val id = "11111111-2222-3333-4444-555555555555"
        val proper = "/home/auditor/.local/share/ghost-wifi-auditor/captures/$id.pcap"
        assertTrue(RemoteCommandPolicy.isAcceptedCapturePath(id, proper))
        for (path in listOf(
            "/etc/$id.pcap", "/tmp/.local/share/ghost-wifi-auditor/captures/../$id.pcap",
            proper + ".bak", "/home//auditor/.local/share/ghost-wifi-auditor/captures/$id.pcap"
        )) assertFalse(RemoteCommandPolicy.isAcceptedCapturePath(id, path))
    }

    @Test fun enforcesTransferBounds() {
        assertTrue(RemoteCommandPolicy.validateCaptureSize(25))
        assertFalse(RemoteCommandPolicy.validateCaptureSize(0))
        assertFalse(RemoteCommandPolicy.validateCaptureSize(33L * 1024 * 1024))
    }
}
