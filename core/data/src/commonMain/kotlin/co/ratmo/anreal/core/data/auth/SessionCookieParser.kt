package co.ratmo.anreal.core.data.auth

const val SESSION_COOKIE_NAME = "better-auth.session_token"
const val SESSION_TOKEN_HEADER = "set-auth-token"

fun parseSessionToken(setCookieHeaders: List<String>): String? {
    return setCookieHeaders.firstNotNullOfOrNull { header ->
        header.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$SESSION_COOKIE_NAME=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotBlank() }
    }
}

fun sessionCookieHeader(token: String): String = "$SESSION_COOKIE_NAME=$token"

fun responseSessionToken(
    bearerHeader: String?,
    setCookieHeaders: List<String>,
): String? = bearerHeader?.trim()?.takeIf { it.isNotEmpty() }
    ?: parseSessionToken(setCookieHeaders)
