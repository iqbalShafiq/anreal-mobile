package co.ratmo.anreal.feature.chat.presentation

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.feature.chat.presentation.preview.chatEmptyPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import kotlin.test.Test

class ChatAtmosphereTest {

    @Test
    fun emptyThreadKeepsAuroraAfterSessionCatalogLoads() {
        val state = chatEmptyPreviewState().copy(
            selectedSessionId = "server-session",
            sessions = listOf(
                ChatSessionUi(
                    id = "server-session",
                    title = "A server-resolved title",
                    unread = false,
                ),
            ),
            catalogLoading = false,
        )

        assertThat(shouldShowChatAurora(state)).isTrue()
    }

    @Test
    fun firstMessageSwitchesChatToSurface() {
        assertThat(shouldShowChatAurora(chatPopulatedPreviewState())).isFalse()
    }
}
