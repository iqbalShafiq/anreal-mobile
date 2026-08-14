package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.data.auth.SessionTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import co.ratmo.anreal.core.data.auth.parseSessionToken
import co.ratmo.anreal.core.data.auth.sessionCookieHeader
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
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
            install(Logging) {
                level = LogLevel.INFO
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Accept, "application/json")
            }
        }.also { client ->
            client.plugin(HttpSend).intercept { request ->
                tokenStore.token()?.let { token ->
                    request.headers.append(HttpHeaders.Cookie, sessionCookieHeader(token))
                }
                val call = execute(request)
                if (call.response.status == HttpStatusCode.Unauthorized) {
                    tokenStore.clear()
                } else {
                    parseSessionToken(call.response.headers.getAll(HttpHeaders.SetCookie).orEmpty())
                        ?.let { tokenStore.save(it) }
                }
                call
            }
        }
    }
}
