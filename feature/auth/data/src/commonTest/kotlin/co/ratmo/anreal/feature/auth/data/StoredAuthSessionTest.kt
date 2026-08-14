package co.ratmo.anreal.feature.auth.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.auth.domain.AuthError
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StoredAuthSessionTest {

    @Test
    fun no_token_is_signed_out() = runTest {
        val session = StoredAuthSession(InMemorySessionTokenStore(), FakeRemote())
        session.status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.Checking)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedOut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun token_and_user_is_signed_in() = runTest {
        val store = InMemorySessionTokenStore().also { it.save("tok") }
        val remote = FakeRemote(user = User(id = "1", email = "a@b.com", name = "Ada"))
        StoredAuthSession(store, remote).status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.Checking)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun stale_token_clears_store_and_signs_out() = runTest {
        val store = InMemorySessionTokenStore().also { it.save("stale") }
        val remote = FakeRemote(user = null)
        StoredAuthSession(store, remote).status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.Checking)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedOut)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(store.token()).isNull()
    }

    @Test
    fun clearing_token_after_sign_in_emits_signed_out() = runTest {
        val store = InMemorySessionTokenStore().also { it.save("tok") }
        val remote = FakeRemote(user = User(id = "1", email = "a@b.com", name = "Ada"))
        StoredAuthSession(store, remote).status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.Checking)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedIn)
            store.clear()
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedOut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun network_error_with_token_stays_signed_in() = runTest {
        val store = InMemorySessionTokenStore().also { it.save("tok") }
        val remote = FakeRemote(
            currentUserResult = Result.Error(AuthError.Network(DataError.Network.NO_INTERNET)),
        )
        StoredAuthSession(store, remote).status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.Checking)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeRemote(
    private val user: User? = null,
    private val currentUserResult: Result<User?, AuthError>? = null,
) : AuthRemoteDataSource {
    override suspend fun signIn(email: String, password: String) = error("unused")
    override suspend fun signUp(name: String, email: String, password: String) = error("unused")
    override suspend fun signOut(): EmptyResult<AuthError> = Result.Success(Unit)
    override suspend fun currentUser(): Result<User?, AuthError> {
        return currentUserResult ?: Result.Success(user)
    }
}
