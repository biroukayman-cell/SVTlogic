package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QrCodeUtilsTest {

    @Test
    fun parseScanPayload_handlesBioterreRosterFormat() {
        val result = QrCodeUtils.parseScanPayload("BIOTERRE:SVT-2BAC-A1|G134567890")
        assertEquals("SVT-2BAC-A1", result.classToken)
        assertEquals("G134567890", result.massarNumber)
    }

    @Test
    fun parseScanPayload_handlesPlainClassCode() {
        val result = QrCodeUtils.parseScanPayload("  svt-1bac-b2 ")
        assertEquals("svt-1bac-b2", result.classToken)
        assertNull(result.massarNumber)
    }

    @Test
    fun generateAndDecodeQrBitmap_roundtripsClassPayload() {
        val payload = "BIOTERRE:SVT-9X8Y7Z|K112233445"
        val bitmap = QrCodeUtils.generateQrBitmap(payload, sizePx = 320)
        val decoded = QrCodeUtils.decodeQrFromBitmap(bitmap)
        assertNotNull(decoded)
        assertEquals("SVT-9X8Y7Z", decoded?.classToken)
        assertEquals("K112233445", decoded?.massarNumber)
    }
}
