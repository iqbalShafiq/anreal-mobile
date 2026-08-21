package co.ratmo.anreal.core.designsystem.component

import androidx.compose.runtime.Composable

@Composable
expect fun AnrealBackHandler(enabled: Boolean, onBack: () -> Unit)
