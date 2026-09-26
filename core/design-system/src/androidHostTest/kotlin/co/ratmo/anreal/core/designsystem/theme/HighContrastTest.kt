package co.ratmo.anreal.core.designsystem.theme

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class HighContrastTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun default_contrast_is_inactive() {
        assertFalse(highContrastActive(0f))
        assertEquals(0f, contrastLevelFor(0f))
    }

    @Test
    fun medium_and_high_contrast_are_active_minimum_is_not() {
        assertTrue(highContrastActive(0.5f))
        assertTrue(highContrastActive(1f))
        assertFalse(highContrastActive(-1f))
    }

    @Test
    fun contrast_level_is_clamped_passthrough() {
        assertEquals(-1f, contrastLevelFor(-2f))
        assertEquals(0.5f, contrastLevelFor(0.5f))
        assertEquals(1f, contrastLevelFor(2f))
    }

    @Test
    fun platform_value_maps_by_api_level() {
        assertEquals(1f, platformContrastValue(sdkInt = 33, uiModeContrast = null, legacyEnabled = true))
        assertEquals(0f, platformContrastValue(sdkInt = 33, uiModeContrast = null, legacyEnabled = false))
        assertEquals(0.5f, platformContrastValue(sdkInt = 34, uiModeContrast = 0.5f, legacyEnabled = false))
        assertEquals(0f, platformContrastValue(sdkInt = 34, uiModeContrast = null, legacyEnabled = true))
    }

    @Test
    fun legacy_setting_reads_full_contrast_on_pre_34() {
        val context = RuntimeEnvironment.getApplication()
        Settings.Secure.putInt(context.contentResolver, "high_text_contrast_enabled", 1)

        var contrast: Float? = null
        composeTestRule.setContent { contrast = rememberPlatformContrast() }
        composeTestRule.waitForIdle()

        assertEquals(1f, contrast)
    }
}
