package co.ratmo.anreal.core.data.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.domain.util.DataError
import kotlin.test.Test

class HttpResponseMapperTest {

    @Test
    fun maps_common_http_statuses() {
        assertThat(statusToNetworkError(401)).isEqualTo(DataError.Network.UNAUTHORIZED)
        assertThat(statusToNetworkError(403)).isEqualTo(DataError.Network.FORBIDDEN)
        assertThat(statusToNetworkError(404)).isEqualTo(DataError.Network.NOT_FOUND)
        assertThat(statusToNetworkError(408)).isEqualTo(DataError.Network.REQUEST_TIMEOUT)
        assertThat(statusToNetworkError(409)).isEqualTo(DataError.Network.CONFLICT)
        assertThat(statusToNetworkError(413)).isEqualTo(DataError.Network.PAYLOAD_TOO_LARGE)
        assertThat(statusToNetworkError(429)).isEqualTo(DataError.Network.TOO_MANY_REQUESTS)
        assertThat(statusToNetworkError(500)).isEqualTo(DataError.Network.SERVER_ERROR)
        assertThat(statusToNetworkError(503)).isEqualTo(DataError.Network.SERVICE_UNAVAILABLE)
        assertThat(statusToNetworkError(418)).isEqualTo(DataError.Network.UNKNOWN)
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
