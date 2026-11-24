package com.example.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityLockManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SecurityLockManager.disablePin(context, "user_alpha")
        SecurityLockManager.disablePin(context, "user_beta")
    }

    @Test
    fun pinFormatValidation_enforces4To6Digits() {
        assertFalse(SecurityLockManager.isValidPinFormat("123"))
        assertFalse(SecurityLockManager.isValidPinFormat("12a4"))
        assertFalse(SecurityLockManager.isValidPinFormat("1234567"))
        assertTrue(SecurityLockManager.isValidPinFormat("1234"))
        assertTrue(SecurityLockManager.isValidPinFormat("098765"))
    }

    @Test
    fun setPinAndVerify_succeedsPerUserIdAndDisablesCleanly() {
        assertFalse(SecurityLockManager.isPinEnabled(context, "user_alpha"))
        assertEquals(PinVerifyResult.NotConfigured, SecurityLockManager.verifyPin(context, "user_alpha", "1234"))

        assertTrue(SecurityLockManager.setPin(context, "user_alpha", "2580"))
        assertTrue(SecurityLockManager.isPinEnabled(context, "user_alpha"))
        assertFalse(SecurityLockManager.isPinEnabled(context, "user_beta"))

        assertEquals(PinVerifyResult.Success, SecurityLockManager.verifyPin(context, "user_alpha", "2580"))

        SecurityLockManager.disablePin(context, "user_alpha")
        assertFalse(SecurityLockManager.isPinEnabled(context, "user_alpha"))
    }

    @Test
    fun fiveWrongAttempts_triggers30SecondLockoutAndBlocksValidPinUntilExpiry() {
        assertTrue(SecurityLockManager.setPin(context, "user_alpha", "4242"))

        var baseTime = 1_000_000L
        for (attempt in 1..4) {
            val res = SecurityLockManager.verifyPin(context, "user_alpha", "0000", nowMillis = baseTime)
            assertEquals(PinVerifyResult.Invalid(attemptsRemaining = 5 - attempt), res)
        }

        val lockoutRes = SecurityLockManager.verifyPin(context, "user_alpha", "0000", nowMillis = baseTime)
        assertEquals(PinVerifyResult.LockedOut(30), lockoutRes)
        assertEquals(30, SecurityLockManager.getRemainingLockoutSeconds(context, "user_alpha", baseTime))

        // Valid PIN must remain blocked during the 30s lockout window
        val duringLockout = SecurityLockManager.verifyPin(
            context,
            "user_alpha",
            "4242",
            nowMillis = baseTime + 10_000L
        )
        assertEquals(PinVerifyResult.LockedOut(20), duringLockout)

        // Once 30s passes, the valid PIN unlocks cleanly
        val afterLockout = SecurityLockManager.verifyPin(
            context,
            "user_alpha",
            "4242",
            nowMillis = baseTime + 31_000L
        )
        assertEquals(PinVerifyResult.Success, afterLockout)
        assertEquals(0, SecurityLockManager.getRemainingLockoutSeconds(context, "user_alpha", baseTime + 31_000L))
    }

    @Test
    fun setPin_storesPbkdf2V2Hash_andLegacySha256UpgradesOnVerify() {
        assertTrue(SecurityLockManager.setPin(context, "user_alpha", "7391"))
        val prefs = context.getSharedPreferences("bioterre_secondary_security", Context.MODE_PRIVATE)
        val storedV2 = prefs.getString("pin_hash_user_alpha", "").orEmpty()
        assertTrue(storedV2.startsWith("v2$"))

        // Simulate a legacy SHA-256 PIN entry from an older app build
        val legacySalt = "legacySaltB64"
        val legacyHash = SecurityLockManager.hashPinWithSalt("6420", legacySalt)
        prefs.edit()
            .putString("pin_salt_user_beta", legacySalt)
            .putString("pin_hash_user_beta", legacyHash)
            .commit()

        assertEquals(PinVerifyResult.Success, SecurityLockManager.verifyPin(context, "user_beta", "6420"))
        val upgraded = prefs.getString("pin_hash_user_beta", "").orEmpty()
        assertTrue("Legacy hash must upgrade transparently to PBKDF2 v2", upgraded.startsWith("v2$"))
        assertEquals(PinVerifyResult.Success, SecurityLockManager.verifyPin(context, "user_beta", "6420"))
    }
}
