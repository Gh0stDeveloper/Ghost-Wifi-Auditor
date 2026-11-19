package com.ghostdeveloper.ghostwifiauditor

import org.junit.Assert.*
import org.junit.Test

class DiagnosticParsersTest {
    @Test fun monitorPresentOnlyInActualSection() {
        val text = """
Wiphy phy0
  Supported interface modes:
    * managed
    * monitor
  Band 1:
    Frequencies:
  software interface modes (can always be added):
    * P2P-device
""".trimIndent()
        val finding = DiagnosticParsers.monitor(text, 0, false)
        assertEquals(CapabilityStatus.SUPPORTED, finding.status)
        assertTrue(finding.summary.contains("no verificado"))
    }

    @Test fun ignoreMonitorOutsideAdvertisedSection() {
        val text = """
Supported interface modes:
   * managed
   * AP
Band 1:
Supported TX frame types:
  * monitor
""".trimIndent()
        assertEquals(CapabilityStatus.UNSUPPORTED, DiagnosticParsers.monitor(text, 0, false).status)
    }

    @Test fun missingHeaderIsUnknown() {
        assertEquals(CapabilityStatus.UNKNOWN, DiagnosticParsers.monitor("Wiphy phy0\nno modes here", 0, false).status)
    }

    @Test fun failedExecutionIsNotIncompatible() {
        assertEquals(CapabilityStatus.UNKNOWN, DiagnosticParsers.monitor("Permission denied", 13, false).status)
        assertEquals(CapabilityStatus.ERROR, DiagnosticParsers.monitor("", -1, true).status)
    }

    @Test fun rootDependsOnRealUid() {
        assertEquals(CapabilityStatus.SUPPORTED, DiagnosticParsers.root("uid=0(root) gid=0(root)", 0, false).status)
        assertEquals(CapabilityStatus.UNKNOWN, DiagnosticParsers.root("uid=10000(app)", 0, false).status)
        assertEquals(CapabilityStatus.UNKNOWN, DiagnosticParsers.root("no su", 127, false).status)
    }
}
