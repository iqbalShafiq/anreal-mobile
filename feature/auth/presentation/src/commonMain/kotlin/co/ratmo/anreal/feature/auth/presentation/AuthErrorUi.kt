package co.ratmo.anreal.feature.auth.presentation

import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.feature.auth.domain.AuthError

fun AuthError.toUiText(): UiText {
    return when (this) {
        AuthError.InvalidCredentials -> UiText.StringResource(AnrealCopy.ERROR_INVALID_CREDENTIALS)
        AuthError.EmailTaken -> UiText.StringResource(AnrealCopy.ERROR_EMAIL_TAKEN)
        is AuthError.Network -> error.toUiText()
    }
}
