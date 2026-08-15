package co.ratmo.anreal.core.designsystem.theme

enum class ThemeMode {
    System,
    Light,
    Dark,
}

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
) {
    fun resolveDark(systemDark: Boolean): Boolean {
        return when (mode) {
            ThemeMode.System -> systemDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    }
}

object AnrealBrand {
    const val seedArgb: Int = 0xFFE8A317.toInt()
    /** Near-black canvas the web glass chrome samples. */
    const val canvasArgb: Int = 0xFF050505.toInt()
}
