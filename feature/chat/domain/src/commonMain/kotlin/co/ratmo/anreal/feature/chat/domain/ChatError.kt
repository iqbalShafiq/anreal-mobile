package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Error

sealed interface ChatError : Error {
    data object RunActive : ChatError
    data object NoActiveRun : ChatError
    data object InteractionNotFound : ChatError
    data object InteractionExpired : ChatError
    data object InteractionHandled : ChatError
    data object InteractionPolicyUnavailable : ChatError
    data object InteractionStageInvalid : ChatError
    data object ShareNotFound : ChatError
    data object NoActiveShare : ChatError
    data object ForkTargetNotFound : ChatError
    data object InvalidForkRequest : ChatError
    data object ShareUnavailable : ChatError
    data class Network(val error: DataError.Network) : ChatError
    data class Local(val error: DataError.Local) : ChatError
}
