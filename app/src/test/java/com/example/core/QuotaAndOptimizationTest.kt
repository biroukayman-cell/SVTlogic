package com.example.core

import com.example.data.ai.SvtAiRepository
import com.example.data.model.VideoTracking
import com.example.data.repository.SvtRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class QuotaAndOptimizationTest {

    @Test
    fun isQuotaExceededError_detectsResourceExhaustedAndRateLimitMessages() {
        assertTrue(isQuotaExceededError(RuntimeException("RESOURCE_EXHAUSTED: limit reached")))
        assertTrue(
            isQuotaExceededError(
                IllegalStateException("Wrapper", RuntimeException("Quota exceeded for project"))
            )
        )
        assertTrue(isQuotaExceededError(RuntimeException("Free daily quota reached")))
        assertTrue(isQuotaExceededError(RuntimeException("HTTP 429 Too Many Requests")))
        assertTrue(isQuotaExceededError(RuntimeException("Too many attempts, please wait")))
        assertTrue(isQuotaExceededError(RuntimeException("API rate limit exceeded")))

        assertFalse(isQuotaExceededError(RuntimeException("PERMISSION_DENIED: Missing permissions")))
        assertFalse(isQuotaExceededError(IOException("Network unavailable")))
        assertFalse(isQuotaExceededError(null))
    }

    @Test
    fun isPermissionDeniedError_detectsPermissionDeniedMessages() {
        assertTrue(isPermissionDeniedError(RuntimeException("PERMISSION_DENIED")))
        assertTrue(
            isPermissionDeniedError(
                IllegalStateException(
                    "Operation failed",
                    RuntimeException("Missing or insufficient permissions.")
                )
            )
        )
        assertFalse(isPermissionDeniedError(RuntimeException("RESOURCE_EXHAUSTED")))
        assertFalse(isPermissionDeniedError(IOException("Network reset")))
        assertFalse(isPermissionDeniedError(null))
    }

    @Test
    fun isAppCheckOrTokenError_doesNotMisclassifyQuotaErrorsAsAppCheck() {
        val rateLimitedRecaptcha = IllegalStateException("App Check failed: Too many attempts")
        assertFalse(SvtAiRepository.isAppCheckOrTokenError(rateLimitedRecaptcha))

        val quotaWrapped = IllegalStateException(
            "reCAPTCHA error",
            RuntimeException("RESOURCE_EXHAUSTED: Quota exceeded")
        )
        assertFalse(SvtAiRepository.isAppCheckOrTokenError(quotaWrapped))
    }

    @Test
    fun buildDefinitionCacheKey_normalizesCaseWhitespaceAndLanguage() {
        val fr1 = SvtAiRepository.buildDefinitionCacheKey("  Mitose  ", AppLanguage.FR)
        val fr2 = SvtAiRepository.buildDefinitionCacheKey("mitose", AppLanguage.FR)
        val en = SvtAiRepository.buildDefinitionCacheKey("mitose", AppLanguage.EN)

        assertEquals(fr1, fr2)
        assertNotEquals(fr1, en)
    }

    @Test
    fun shouldRetryWorkerFirestoreCodeName_stopsOnQuotaAndPermissionErrorsAndRetriesTransient() {
        assertFalse(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("RESOURCE_EXHAUSTED", 0))
        assertFalse(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("PERMISSION_DENIED", 0))
        assertFalse(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("UNAUTHENTICATED", 0))
        assertFalse(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("INVALID_ARGUMENT", 0))

        assertTrue(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("UNAVAILABLE", 0))
        assertTrue(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("DEADLINE_EXCEEDED", 1))
        assertTrue(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("ABORTED", 0))

        // Exhausted max retry attempts
        assertFalse(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("UNAVAILABLE", 2))
        assertFalse(SvtBackgroundNotificationWorker.shouldRetryWorkerFirestoreCodeName("DEADLINE_EXCEEDED", 2))
    }

    @Test
    fun shouldRetryWorkerException_stopsOnQuotaAndPermissionAndRetriesIo() {
        assertFalse(
            SvtBackgroundNotificationWorker.shouldRetryWorkerException(
                RuntimeException("HTTP 429 Too Many Requests"),
                0
            )
        )
        assertFalse(
            SvtBackgroundNotificationWorker.shouldRetryWorkerException(
                RuntimeException("Missing or insufficient permissions"),
                0
            )
        )
        assertTrue(
            SvtBackgroundNotificationWorker.shouldRetryWorkerException(
                IOException("Connection reset"),
                0
            )
        )
        assertFalse(
            SvtBackgroundNotificationWorker.shouldRetryWorkerException(
                IOException("Connection reset"),
                2
            )
        )
    }

    @Test
    fun shouldScheduleLeaveCheck_enforcesCooldownWindow() {
        val cooldown = NotificationSyncScheduler.LEAVE_CHECK_COOLDOWN_MS
        assertTrue(NotificationSyncScheduler.shouldScheduleLeaveCheck(nowMs = 1_000L, lastScheduledMs = 0L))
        assertFalse(
            NotificationSyncScheduler.shouldScheduleLeaveCheck(
                nowMs = 1_000_000L + cooldown - 1L,
                lastScheduledMs = 1_000_000L
            )
        )
        assertTrue(
            NotificationSyncScheduler.shouldScheduleLeaveCheck(
                nowMs = 1_000_000L + cooldown,
                lastScheduledMs = 1_000_000L
            )
        )
    }

    @Test
    fun repositoryValidationHelpers_enforceRuleConstraints() {
        assertEquals("stu_123_abc", SvtRepository.sanitizeId("stu@123/abc"))
        assertEquals(10, SvtRepository.sanitizeId("a".repeat(25), maxLen = 10).length)

        assertEquals("Anonyme", SvtRepository.clampMinMax("   ", fallback = "Anonyme", minLen = 2, maxLen = 20))
        assertEquals("A_", SvtRepository.clampMinMax("A", fallback = "Anonyme", minLen = 2, maxLen = 20))
        assertEquals("12345", SvtRepository.clampMinMax("12345678", fallback = "x", minLen = 1, maxLen = 5))

        assertTrue(SvtRepository.isValidResourceUrlForRules("https://example.com/course.pdf"))
        assertTrue(SvtRepository.isValidResourceUrlForRules("firestore://resource_chunks/res_123?ext=pdf"))
        assertFalse(SvtRepository.isValidResourceUrlForRules("http://insecure.example.com/a.pdf"))
        assertFalse(SvtRepository.isValidResourceUrlForRules("bioterre-chunk://firestore/res_123"))
        assertFalse(SvtRepository.isValidResourceUrlForRules("file:///data/local/tmp/a.pdf"))
    }

    @Test
    fun shouldSkipVideoTrackingSync_skipsRedundantUpdatesAndAllowsMeaningfulProgress() {
        val existingPartial = VideoTracking(
            id = "res1_stu1",
            resourceId = "res1",
            maxPositionSeconds = 20L,
            durationSeconds = 100L,
            watchedPercentage = 20.0,
            status = "partial"
        )
        val tinyAdvance = SvtRepository.VideoTrackingMetrics(
            durationSeconds = 100L,
            maxPositionSeconds = 22L,
            watchedPercentage = 22.0,
            status = "partial"
        )
        val meaningfulAdvance = SvtRepository.VideoTrackingMetrics(
            durationSeconds = 100L,
            maxPositionSeconds = 30L,
            watchedPercentage = 30.0,
            status = "partial"
        )
        val newlyCompleted = SvtRepository.VideoTrackingMetrics(
            durationSeconds = 100L,
            maxPositionSeconds = 90L,
            watchedPercentage = 90.0,
            status = "completed"
        )

        assertFalse(SvtRepository.shouldSkipVideoTrackingSync(null, tinyAdvance))
        assertTrue(SvtRepository.shouldSkipVideoTrackingSync(existingPartial, tinyAdvance))
        assertFalse(SvtRepository.shouldSkipVideoTrackingSync(existingPartial, meaningfulAdvance))
        assertFalse(SvtRepository.shouldSkipVideoTrackingSync(existingPartial, newlyCompleted))

        val alreadyFull = existingPartial.copy(
            maxPositionSeconds = 100L,
            watchedPercentage = 100.0,
            status = "completed"
        )
        assertTrue(
            SvtRepository.shouldSkipVideoTrackingSync(
                alreadyFull,
                newlyCompleted.copy(maxPositionSeconds = 100L, watchedPercentage = 100.0)
            )
        )
    }
}
