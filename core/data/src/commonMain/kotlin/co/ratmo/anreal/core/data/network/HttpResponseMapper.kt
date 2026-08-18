package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.domain.util.DataError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val errorJson = Json { ignoreUnknownKeys = true }

data class ServerErrorPayload(
    val message: String?,
    val code: String?,
    val details: Map<String, String>,
)

fun statusToNetworkError(
    statusCode: Int,
    payload: ServerErrorPayload? = null,
): DataError.Network {
    val kind = when (statusCode) {
        400 -> DataError.Network.Kind.BAD_REQUEST
        401 -> DataError.Network.Kind.UNAUTHORIZED
        403 -> DataError.Network.Kind.FORBIDDEN
        404 -> DataError.Network.Kind.NOT_FOUND
        408 -> DataError.Network.Kind.REQUEST_TIMEOUT
        409 -> DataError.Network.Kind.CONFLICT
        413 -> DataError.Network.Kind.PAYLOAD_TOO_LARGE
        422 -> DataError.Network.Kind.UNPROCESSABLE_ENTITY
        429 -> DataError.Network.Kind.TOO_MANY_REQUESTS
        503 -> DataError.Network.Kind.SERVICE_UNAVAILABLE
        in 500..599 -> DataError.Network.Kind.SERVER_ERROR
        else -> DataError.Network.Kind.UNKNOWN
    }
    return DataError.Network(
        kind = kind,
        statusCode = statusCode,
        serverMessage = payload?.message,
        code = payload?.code,
        details = payload?.details.orEmpty(),
    )
}

fun parseServerErrorPayload(body: String): ServerErrorPayload? {
    if (body.isBlank()) return null
    val root = runCatching { errorJson.parseToJsonElement(body) as? JsonObject }.getOrNull()
        ?: return null
    val message = root.stringValue("error") ?: root.stringValue("message")
    val code = root.stringValue("code")
    val details = root.mapNotNull { (key, value) ->
        if (key == "error" || key == "message" || key == "code") return@mapNotNull null
        val primitive = value as? JsonPrimitive ?: return@mapNotNull null
        primitive.contentOrNull?.let { key to it }
    }.toMap()
    if (message == null && code == null && details.isEmpty()) return null
    return ServerErrorPayload(message = message, code = code, details = details)
}

private fun JsonObject.stringValue(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

fun constructRoute(baseUrl: String, route: String): String {
    if (route.contains(baseUrl)) return route
    val normalizedBase = baseUrl.trimEnd('/')
    val normalizedRoute = if (route.startsWith("/")) route else "/$route"
    return normalizedBase + normalizedRoute
}
