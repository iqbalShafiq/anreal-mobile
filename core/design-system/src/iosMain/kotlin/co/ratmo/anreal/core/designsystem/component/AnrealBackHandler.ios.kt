package co.ratmo.anreal.core.designsystem.component

import androidx.compose.runtime.Composable

@Composable
actual fun AnrealBackHandler(
    @Suppress("UNUSED_PARAMETER") enabled: Boolean,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
) {
    // TODO: wire iOS swipe-back to leave project-workspace.
}
