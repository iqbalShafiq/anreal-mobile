package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete as ktorDelete
import io.ktor.client.request.get as ktorGet
import io.ktor.client.request.parameter
import io.ktor.client.request.patch as ktorPatch
import io.ktor.client.request.post as ktorPost
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLine
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

suspend inline fun <reified Request : Any, reified Response : Any> HttpClient.patch(
    route: String,
    body: Request,
): Result<Response, DataError.Network> {
    return safeCall {
        ktorPatch(urlString = route) {
            setBody(body)
        }
    }
}

suspend inline fun HttpClient.delete(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
): Result<Unit, DataError.Network> {
    return safeCall {
        ktorDelete(urlString = route) {
            queryParameters.forEach { (key, value) ->
                if (value != null) parameter(key, value)
            }
        }
    }
}

suspend inline fun <reified Request : Any> HttpClient.delete(
    route: String,
    body: Request,
): Result<Unit, DataError.Network> {
    return safeCall {
        ktorDelete(urlString = route) {
            setBody(body)
        }
    }
}

suspend inline fun <reified Request : Any> HttpClient.postJsonl(
    route: String,
    body: Request,
    noinline onLine: suspend (String) -> Unit,
): Result<Unit, DataError.Network> {
    return try {
        preparePost(urlString = route) {
            setBody(body)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                return@execute Result.Error(statusToNetworkError(response.status.value))
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: break
                if (line.isNotBlank()) onLine(line)
            }
            Result.Success(Unit)
        }
    } catch (exception: UnresolvedAddressException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (exception: SerializationException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: ClientRequestException) {
        Result.Error(statusToNetworkError(exception.response.status.value))
    } catch (exception: Exception) {
        Result.Error(DataError.Network.UNKNOWN)
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
