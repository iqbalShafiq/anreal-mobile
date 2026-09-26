package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformContrast(): Float {
    // TODO(iOS): map UIAccessibilityDarkerSystemColorsEnabled / increase-contrast when iOS platform code lands.
    return 0f
}
