package co.ratmo.anreal.core.presentation

object AnrealCopy {
    const val ERROR_NO_INTERNET = "error_no_internet"
    const val ERROR_UNAUTHORIZED = "error_unauthorized"
    const val ERROR_FORBIDDEN = "error_forbidden"
    const val ERROR_NOT_FOUND = "error_not_found"
    const val ERROR_TIMEOUT = "error_timeout"
    const val ERROR_CONFLICT = "error_conflict"
    const val ERROR_TOO_MANY_REQUESTS = "error_too_many_requests"
    const val ERROR_SERVER = "error_server"
    const val ERROR_UNKNOWN = "error_unknown"
    const val ERROR_DISK_FULL = "error_disk_full"
    const val ERROR_INVALID_EMAIL = "error_invalid_email"
    const val ERROR_PASSWORD_TOO_SHORT = "error_password_too_short"
    const val ERROR_PASSWORD_MISMATCH = "error_password_mismatch"
    const val ERROR_NAME_REQUIRED = "error_name_required"
    const val ERROR_INVALID_CREDENTIALS = "error_invalid_credentials"
    const val ERROR_EMAIL_TAKEN = "error_email_taken"
    const val ERROR_SIGN_IN_FAILED = "error_sign_in_failed"
    const val ERROR_SIGN_UP_FAILED = "error_sign_up_failed"

    const val LABEL_EMAIL = "label_email"
    const val LABEL_PASSWORD = "label_password"
    const val LABEL_NAME = "label_name"
    const val LABEL_CONFIRM_PASSWORD = "label_confirm_password"
    const val ACTION_CONTINUE = "action_continue"
    const val ACTION_SIGNING_IN = "action_signing_in"
    const val ACTION_CREATE_ACCOUNT = "action_create_account"
    const val ACTION_CREATING_ACCOUNT = "action_creating_account"
    const val ACTION_RETRY = "action_retry"
    const val ACTION_SHOW_PASSWORD = "action_show_password"
    const val ACTION_HIDE_PASSWORD = "action_hide_password"
    const val STATUS_LOADING = "status_loading"

    fun get(key: String): String {
        return when (key) {
            ERROR_NO_INTERNET -> "Check your connection and try again."
            ERROR_UNAUTHORIZED -> "Sign in to continue."
            ERROR_FORBIDDEN -> "You don't have access to that."
            ERROR_NOT_FOUND -> "We couldn't find that."
            ERROR_TIMEOUT -> "That took too long. Try again."
            ERROR_CONFLICT -> "That conflicts with the current state. Try again."
            ERROR_TOO_MANY_REQUESTS -> "Too many attempts. Wait a moment and try again."
            ERROR_SERVER -> "The server had a problem. Try again."
            ERROR_UNKNOWN -> "Something went wrong. Try again."
            ERROR_DISK_FULL -> "Not enough storage to finish that."
            ERROR_INVALID_EMAIL -> "Enter a valid email address."
            ERROR_PASSWORD_TOO_SHORT -> "Password must be at least 8 characters."
            ERROR_PASSWORD_MISMATCH -> "Passwords do not match."
            ERROR_NAME_REQUIRED -> "Name is required."
            ERROR_INVALID_CREDENTIALS -> "Invalid email or password."
            ERROR_EMAIL_TAKEN -> "An account with that email already exists."
            ERROR_SIGN_IN_FAILED -> "Could not sign in. Check your connection and try again."
            ERROR_SIGN_UP_FAILED -> "Could not create an account. Try again."
            LABEL_EMAIL -> "Email"
            LABEL_PASSWORD -> "Password"
            LABEL_NAME -> "Name"
            LABEL_CONFIRM_PASSWORD -> "Confirm password"
            ACTION_CONTINUE -> "Continue"
            ACTION_SIGNING_IN -> "Signing in…"
            ACTION_CREATE_ACCOUNT -> "Create account"
            ACTION_CREATING_ACCOUNT -> "Creating account…"
            ACTION_RETRY -> "Retry"
            ACTION_SHOW_PASSWORD -> "Show"
            ACTION_HIDE_PASSWORD -> "Hide"
            STATUS_LOADING -> "Loading"
            else -> key
        }
    }
}

fun UiText.asString(): String {
    return when (this) {
        is UiText.DynamicString -> value
        is UiText.StringResource -> interpolate(AnrealCopy.get(key), args)
    }
}

internal fun interpolate(template: String, args: List<String>): String {
    return args.foldIndexed(template) { index, acc, arg ->
        acc.replace("{$index}", arg)
    }
}
