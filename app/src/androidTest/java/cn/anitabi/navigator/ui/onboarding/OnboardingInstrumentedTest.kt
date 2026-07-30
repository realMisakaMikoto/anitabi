package cn.anitabi.navigator.ui.onboarding

import android.Manifest
import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
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
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            grantAndroid8PermissionDialog()
            return
        }
        grantRuntimePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        grantRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grantRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun grantAndroid8PermissionDialog() {
        repeat(30) {
            val focus = focusedWindow().lowercase()
            if ("permissioncontroller" !in focus && "packageinstaller" !in focus) return
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val allowButton = listOf(
                "com.android.packageinstaller:id/permission_allow_button",
                "com.google.android.packageinstaller:id/permission_allow_button",
            ).firstNotNullOfOrNull { viewId ->
                root?.findAccessibilityNodeInfosByViewId(viewId)?.firstOrNull()
            } ?: listOf("ALLOW", "Allow", "允许").firstNotNullOfOrNull { label ->
                root?.findAccessibilityNodeInfosByText(label)?.firstOrNull { node -> node.isClickable }
            }
            if (allowButton != null) {
                check(allowButton.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                instrumentation.waitForIdleSync()
            }
            Thread.sleep(500L)
        }
        throw AssertionError("The Android 8 permission dialog did not return to the app")
    }

    private fun grantRuntimePermission(permission: String) {
        shell("pm grant ${application.packageName} $permission")
    }

    private fun returnFromPermissionDialog() {
        repeat(30) {
            if (application.packageName in focusedWindow().lowercase()) return
            Thread.sleep(500L)
        }
        shell("input keyevent 4")
        repeat(10) {
            if (application.packageName in focusedWindow().lowercase()) return
            Thread.sleep(500L)
        }
        throw AssertionError("The app did not regain focus after permissions were granted")
    }

    private fun continueAfterPermissionGrantIfNeeded() {
        composeRule.waitUntil(timeoutMillis = 15_000L) {
            composeRule.onAllNodesWithTag("onboarding-service-step").fetchSemanticsNodes().isNotEmpty() ||
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
