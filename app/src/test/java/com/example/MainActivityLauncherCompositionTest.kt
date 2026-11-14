package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.core.AppLanguage
import com.example.core.rememberLocalizedContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLauncherCompositionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun localizedContextWrapper_preservesActivityResultRegistryAndBackDispatcher() {
        composeRule.setContent {
            val activity = composeRule.activity
            val localizedContext = rememberLocalizedContext(activity, AppLanguage.AR)
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides AppLanguage.AR.layoutDirection,
                LocalActivityResultRegistryOwner provides activity,
                LocalOnBackPressedDispatcherOwner provides activity
            ) {
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }
                BackHandler(enabled = true) { }
                Text("Launcher Ready")
            }
        }

        composeRule.onNodeWithText("Launcher Ready").assertIsDisplayed()
    }
}
