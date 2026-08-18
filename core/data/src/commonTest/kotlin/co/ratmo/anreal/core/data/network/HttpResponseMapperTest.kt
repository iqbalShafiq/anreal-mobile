package co.ratmo.anreal.core.data.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.domain.util.DataError
import kotlin.test.Test

class HttpResponseMapperTest {

    @Test
    fun maps_common_http_statuses() {
        assertThat(statusToNetworkError(400).kind).isEqualTo(DataError.Network.Kind.BAD_REQUEST)
        assertThat(statusToNetworkError(401).kind).isEqualTo(DataError.Network.Kind.UNAUTHORIZED)
        assertThat(statusToNetworkError(403).kind).isEqualTo(DataError.Network.Kind.FORBIDDEN)
        assertThat(statusToNetworkError(404).kind).isEqualTo(DataError.Network.Kind.NOT_FOUND)
        assertThat(statusToNetworkError(408).kind).isEqualTo(DataError.Network.Kind.REQUEST_TIMEOUT)
        assertThat(statusToNetworkError(409).kind).isEqualTo(DataError.Network.Kind.CONFLICT)
        assertThat(statusToNetworkError(413).kind).isEqualTo(DataError.Network.Kind.PAYLOAD_TOO_LARGE)
        assertThat(statusToNetworkError(422).kind)
            .isEqualTo(DataError.Network.Kind.UNPROCESSABLE_ENTITY)
        assertThat(statusToNetworkError(429).kind)
            .isEqualTo(DataError.Network.Kind.TOO_MANY_REQUESTS)
        assertThat(statusToNetworkError(500).kind).isEqualTo(DataError.Network.Kind.SERVER_ERROR)
        assertThat(statusToNetworkError(503).kind)
            .isEqualTo(DataError.Network.Kind.SERVICE_UNAVAILABLE)
        assertThat(statusToNetworkError(418).kind).isEqualTo(DataError.Network.Kind.UNKNOWN)
    }

    @Test
    fun parses_server_error_message_code_and_primitive_details() {
        val payload = parseServerErrorPayload(
            """{"error":"Storage limit exceeded","code":"STORAGE_QUOTA_EXCEEDED","maxBytes":200,"retry":true}""",
        )

        assertThat(payload).isEqualTo(
            ServerErrorPayload(
                message = "Storage limit exceeded",
                code = "STORAGE_QUOTA_EXCEEDED",
                details = mapOf("maxBytes" to "200", "retry" to "true"),
            ),
        )
    }

    @Test
    fun constructRoute_prefixes_base_url() {
        assertThat(constructRoute("http://127.0.0.1:3001", "/api/chat"))
            .isEqualTo("http://127.0.0.1:3001/api/chat")
        assertThat(constructRoute("http://127.0.0.1:3001", "api/chat"))
            .isEqualTo("http://127.0.0.1:3001/api/chat")
        assertThat(constructRoute("http://127.0.0.1:3001", "http://127.0.0.1:3001/api/chat"))
            .isEqualTo("http://127.0.0.1:3001/api/chat")
    }
}
