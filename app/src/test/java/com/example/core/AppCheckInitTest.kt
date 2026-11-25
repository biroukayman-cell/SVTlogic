package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCheckInitTest {

    @Test
    fun `debug build with harness intent token uses debug harness provider`() {
        val mode = resolveAppCheckMode(
            isDebugBuild = true,
            intentDebugToken = "11111111-2222-3333-4444-555555555555",
            hasPersistedHarnessToken = false,
            recaptchaSiteKey = "6Lf-site-key"
        )
        assertEquals(AppCheckProviderMode.DEBUG_HARNESS, mode)
    }

    @Test
    fun `debug build on physical phone without harness token uses recaptcha`() {
        val mode = resolveAppCheckMode(
            isDebugBuild = true,
            intentDebugToken = null,
            hasPersistedHarnessToken = false,
            recaptchaSiteKey = "6Lf-site-key"
        )
        assertEquals(AppCheckProviderMode.RECAPTCHA, mode)
    }

    @Test
    fun `debug build with persisted harness token keeps debug harness provider`() {
        val mode = resolveAppCheckMode(
            isDebugBuild = true,
            intentDebugToken = null,
            hasPersistedHarnessToken = true,
            recaptchaSiteKey = "6Lf-site-key"
        )
        assertEquals(AppCheckProviderMode.DEBUG_HARNESS, mode)
    }

    @Test
    fun `release build always uses recaptcha when site key present`() {
        val mode = resolveAppCheckMode(
            isDebugBuild = false,
            intentDebugToken = "should-be-ignored-in-release",
            hasPersistedHarnessToken = true,
            recaptchaSiteKey = "6Lf-site-key"
        )
        assertEquals(AppCheckProviderMode.RECAPTCHA, mode)
    }

    @Test
    fun `diagnostic tag includes provider mode and version code`() {
        val tag = appCheckDiagnosticTag()
        assert(tag.contains("mode="))
        assert(tag.contains("v="))
    }
}
