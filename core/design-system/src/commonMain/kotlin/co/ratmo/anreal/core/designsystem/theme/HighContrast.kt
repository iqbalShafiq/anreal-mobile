package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Android exposes platform contrast since API 34 (UiModeManager.getContrast()). */
internal const val PlatformContrastApiLevel = 34

/** True when the platform asks for elevated contrast (medium or high). */
val LocalAnrealHighContrast = staticCompositionLocalOf { false }

@Composable
expect fun rememberPlatformContrast(): Float

internal fun highContrastActive(platformContrast: Float): Boolean = platformContrast > 0f

internal fun contrastLevelFor(platformContrast: Float): Float = platformContrast.coerceIn(-1f, 1f)

internal fun platformContrastValue(
    sdkInt: Int,
    uiModeContrast: Float?,
    legacyEnabled: Boolean,
): Float = when {
    sdkInt >= PlatformContrastApiLevel -> uiModeContrast ?: 0f
    legacyEnabled -> 1f
    else -> 0f
}
