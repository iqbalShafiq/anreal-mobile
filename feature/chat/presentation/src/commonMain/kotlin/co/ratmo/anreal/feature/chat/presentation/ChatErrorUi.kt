package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.feature.chat.domain.ChatError

fun ChatError.toUiText(): UiText {
    return when (this) {
        ChatError.RunActive -> UiText.StringResource(AnrealCopy.ERROR_RUN_ACTIVE)
        ChatError.NoActiveRun -> UiText.StringResource(AnrealCopy.ERROR_NO_ACTIVE_RUN)
        ChatError.InteractionNotFound -> UiText.StringResource(AnrealCopy.ERROR_INTERACTION_GONE)
        ChatError.InteractionExpired -> UiText.StringResource(AnrealCopy.ERROR_INTERACTION_EXPIRED)
        ChatError.InteractionHandled -> UiText.StringResource(AnrealCopy.ERROR_INTERACTION_HANDLED)
        ChatError.InteractionPolicyUnavailable ->
            UiText.StringResource(AnrealCopy.ERROR_INTERACTION_UNAVAILABLE)
        ChatError.InteractionStageInvalid -> UiText.StringResource(AnrealCopy.ERROR_INTERACTION_INVALID)
        is ChatError.Network -> error.toUiText()
        is ChatError.Local -> error.toUiText()
    }
}
