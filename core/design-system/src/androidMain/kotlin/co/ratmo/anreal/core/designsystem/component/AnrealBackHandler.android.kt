package co.ratmo.anreal.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun AnrealBackHandler(enabled: Boolean, onBack: () -> Unit) {
    if (enabled) {
        BackHandler(onBack = onBack)
    }
}
