package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.component.SiteBuildPanel
import co.ratmo.anreal.feature.chat.presentation.component.siteFailedState
import co.ratmo.anreal.feature.chat.presentation.component.siteReadyState
import co.ratmo.anreal.feature.chat.presentation.component.siteVersionEntries
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class SiteBuildPanelScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sitePanelReadyLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                SiteBuildPanel(build = siteReadyState(), versions = siteVersionEntries())
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(AnrealCopy.get(AnrealCopy.SITE_PREVIEW_ACTION)).assertExists()
        composeTestRule.onNodeWithText(AnrealCopy.get(AnrealCopy.SITE_PREVIEW_ACTION)).captureRoboImage()
    }

    @Test
    fun sitePanelFailedLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                SiteBuildPanel(build = siteFailedState(), versions = siteVersionEntries())
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(AnrealCopy.get(AnrealCopy.SITE_RETRY_ACTION)).assertExists()
        composeTestRule.onNodeWithText(AnrealCopy.get(AnrealCopy.SITE_RETRY_ACTION)).captureRoboImage()
    }
}
