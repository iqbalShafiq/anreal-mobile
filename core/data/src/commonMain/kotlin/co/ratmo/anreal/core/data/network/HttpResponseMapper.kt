package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.domain.util.DataError

fun statusToNetworkError(statusCode: Int): DataError.Network {
    return when (statusCode) {
        401 -> DataError.Network.UNAUTHORIZED
        403 -> DataError.Network.FORBIDDEN
        404 -> DataError.Network.NOT_FOUND
        408 -> DataError.Network.REQUEST_TIMEOUT
        409 -> DataError.Network.CONFLICT
        413 -> DataError.Network.PAYLOAD_TOO_LARGE
        429 -> DataError.Network.TOO_MANY_REQUESTS
        in 500..599 -> {
            if (statusCode == 503) DataError.Network.SERVICE_UNAVAILABLE
            else DataError.Network.SERVER_ERROR
        }
        else -> DataError.Network.UNKNOWN
    }
}

fun constructRoute(baseUrl: String, route: String): String {
    if (route.contains(baseUrl)) return route
    val normalizedBase = baseUrl.trimEnd('/')
    val normalizedRoute = if (route.startsWith("/")) route else "/$route"
    return normalizedBase + normalizedRoute
}
