package co.ratmo.anreal.feature.auth.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.auth.domain.AuthError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorAuthDataSourceTest {

    @Test
    fun signIn_reads_user_and_stores_bearer_token() = runTest {
        val store = InMemorySessionTokenStore()
        val source = source(
            store = store,
            path = "/api/auth/sign-in/email",
            status = HttpStatusCode.OK,
            body = """{"user":{"id":"u1","email":"a@b.com","name":"Ada"}}""",
            authToken = "tok-1",
        )

        val result = source.signIn("a@b.com", "password1")

        assertThat(result).isEqualTo(
            Result.Success(User(id = "u1", email = "a@b.com", name = "Ada")),
        )
        assertThat(store.token()).isEqualTo("tok-1")
    }

    @Test
    fun signIn_maps_401_to_invalid_credentials() = runTest {
        val source = source(
            path = "/api/auth/sign-in/email",
            status = HttpStatusCode.Unauthorized,
            body = """{"message":"no"}""",
        )

        assertThat(source.signIn("a@b.com", "badpass1"))
            .isEqualTo(Result.Error(AuthError.InvalidCredentials))
    }

    @Test
    fun signUp_maps_better_auth_existing_user_body_to_email_taken() = runTest {
        val source = source(
            path = "/api/auth/sign-up/email",
            status = HttpStatusCode.BadRequest,
            body = """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL","message":"User already exists"}""",
        )

        assertThat(source.signUp("Ada", "a@b.com", "password1"))
            .isEqualTo(Result.Error(AuthError.EmailTaken))
    }

    @Test
    fun currentUser_treats_401_as_signed_out() = runTest {
        val source = source(
            path = "/api/auth/get-session",
            status = HttpStatusCode.Unauthorized,
            body = """{"error":"no"}""",
        )

        assertThat(source.currentUser()).isEqualTo(Result.Success(null))
    }
}

private fun source(
    store: InMemorySessionTokenStore = InMemorySessionTokenStore(),
    path: String,
    status: HttpStatusCode,
    body: String,
    authToken: String? = null,
): KtorAuthDataSource {
    val engine = MockEngine {
        val headers = if (authToken == null) {
            headersOf(HttpHeaders.ContentType, "application/json")
        } else {
            headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                "set-auth-token" to listOf(authToken),
            )
        }
        respond(content = body, status = status, headers = headers)
    }
    return KtorAuthDataSource(
        httpClient = HttpClientFactory.create(
            engine = engine,
            tokenStore = store,
            baseUrl = "http://127.0.0.1:3001",
        ),
        tokenStore = store,
    )
}
