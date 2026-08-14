package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.data.auth.SessionTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
            install(Auth) {
                bearer {
                    loadTokens {
                        tokenStore.token()?.let { BearerTokens(it, "") }
                    }
                }
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Accept, "application/json")
            }
        }
    }
}
