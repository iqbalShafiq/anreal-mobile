package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AnrealTypographyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun every_style_uses_geist() {
        var typography: Typography? = null
        composeTestRule.setContent {
            AnrealTheme(settings = ThemeSettings(dynamicColor = false)) {
                typography = MaterialTheme.typography
            }
        }
        composeTestRule.waitForIdle()
        val t = typography ?: error("typography not captured")
        val styles: List<Pair<String, TextStyle>> = listOf(
            "displayLarge" to t.displayLarge,
            "displayMedium" to t.displayMedium,
            "displaySmall" to t.displaySmall,
            "headlineLarge" to t.headlineLarge,
            "headlineMedium" to t.headlineMedium,
            "headlineSmall" to t.headlineSmall,
            "titleLarge" to t.titleLarge,
            "titleMedium" to t.titleMedium,
            "titleSmall" to t.titleSmall,
            "bodyLarge" to t.bodyLarge,
            "bodyMedium" to t.bodyMedium,
            "bodySmall" to t.bodySmall,
            "labelLarge" to t.labelLarge,
            "labelMedium" to t.labelMedium,
            "labelSmall" to t.labelSmall,
            "displayLargeEmphasized" to t.displayLargeEmphasized,
            "displayMediumEmphasized" to t.displayMediumEmphasized,
            "displaySmallEmphasized" to t.displaySmallEmphasized,
            "headlineLargeEmphasized" to t.headlineLargeEmphasized,
            "headlineMediumEmphasized" to t.headlineMediumEmphasized,
            "headlineSmallEmphasized" to t.headlineSmallEmphasized,
            "titleLargeEmphasized" to t.titleLargeEmphasized,
            "titleMediumEmphasized" to t.titleMediumEmphasized,
            "titleSmallEmphasized" to t.titleSmallEmphasized,
            "bodyLargeEmphasized" to t.bodyLargeEmphasized,
            "bodyMediumEmphasized" to t.bodyMediumEmphasized,
            "bodySmallEmphasized" to t.bodySmallEmphasized,
            "labelLargeEmphasized" to t.labelLargeEmphasized,
            "labelMediumEmphasized" to t.labelMediumEmphasized,
            "labelSmallEmphasized" to t.labelSmallEmphasized,
        )
        for ((name, style) in styles) {
            val family = style.fontFamily
            assertTrue(family is FontListFontFamily, "$name must use the Geist family")
            assertEquals(3, family.fonts.size, "$name must carry 3 Geist weights")
        }
    }

    @Test
    fun mono_family_is_single_weight() {
        var family: FontFamily? = null
        composeTestRule.setContent { family = AnrealFontFamily.Mono() }
        composeTestRule.waitForIdle()
        val resolved = family ?: error("mono family not captured")
        assertTrue(resolved is FontListFontFamily, "Mono must be a custom family")
        assertEquals(1, resolved.fonts.size)
    }
}
