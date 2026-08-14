package co.ratmo.anreal.core.domain.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.domain.util.Result
import kotlin.test.Test

class ValidatorsTest {

    @Test
    fun email_rejects_blank_and_malformed() {
        assertThat(validateEmail("")).isEqualTo(Result.Error(EmailValidationError.EMPTY))
        assertThat(validateEmail("   ")).isEqualTo(Result.Error(EmailValidationError.EMPTY))
        assertThat(validateEmail("not-an-email")).isEqualTo(Result.Error(EmailValidationError.INVALID))
        assertThat(validateEmail("you@company.com")).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun password_requires_eight_characters() {
        assertThat(validatePassword("short")).isEqualTo(Result.Error(PasswordValidationError.TOO_SHORT))
        assertThat(validatePassword("12345678")).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun required_name_rejects_blank() {
        assertThat(validateRequiredName("")).isEqualTo(Result.Error(NameValidationError.EMPTY))
        assertThat(validateRequiredName("  ")).isEqualTo(Result.Error(NameValidationError.EMPTY))
        assertThat(validateRequiredName("Ada")).isEqualTo(Result.Success(Unit))
    }

    @Test
    fun password_match_rejects_mismatch() {
        assertThat(validatePasswordMatch("password1", "password2"))
            .isEqualTo(Result.Error(PasswordMatchError.MISMATCH))
        assertThat(validatePasswordMatch("password1", "password1"))
            .isEqualTo(Result.Success(Unit))
    }
}
