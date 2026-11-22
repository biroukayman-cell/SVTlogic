package com.example.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAuthHelperTest {

    @Test
    fun `hasBiometricHardware returns true only when phone has biometric hardware`() {
        assertTrue(BiometricAuthHelper.hasBiometricHardware(BiometricAvailability.AVAILABLE))
        assertTrue(BiometricAuthHelper.hasBiometricHardware(BiometricAvailability.NONE_ENROLLED))
        assertTrue(BiometricAuthHelper.hasBiometricHardware(BiometricAvailability.HARDWARE_UNAVAILABLE))
        assertFalse(BiometricAuthHelper.hasBiometricHardware(BiometricAvailability.NO_HARDWARE))
    }

    @Test
    fun `canShowFingerprintLogin requires AVAILABLE hardware and saved credentials`() {
        assertTrue(
            BiometricAuthHelper.canShowFingerprintLogin(
                availability = BiometricAvailability.AVAILABLE,
                hasSavedCredential = true
            )
        )
        assertFalse(
            BiometricAuthHelper.canShowFingerprintLogin(
                availability = BiometricAvailability.AVAILABLE,
                hasSavedCredential = false
            )
        )
        assertFalse(
            BiometricAuthHelper.canShowFingerprintLogin(
                availability = BiometricAvailability.NONE_ENROLLED,
                hasSavedCredential = true
            )
        )
        assertFalse(
            BiometricAuthHelper.canShowFingerprintLogin(
                availability = BiometricAvailability.HARDWARE_UNAVAILABLE,
                hasSavedCredential = true
            )
        )
        assertFalse(
            BiometricAuthHelper.canShowFingerprintLogin(
                availability = BiometricAvailability.NO_HARDWARE,
                hasSavedCredential = true
            )
        )
    }
}
