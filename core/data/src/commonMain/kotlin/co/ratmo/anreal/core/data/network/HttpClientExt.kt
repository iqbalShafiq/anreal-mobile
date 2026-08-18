package co.ratmo.anreal.core.data.network

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete as ktorDelete
import io.ktor.client.request.accept
import io.ktor.client.request.get as ktorGet
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch as ktorPatch
import io.ktor.client.request.put as ktorPut
import io.ktor.client.request.post as ktorPost
import io.ktor.client.request.preparePost
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readLine
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

data class MultipartFile(
    val bytes: ByteArray,
    val filename: String,
    val contentType: String,
)

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

suspend fun HttpClient.getBytes(
    route: String,
    queryParameters: Map<String, Any?> = emptyMap(),
): Result<ByteArray, DataError.Network> = safeCall {
    ktorGet(urlString = route) {
        queryParameters.forEach { (key, value) ->
            if (value != null) parameter(key, value)
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

suspend inline fun <reified Response : Any> HttpClient.postForResponse(
    route: String,
): Result<Response, DataError.Network> {
    return safeCall {
        ktorPost(urlString = route) {}
    }
}

suspend inline fun <reified Response : Any> HttpClient.postMultipart(
    route: String,
    fields: Map<String, String>,
    file: MultipartFile,
): Result<Response, DataError.Network> {
    val safeFilename = file.filename.replace("\r", "").replace("\n", "").replace("\"", "")
    return safeCall {
        ktorPost(urlString = route) {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        fields.forEach { (name, value) -> append(name, value) }
                        append(
                            key = "file",
                            value = file.bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, file.contentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$safeFilename\"")
                            },
                        )
                    },
                ),
            )
        }
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

suspend inline fun <reified Request : Any, reified Response : Any> HttpClient.put(
    route: String,
    body: Request,
): Result<Response, DataError.Network> {
    return safeCall {
        ktorPut(urlString = route) { setBody(body) }
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
            // defaultRequest advertises JSON for ordinary API calls. A single
            // explicit streaming Accept value prevents proxies from selecting
            // a buffered JSON response, while identity encoding avoids delayed
            // gzip delivery on some Android/proxy combinations.
            headers.remove(HttpHeaders.Accept)
            accept(ContentType.parse("application/x-ndjson"))
            header(HttpHeaders.CacheControl, "no-cache")
            header(HttpHeaders.AcceptEncoding, "identity")
            setBody(body)
            timeout {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                return@execute Result.Error(response.toNetworkError())
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
    } catch (exception: HttpRequestTimeoutException) {
        Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (exception: ConnectTimeoutException) {
        Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (exception: SocketTimeoutException) {
        Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (exception: SerializationException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (exception: CancellationException) {
        throw exception
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
    } catch (exception: HttpRequestTimeoutException) {
        return Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (exception: ConnectTimeoutException) {
        return Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (exception: SocketTimeoutException) {
        return Result.Error(DataError.Network.REQUEST_TIMEOUT)
    } catch (exception: SerializationException) {
        return Result.Error(DataError.Network.SERIALIZATION)
    } catch (exception: CancellationException) {
        throw exception
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
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.Error(DataError.Network.SERIALIZATION)
        }
    }
    return Result.Error(response.toNetworkError())
}

suspend fun HttpResponse.toNetworkError(): DataError.Network {
    val payload = try {
        parseServerErrorPayload(bodyAsText())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        null
    }
    return statusToNetworkError(status.value, payload)
}
