package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Error

sealed interface ChatError : Error {
    data object RunActive : ChatError
    data class Network(val error: DataError.Network) : ChatError
    data class Local(val error: DataError.Local) : ChatError
}
