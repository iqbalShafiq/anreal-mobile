package co.ratmo.anreal.core.presentation

import co.ratmo.anreal.core.domain.util.DataError

fun DataError.toUiText(): UiText {
    val key = when (this) {
        DataError.Network.NO_INTERNET -> "error_no_internet"
        DataError.Network.UNAUTHORIZED -> "error_unauthorized"
        DataError.Network.FORBIDDEN -> "error_forbidden"
        DataError.Network.NOT_FOUND -> "error_not_found"
        DataError.Network.REQUEST_TIMEOUT -> "error_timeout"
        DataError.Network.CONFLICT -> "error_conflict"
        DataError.Network.TOO_MANY_REQUESTS -> "error_too_many_requests"
        DataError.Network.SERVER_ERROR,
        DataError.Network.SERVICE_UNAVAILABLE,
        -> "error_server"
        DataError.Network.BAD_REQUEST,
        DataError.Network.PAYLOAD_TOO_LARGE,
        DataError.Network.SERIALIZATION,
        DataError.Network.UNKNOWN,
        -> "error_unknown"
        DataError.Local.DISK_FULL -> "error_disk_full"
        DataError.Local.NOT_FOUND -> "error_not_found"
        DataError.Local.UNKNOWN -> "error_unknown"
    }
    return UiText.StringResource(key)
}
