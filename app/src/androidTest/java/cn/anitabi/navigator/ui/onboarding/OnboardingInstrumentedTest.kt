package cn.anitabi.navigator.ui.onboarding

import android.Manifest
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.anitabi.navigator.AnitabiApplication
import cn.anitabi.navigator.MainActivity
import java.io.FileInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingInstrumentedTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val application: AnitabiApplication
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun completesPermissionsServiceAndRestartFlow() {
        assertFalse(application.container.appSettingsStore.hasCompletedOnboarding())
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("初次使用设置").assertIsDisplayed()
            composeRule.onNodeWithTag("onboarding-start").performClick()
            composeRule.onNodeWithTag("onboarding-permission-request").performClick()

            awaitPermissionDialog()
            reportEvidence("ONBOARDING_PERMISSION_DIALOG_SHOWN")
            grantRequiredPermissions()
            returnFromPermissionDialog()
            continueAfterPermissionGrantIfNeeded()

            awaitTag("onboarding-service-step")
            composeRule.onNodeWithTag("onboarding-service-step")
                .performScrollTo()
                .assertIsDisplayed()
            reportEvidence("ONBOARDING_SERVICE_GUIDE_SHOWN")

            composeRule.onNodeWithTag("onboarding-service-submit")
                .performScrollTo()
                .performClick()
            composeRule.onNodeWithText("搜索 Bangumi").assertIsDisplayed()
            reportEvidence("ONBOARDING_COMPLETED_TO_SEARCH")
        }

        assertTrue(application.container.appSettingsStore.hasCompletedOnboarding())
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("搜索 Bangumi").assertIsDisplayed()
            composeRule.onAllNodesWithTag("onboarding-start").fetchSemanticsNodes()
                .also { nodes -> assertTrue(nodes.isEmpty()) }
            reportEvidence("ONBOARDING_RESTARTED_IN_SEARCH")
        }
    }

    private fun awaitTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitPermissionDialog() {
        repeat(30) {
            val focus = focusedWindow().lowercase()
            if ("permissioncontroller" in focus || "packageinstaller" in focus) return
            Thread.sleep(250L)
        }
        throw AssertionError("The Android runtime-permission dialog did not appear")
    }

    private fun grantRequiredPermissions() {
        grantRuntimePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        grantRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grantRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun grantRuntimePermission(permission: String) {
        shell("pm grant ${application.packageName} $permission")
    }

    private fun returnFromPermissionDialog() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            shell("am force-stop com.google.android.packageinstaller")
            Thread.sleep(500L)
            return
        }
        repeat(4) {
            if (application.packageName in focusedWindow().lowercase()) return
            shell("input keyevent 4")
            Thread.sleep(500L)
        }
        throw AssertionError("The app did not regain focus after permissions were granted")
    }

    private fun continueAfterPermissionGrantIfNeeded() {
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithTag("onboarding-key-input").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("onboarding-permission-continue")
                    .fetchSemanticsNodes().isNotEmpty()
        }
        if (
            composeRule.onAllNodesWithTag("onboarding-permission-continue")
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            composeRule.onNodeWithTag("onboarding-permission-continue").performClick()
        }
    }

    private fun focusedWindow(): String {
        val windowLines = shell("dumpsys window windows").lineSequence().toList()
        val activityLines = shell("dumpsys activity activities").lineSequence().toList()
        return buildList {
            addAll(
                windowLines.filter { line ->
                    "mCurrentFocus" in line || "mFocusedApp" in line
                },
            )
            addAll(
                activityLines.filter { line ->
                    "mResumedActivity" in line || "topResumedActivity" in line ||
                        "ResumedActivity" in line
                },
            )
        }.joinToString("\n")
    }

    private fun reportEvidence(message: String) {
        instrumentation.sendStatus(
            0,
            Bundle().apply { putString(Instrumentation.REPORT_KEY_STREAMRESULT, "$message\n") },
        )
    }

    private fun shell(command: String): String {
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        return descriptor.use {
            FileInputStream(it.fileDescriptor).bufferedReader().use { reader -> reader.readText() }
        }
    }
}
