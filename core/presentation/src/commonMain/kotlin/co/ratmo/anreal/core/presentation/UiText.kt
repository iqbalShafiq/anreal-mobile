package co.ratmo.anreal.core.presentation

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResource(val key: String, val args: List<String> = emptyList()) : UiText
}
