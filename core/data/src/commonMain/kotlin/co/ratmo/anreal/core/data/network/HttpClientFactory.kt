package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.data.auth.SessionTokenStore
import co.ratmo.anreal.core.data.auth.SESSION_TOKEN_HEADER
import co.ratmo.anreal.core.data.auth.responseSessionToken
import co.touchlab.kermit.Logger as KermitLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(
        engine: HttpClientEngine,
        tokenStore: SessionTokenStore,
        baseUrl: String,
        enableNetworkLogging: Boolean = false,
        networkLogger: KtorLogger = KermitNetworkLogger,
    ): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS
            }
            if (enableNetworkLogging) {
                install(Logging) {
                    logger = networkLogger
                    level = LogLevel.INFO
                    sanitizeHeader { header ->
                        header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                            header.equals(HttpHeaders.Cookie, ignoreCase = true) ||
                            header.equals(HttpHeaders.SetCookie, ignoreCase = true)
                    }
                }
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Accept, "application/json")
            }
        }.also { client ->
            client.plugin(HttpSend).intercept { request ->
                tokenStore.token()?.let { token ->
                    request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
                val call = execute(request)
                if (call.response.status == HttpStatusCode.Unauthorized) {
                    tokenStore.clear()
                } else {
                    responseSessionToken(
                        bearerHeader = call.response.headers[SESSION_TOKEN_HEADER],
                        setCookieHeaders = call.response.headers.getAll(HttpHeaders.SetCookie).orEmpty(),
                    )
                        ?.let { tokenStore.save(it) }
                }
                call
            }
        }
    }
}

internal const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 60_000L
internal const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 15_000L
internal const val DEFAULT_SOCKET_TIMEOUT_MILLIS = 60_000L

private object KermitNetworkLogger : KtorLogger {
    private val logger = KermitLogger.withTag("AnrealApi")

    override fun log(message: String) {
        logger.d { message }
    }
}
