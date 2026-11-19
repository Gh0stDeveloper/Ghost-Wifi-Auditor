package com.ghostdeveloper.ghostwifiauditor

import org.junit.Assert.*
import org.junit.Test

class UsbProfilesTest {
    private val atheros = UsbAdapterProfile(
        id = "ar9271", vendorId = 0x0cf3, productId = 0x9271,
        candidateChipset = "Atheros AR9271", linuxDriver = "ath9k_htc",
        firmware = "ath9k_htc/htc_9271-1.4.0.fw",
        bands = "2.4 GHz", modelExamples = "ALFA AWUS036NHA",
        caveat = "Condicional", sourceUrl = "https://wireless.docs.kernel.org/"
    )
    private val mediatek = atheros.copy(id="mt7612u", vendorId=0x0e8d,
        productId=0x7612, linuxDriver="mt76x2u")

    @Test fun exactVidPidMatchesEvenAcrossDifferentVendors() {
        assertEquals("ar9271", UsbProfiles.match(listOf(atheros,mediatek), 0x0cf3,0x9271)?.id)
        assertEquals("mt7612u", UsbProfiles.match(listOf(atheros,mediatek), 0x0e8d,0x7612)?.id)
        assertNull(UsbProfiles.match(listOf(atheros,mediatek), 0x0cf3,0x7612))
    }

    @Test fun unknownDevicesRemainUnknown() {
        assertNull(UsbProfiles.match(listOf(atheros), 0x9999,0x1234))
        assertEquals("0001:0002", UsbProfiles.formatId(1,2))
    }

    @Test fun driverEvidenceDoesNotClaimMonitor() {
        val text=UsbProfiles.driverEvidence(atheros,setOf("ath9k_htc","cfg80211"))
        assertTrue(text.contains("no demuestra"))
        assertTrue(UsbProfiles.driverEvidence(atheros,emptySet()).contains("No se encontró"))
        assertTrue(UsbProfiles.driverEvidence(atheros,null).contains("no consultables"))
        assertTrue(UsbProfiles.driverEvidence(null,emptySet()).contains("por identificar"))
    }
}
