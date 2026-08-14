package co.ratmo.anreal.core.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.validation.EmailValidationError
import co.ratmo.anreal.core.domain.validation.NameValidationError
import co.ratmo.anreal.core.domain.validation.PasswordMatchError
import co.ratmo.anreal.core.domain.validation.PasswordValidationError
import kotlin.test.Test

class ValidationToUiTextTest {

    @Test
    fun maps_validation_errors_to_copy_keys() {
        assertThat(EmailValidationError.EMPTY.toUiText())
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL))
        assertThat(EmailValidationError.INVALID.toUiText())
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL))
        assertThat(PasswordValidationError.TOO_SHORT.toUiText())
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_PASSWORD_TOO_SHORT))
        assertThat(NameValidationError.EMPTY.toUiText())
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_NAME_REQUIRED))
        assertThat(PasswordMatchError.MISMATCH.toUiText())
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_PASSWORD_MISMATCH))
    }

    @Test
    fun errorText_maps_error_and_ignores_success() {
        val error: Result<Unit, EmailValidationError> = Result.Error(EmailValidationError.INVALID)
        val success: Result<Unit, EmailValidationError> = Result.Success(Unit)

        assertThat(error.errorText { it.toUiText() })
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL))
        assertThat(success.errorText { it.toUiText() }).isNull()
    }

    @Test
    fun asString_interpolates_args() {
        val text = UiText.DynamicString("Ada")
        assertThat(text.asString()).isEqualTo("Ada")
        assertThat(interpolate("Hello {0}", listOf("Ada"))).isEqualTo("Hello Ada")
    }
}
