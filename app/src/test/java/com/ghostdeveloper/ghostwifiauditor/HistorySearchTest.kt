package com.ghostdeveloper.ghostwifiauditor

import org.junit.Assert.*
import org.junit.Test

class HistorySearchTest {
    @Test fun emptyQueryIncludesAll() {
        assertTrue(HistorySearch.matches("   ", "sample.pcap"))
    }

    @Test fun searchesMetadataIgnoringCase() {
        assertTrue(HistorySearch.matches("LAB", "sample.pcap", "Test Lab", "note"))
        assertTrue(HistorySearch.matches("pcap", "sample.pcap"))
        assertFalse(HistorySearch.matches("secret-token", "sample.pcap", "test lab"))
    }

    @Test fun cleansAnnotationsBeforeSaving() {
        assertEquals("test label", HistorySearch.sanitizeLabel(" test\nlabel "))
        assertEquals(80, HistorySearch.sanitizeLabel("x".repeat(400)).length)
    }
}
