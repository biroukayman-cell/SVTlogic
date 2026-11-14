package com.example.core

import com.example.data.model.CourseResource
import com.example.data.offline.PendingVideoTracking
import com.example.data.repository.SvtRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTrackingAndThemeTest {

    @Test
    fun computeVideoTrackingMetrics_trustsPlayerDurationOverDefaultResourceDuration() {
        // Teacher left default 120s, but actual ExoPlayer video duration is 10s and student watched all 10s.
        val metrics = SvtRepository.computeVideoTrackingMetrics(
            positionSeconds = 10L,
            totalDurationSeconds = 10L,
            resourceDurationSeconds = 120L
        )
        assertEquals(10L, metrics.durationSeconds)
        assertEquals(10L, metrics.maxPositionSeconds)
        assertEquals(100.0, metrics.watchedPercentage, 0.01)
        assertEquals("completed", metrics.status)
    }

    @Test
    fun computeVideoTrackingMetrics_fallsBackToResourceDurationWhenPlayerDurationUnknown() {
        val metrics = SvtRepository.computeVideoTrackingMetrics(
            positionSeconds = 60L,
            totalDurationSeconds = 0L,
            resourceDurationSeconds = 120L
        )
        assertEquals(120L, metrics.durationSeconds)
        assertEquals(60L, metrics.maxPositionSeconds)
        assertEquals(50.0, metrics.watchedPercentage, 0.01)
        assertEquals("partial", metrics.status)
    }

    @Test
    fun computeVideoTrackingMetrics_neverRegressesProgress() {
        val metrics = SvtRepository.computeVideoTrackingMetrics(
            positionSeconds = 5L,
            totalDurationSeconds = 100L,
            resourceDurationSeconds = 100L,
            prevPositionSeconds = 80L,
            prevPercentage = 80.0,
            watchedPercentOverride = 20.0
        )
        assertEquals(80L, metrics.maxPositionSeconds)
        assertEquals(80.0, metrics.watchedPercentage, 0.01)
        assertEquals("partial", metrics.status)
    }

    @Test
    fun pendingVideoTracking_respectsPercentageOverrideAndRealDuration() {
        val resource = CourseResource(
            id = "res_vid_1",
            classId = "cls_1",
            teacherId = "teach_1",
            title = "Mitose",
            type = "video",
            durationSeconds = 120L
        )
        val pending = PendingVideoTracking(
            resource = resource,
            studentName = "Amina",
            maxPositionSeconds = 10L,
            totalDurationSeconds = 10L,
            updatedAtMillis = 1000L,
            watchedPercentageOverride = 100.0
        )
        val tracking = pending.toVideoTracking("stu_1")
        assertEquals(10L, tracking.durationSeconds)
        assertEquals(100.0, tracking.watchedPercentage, 0.01)
        assertEquals("completed", tracking.status)
    }

    @Test
    fun themeEnums_resolveKeysWithSensibleDefaults() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("unknown"))

        assertEquals(AppThemePalette.OCEAN, AppThemePalette.fromKey("ocean"))
        assertEquals(AppThemePalette.EARTH, AppThemePalette.fromKey("earth"))
        assertEquals(AppThemePalette.BOTANICAL, AppThemePalette.fromKey("botanical"))
        assertEquals(AppThemePalette.EMERALD, AppThemePalette.fromKey(null))
    }
}
