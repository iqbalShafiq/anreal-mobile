package co.ratmo.anreal.core.domain.validation

import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Error
import co.ratmo.anreal.core.domain.util.Result

enum class EmailValidationError : Error {
    EMPTY,
    INVALID,
}

enum class PasswordValidationError : Error {
    TOO_SHORT,
}

enum class NameValidationError : Error {
    EMPTY,
}

enum class PasswordMatchError : Error {
    MISMATCH,
}

private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

fun validateEmail(raw: String): EmptyResult<EmailValidationError> {
    val email = raw.trim()
    if (email.isEmpty()) return Result.Error(EmailValidationError.EMPTY)
    if (!emailRegex.matches(email)) return Result.Error(EmailValidationError.INVALID)
    return Result.Success(Unit)
}

fun validatePassword(raw: String, minLength: Int = 8): EmptyResult<PasswordValidationError> {
    if (raw.length < minLength) return Result.Error(PasswordValidationError.TOO_SHORT)
    return Result.Success(Unit)
}

fun validateRequiredName(raw: String): EmptyResult<NameValidationError> {
    if (raw.trim().isEmpty()) return Result.Error(NameValidationError.EMPTY)
    return Result.Success(Unit)
}

fun validatePasswordMatch(
    password: String,
    confirm: String,
): EmptyResult<PasswordMatchError> {
    if (password != confirm) return Result.Error(PasswordMatchError.MISMATCH)
    return Result.Success(Unit)
}
