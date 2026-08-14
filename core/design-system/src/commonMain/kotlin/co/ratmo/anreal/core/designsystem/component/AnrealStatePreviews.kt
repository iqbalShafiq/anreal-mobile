package co.ratmo.anreal.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme

@Preview
@Composable
private fun AnrealEmptyPreview() {
    AnrealTheme {
        AnrealEmpty(
            title = "Ask anything about your documents",
            body = "Upload a PDF or image, then ask questions.",
            actionLabel = "Upload",
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AnrealErrorPreview() {
    AnrealTheme {
        AnrealError(
            message = "Could not load chats. Check your connection and try again.",
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun AnrealSkeletonPreview() {
    AnrealTheme {
        AnrealSkeleton()
    }
}
