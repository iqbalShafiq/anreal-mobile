package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get as ktorGet
import io.ktor.client.request.parameter
import io.ktor.client.request.post as ktorPost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

suspend inline fun <reified Response : Any> HttpClient.get(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
): Result<Response, DataError.Network> {
    return safeCall {
        ktorGet(urlString = route) {
            queryParameters.forEach { (key, value) ->
                if (value != null) parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Request : Any, reified Response : Any> HttpClient.post(
    route: String,
    body: Request,
): Result<Response, DataError.Network> {
    return safeCall {
        ktorPost(urlString = route) {
            setBody(body)
        }
    }
}

suspend inline fun HttpClient.post(
    route: String,
): Result<Unit, DataError.Network> {
    return safeCall {
        ktorPost(urlString = route) {}
    }
}

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse,
): Result<T, DataError.Network> {
    val response = try {
        execute()
    } catch (exception: UnresolvedAddressException) {
        return Result.Error(DataError.Network.NO_INTERNET)
    } catch (exception: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: ClientRequestException) {
        return Result.Error(statusToNetworkError(exception.response.status.value))
    } catch (exception: Exception) {
        return Result.Error(DataError.Network.UNKNOWN)
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Network> {
    if (response.status.isSuccess()) {
        return try {
            if (Unit is T) {
                Result.Success(Unit)
            } else {
                Result.Success(response.body())
            }
        } catch (exception: SerializationException) {
            Result.Error(DataError.Network.SERIALIZATION)
        }
    }
    return Result.Error(statusToNetworkError(response.status.value))
}
