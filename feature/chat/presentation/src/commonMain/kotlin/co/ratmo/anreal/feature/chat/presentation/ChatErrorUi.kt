package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.feature.chat.domain.ChatError

fun ChatError.toUiText(): UiText {
    return when (this) {
        ChatError.RunActive -> UiText.StringResource(AnrealCopy.ERROR_RUN_ACTIVE)
        is ChatError.Network -> error.toUiText()
        is ChatError.Local -> error.toUiText()
    }
}
