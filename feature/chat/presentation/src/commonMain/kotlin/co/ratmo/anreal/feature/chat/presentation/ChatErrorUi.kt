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
        ChatError.ShareNotFound -> UiText.StringResource(AnrealCopy.ERROR_SHARE_NOT_FOUND)
        ChatError.NoActiveShare -> UiText.StringResource(AnrealCopy.ERROR_NO_ACTIVE_SHARE)
        ChatError.ForkTargetNotFound -> UiText.StringResource(AnrealCopy.ERROR_FORK_TARGET_NOT_FOUND)
        ChatError.InvalidForkRequest -> UiText.StringResource(AnrealCopy.ERROR_FORK_INVALID)
        ChatError.ShareUnavailable -> UiText.StringResource(AnrealCopy.ERROR_SHARE_UNAVAILABLE)
        is ChatError.Network -> error.toUiText()
        is ChatError.Local -> error.toUiText()
    }
}
