package co.ratmo.anreal.core.presentation

import co.ratmo.anreal.core.domain.util.Error
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.validation.EmailValidationError
import co.ratmo.anreal.core.domain.validation.NameValidationError
import co.ratmo.anreal.core.domain.validation.PasswordMatchError
import co.ratmo.anreal.core.domain.validation.PasswordValidationError

fun EmailValidationError.toUiText(): UiText {
    return UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL)
}

fun PasswordValidationError.toUiText(): UiText {
    return UiText.StringResource(AnrealCopy.ERROR_PASSWORD_TOO_SHORT)
}

fun NameValidationError.toUiText(): UiText {
    return UiText.StringResource(AnrealCopy.ERROR_NAME_REQUIRED)
}

fun PasswordMatchError.toUiText(): UiText {
    return UiText.StringResource(AnrealCopy.ERROR_PASSWORD_MISMATCH)
}

inline fun <T, E : Error> Result<T, E>.errorText(
    toText: (E) -> UiText,
): UiText? {
    return when (this) {
        is Result.Error -> toText(error)
        is Result.Success -> null
    }
}
