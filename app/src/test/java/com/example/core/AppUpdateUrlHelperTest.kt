package com.example.core

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUpdateUrlHelperTest {

    @Test
    fun normalizeDownloadUrl_convertsDriveSdkShareLinkToDirectDownload() {
        val raw = "  https://drive.google.com/file/d/1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm/view?usp=drivesdk  "
        val normalized = AppUpdateUrlHelper.normalizeDownloadUrl(raw)
        assertEquals(
            "https://drive.google.com/uc?export=download&confirm=t&id=1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm",
            normalized
        )
    }

    @Test
    fun normalizeDownloadUrl_handlesOpenIdAndUsercontentLinks() {
        val openLink = "https://drive.google.com/open?id=1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm"
        assertEquals(
            "https://drive.google.com/uc?export=download&confirm=t&id=1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm",
            AppUpdateUrlHelper.normalizeDownloadUrl(openLink)
        )

        val ucLink = "https://drive.google.com/uc?id=1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm&export=download"
        assertEquals(
            "https://drive.google.com/uc?export=download&confirm=t&id=1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm",
            AppUpdateUrlHelper.normalizeDownloadUrl(ucLink)
        )
    }

    @Test
    fun folderLinks_areDetectedAndNotConverted() {
        val folder = "https://drive.google.com/drive/folders/1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm?usp=sharing"
        assertTrue(AppUpdateUrlHelper.isGoogleDriveFolderUrl(folder))
        assertNull(AppUpdateUrlHelper.extractGoogleDriveFileId(folder))
    }

    @Test
    fun nonDriveLinks_arePreservedAndIntentBuiltCleanly() {
        val github = "https://github.com/org/repo/releases/download/v1.2/app-release.apk"
        assertFalse(AppUpdateUrlHelper.isGoogleDriveUrl(github))
        assertEquals(github, AppUpdateUrlHelper.normalizeDownloadUrl(github))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val driveIntent = AppUpdateUrlHelper.buildUpdateViewIntent(
            context,
            "https://drive.google.com/file/d/1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm/view?usp=drivesdk"
        )
        assertEquals(
            "https://drive.google.com/uc?export=download&confirm=t&id=1P_Vlha3fkyyATrCb9uT4JNZF2je1j9bm",
            driveIntent.data.toString()
        )
    }
}
