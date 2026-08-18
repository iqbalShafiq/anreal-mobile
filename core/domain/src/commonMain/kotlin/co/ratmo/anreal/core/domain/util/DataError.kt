package co.ratmo.anreal.core.domain.util

sealed interface DataError : Error {
    data class Network(
        val kind: Kind,
        val statusCode: Int? = null,
        val serverMessage: String? = null,
        val code: String? = null,
        val details: Map<String, String> = emptyMap(),
    ) : DataError {
        enum class Kind {
            BAD_REQUEST,
            REQUEST_TIMEOUT,
            UNAUTHORIZED,
            FORBIDDEN,
            NOT_FOUND,
            CONFLICT,
            UNPROCESSABLE_ENTITY,
            TOO_MANY_REQUESTS,
            NO_INTERNET,
            PAYLOAD_TOO_LARGE,
            SERVER_ERROR,
            SERVICE_UNAVAILABLE,
            SERIALIZATION,
            UNKNOWN,
        }

        companion object {
            val BAD_REQUEST = Network(Kind.BAD_REQUEST)
            val REQUEST_TIMEOUT = Network(Kind.REQUEST_TIMEOUT)
            val UNAUTHORIZED = Network(Kind.UNAUTHORIZED)
            val FORBIDDEN = Network(Kind.FORBIDDEN)
            val NOT_FOUND = Network(Kind.NOT_FOUND)
            val CONFLICT = Network(Kind.CONFLICT)
            val UNPROCESSABLE_ENTITY = Network(Kind.UNPROCESSABLE_ENTITY)
            val TOO_MANY_REQUESTS = Network(Kind.TOO_MANY_REQUESTS)
            val NO_INTERNET = Network(Kind.NO_INTERNET)
            val PAYLOAD_TOO_LARGE = Network(Kind.PAYLOAD_TOO_LARGE)
            val SERVER_ERROR = Network(Kind.SERVER_ERROR)
            val SERVICE_UNAVAILABLE = Network(Kind.SERVICE_UNAVAILABLE)
            val SERIALIZATION = Network(Kind.SERIALIZATION)
            val UNKNOWN = Network(Kind.UNKNOWN)
        }
    }

    enum class Local : DataError {
        DISK_FULL,
        NOT_FOUND,
        UNKNOWN,
    }
}
