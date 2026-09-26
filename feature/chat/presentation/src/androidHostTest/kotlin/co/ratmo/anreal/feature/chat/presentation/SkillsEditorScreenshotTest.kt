package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.feature.chat.presentation.component.SkillsManagementScreen
import co.ratmo.anreal.feature.chat.presentation.component.skillEditorEditState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SkillsEditorScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun skillEditorEditLight() {
        composeTestRule.setContent {
            AnrealPreview(dark = false) {
                SkillsManagementScreen(state = skillEditorEditState(), onAction = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage()
    }
}
