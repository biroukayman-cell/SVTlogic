package com.example.core

import com.example.data.model.CourseResource
import com.example.data.model.VideoTracking
import com.example.data.offline.OfflineStorageManager
import com.example.data.offline.PendingVideoTracking
import com.example.data.repository.SvtRepository
import com.example.ui.viewmodel.SvtMainViewModel
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
    fun filterVideoOnlyTrackings_excludesNonVideoFileOpenRecords() {
        val resources = listOf(
            CourseResource(id = "vid1", type = "video"),
            CourseResource(id = "pdf1", type = "pdf"),
            CourseResource(id = "doc1", type = "word")
        )
        val trackings = listOf(
            VideoTracking(id = "vid1_s1", resourceId = "vid1", watchedPercentage = 95.0),
            VideoTracking(id = "pdf1_s1", resourceId = "pdf1", watchedPercentage = 100.0),
            VideoTracking(id = "doc1_s1", resourceId = "doc1", watchedPercentage = 100.0)
        )
        val filtered = SvtMainViewModel.filterVideoOnlyTrackings(trackings, resources)
        assertEquals(1, filtered.size)
        assertEquals("vid1", filtered.first().resourceId)
    }

    @Test
    fun offlineStorageHelpers_sanitizeNamesAndFormatBytes() {
        assertEquals(
            "Cours_Mitose_SVT.pdf",
            OfflineStorageManager.buildSafeDownloadFileName("Cours: Mitose / SVT", "r1", "pdf")
        )
        assertEquals("512 o", OfflineStorageManager.formatBytesReadable(512L))
        assertEquals("2 Ko", OfflineStorageManager.formatBytesReadable(2048L))
        assertEquals("1.5 Mo", OfflineStorageManager.formatBytesReadable(1536L * 1024L))
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
