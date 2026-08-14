package co.ratmo.anreal.core.data.auth

const val SESSION_COOKIE_NAME = "better-auth.session_token"

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
