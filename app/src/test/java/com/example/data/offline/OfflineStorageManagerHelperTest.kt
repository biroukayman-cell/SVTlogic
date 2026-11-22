package com.example.data.offline

import com.example.data.ai.SvtAiRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineStorageManagerHelperTest {

    @Test
    fun parseChunkResourceId_extractsIdFromChunkScheme() {
        assertEquals(
            "abc123",
            OfflineStorageManager.parseChunkResourceId("firestore://resource_chunks/abc123?ext=pdf")
        )
        assertEquals(
            "res_42",
            OfflineStorageManager.parseChunkResourceId("  firestore://resource_chunks/res_42/part  ")
        )
    }

    @Test
    fun parseChunkResourceId_returnsNullForNonChunkUrls() {
        assertNull(OfflineStorageManager.parseChunkResourceId("https://example.com/cours.pdf"))
        assertNull(OfflineStorageManager.parseChunkResourceId("content://media/external/video/1"))
        assertNull(OfflineStorageManager.parseChunkResourceId("firestore://resource_chunks/"))
    }

    @Test
    fun normalizeDownloadUrl_convertsGoogleDriveLinks() {
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1AbCdEfG_123",
            OfflineStorageManager.normalizeDownloadUrl("https://drive.google.com/file/d/1AbCdEfG_123/view?usp=sharing")
        )
        assertEquals(
            "https://drive.google.com/uc?export=download&id=9XyZ_789",
            OfflineStorageManager.normalizeDownloadUrl("https://drive.google.com/open?id=9XyZ_789")
        )
    }

    @Test
    fun normalizeDownloadUrl_preservesStandardUrlsAndAlreadyNormalizedDriveLinks() {
        val direct = "https://github.com/org/repo/releases/download/v1.1/app-debug.apk"
        assertEquals(direct, OfflineStorageManager.normalizeDownloadUrl("  $direct  "))

        val alreadyExport = "https://drive.google.com/uc?export=download&id=1AbCdEfG_123"
        assertEquals(alreadyExport, OfflineStorageManager.normalizeDownloadUrl(alreadyExport))
    }

    @Test
    fun inferExtension_resolvesQueryUrlAndTypeFallbacks() {
        assertEquals(
            "docx",
            OfflineStorageManager.inferExtension("firestore://resource_chunks/r1?ext=DOCX", "pdf")
        )
        assertEquals(
            "pdf",
            OfflineStorageManager.inferExtension("https://cdn.example.org/files/chapitre1.PDF?token=1", "word")
        )
        assertEquals(
            "mp4",
            OfflineStorageManager.inferExtension("https://cdn.example.org/stream/lesson", "video")
        )
        assertEquals(
            "jpg",
            OfflineStorageManager.inferExtension("https://cdn.example.org/stream/schema", "image")
        )
    }

    @Test
    fun mimeTypeForResource_mapsKnownFormatsAndFallbacks() {
        assertEquals(
            "application/pdf",
            OfflineStorageManager.mimeTypeForResource("pdf", "firestore://resource_chunks/id1?ext=pdf")
        )
        assertEquals(
            "application/msword",
            OfflineStorageManager.mimeTypeForResource("word", "https://example.org/fiche.doc")
        )
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            OfflineStorageManager.mimeTypeForResource("word", "firestore://resource_chunks/id2?ext=docx")
        )
        assertEquals(
            "image/png",
            OfflineStorageManager.mimeTypeForResource("image", "https://example.org/cellule.png")
        )
        assertEquals(
            "video/mp4",
            OfflineStorageManager.mimeTypeForResource("video", "https://example.org/cours_svt.mp4")
        )
    }

    @Test
    fun isAppCheckOrTokenError_detectsAttestationChainAndIgnoresGenericNetworkErrors() {
        val appCheckRoot = RuntimeException("HTTP 403 App Check token is invalid")
        val wrapped = IllegalStateException("AI failure", appCheckRoot)
        assertTrue(SvtAiRepository.isAppCheckOrTokenError(wrapped))

        assertTrue(SvtAiRepository.isAppCheckOrTokenError(IllegalStateException("Missing attestation firebase token")))
        assertTrue(SvtAiRepository.isAppCheckOrTokenError(IllegalStateException("reCAPTCHA enterprise verification failed")))

        val unrelatedApiError = IllegalStateException("HTTP 403: Vertex AI API disabled in project")
        assertFalse(SvtAiRepository.isAppCheckOrTokenError(unrelatedApiError))

        val timeoutError = java.net.SocketTimeoutException("Read timed out")
        assertFalse(SvtAiRepository.isAppCheckOrTokenError(timeoutError))
    }
}
