package co.ratmo.anreal.core.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.domain.util.DataError
import kotlin.test.Test

class DataErrorToUiTextTest {

    @Test
    fun maps_network_errors_to_resource_keys() {
        assertThat(DataError.Network.NO_INTERNET.toUiText())
            .isEqualTo(UiText.StringResource("error_no_internet"))
        assertThat(DataError.Network.UNAUTHORIZED.toUiText())
            .isEqualTo(UiText.StringResource("error_unauthorized"))
        assertThat(DataError.Network.SERVER_ERROR.toUiText())
            .isEqualTo(UiText.StringResource("error_server"))
    }
}
