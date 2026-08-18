package co.ratmo.anreal.core.data

enum class AppEnvironment {
    Development,
    Staging,
    Production,
    ;

    val stubApi: Boolean get() = this == Development

    companion object {
        fun parse(raw: String): AppEnvironment {
            return when (raw.trim().lowercase()) {
                "development", "dev" -> Development
                "staging", "stage" -> Staging
                "production", "prod" -> Production
                else -> Development
            }
        }
    }
}

data class AppConfig(
    val environment: AppEnvironment,
    val baseUrl: String,
    val isDebug: Boolean = false,
) {
    constructor(baseUrl: String) : this(
        environment = AppEnvironment.Development,
        baseUrl = baseUrl,
    )
}
