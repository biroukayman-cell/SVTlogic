package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AvatarStorageHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AvatarStorageHelper.localAvatarFile(context, "stu_restart_test").delete()
    }

    @Test
    fun saveAndEncodeAvatar_persistsDataUriAndRestoresBitmapAcrossRestart() = runBlocking {
        val sampleBmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GREEN)
        }
        val tempInput = File(context.cacheDir, "picked_avatar_source.jpg")
        FileOutputStream(tempInput).use { fos ->
            sampleBmp.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        sampleBmp.recycle()

        val result = AvatarStorageHelper.saveAndEncodeAvatar(
            context = context,
            userId = "stu_restart_test",
            sourceUri = Uri.fromFile(tempInput)
        )
        assertTrue(result.isSuccess)
        val dataUri = result.getOrThrow()
        assertTrue(AvatarStorageHelper.isDataUri(dataUri))
        assertFalse(AvatarStorageHelper.isRemoteHttpUrl(dataUri))

        // Delete temp picked file (simulating ephemeral Photo Picker URI expiring after app close)
        tempInput.delete()

        val localFile = AvatarStorageHelper.localAvatarFile(context, "stu_restart_test")
        assertTrue(localFile.exists())
        assertTrue(localFile.length() > 0L)

        // Delete local cache file too, verify decoding from persisted Firestore data URI still works
        localFile.delete()
        val restoredFromDataUri = AvatarStorageHelper.resolveLocalAvatarBitmap(
            context = context,
            userId = "stu_restart_test",
            avatarUrl = dataUri
        )
        assertNotNull(restoredFromDataUri)
        assertEquals(200, restoredFromDataUri!!.width)
        assertEquals(150, restoredFromDataUri.height)
        assertTrue(localFile.exists())

        // Even with null avatarUrl later, cached local file resolves cleanly
        val restoredFromDisk = AvatarStorageHelper.resolveLocalAvatarBitmap(
            context = context,
            userId = "stu_restart_test",
            avatarUrl = null
        )
        assertNotNull(restoredFromDisk)
    }

    @Test
    fun encodeAndDecodeJpegPayload_handlesInvalidInputsSafely() = runBlocking {
        val sampleBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val baos = ByteArrayOutputStream()
        sampleBmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val uri = AvatarStorageHelper.encodeJpegBytesToDataUri(baos.toByteArray())
        sampleBmp.recycle()

        val decoded = AvatarStorageHelper.decodeBase64Payload(uri)
        assertNotNull(decoded)
        assertNull(AvatarStorageHelper.decodeBase64Payload("content://media/picker/0/1"))
        assertNull(AvatarStorageHelper.resolveLocalAvatarBitmap(context, "no_such_user", "content://expired/uri"))
    }
}
