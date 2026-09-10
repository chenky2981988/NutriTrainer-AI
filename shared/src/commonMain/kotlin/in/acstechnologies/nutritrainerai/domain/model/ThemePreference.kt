package `in`.acstechnologies.nutritrainerai.domain.model

/**
 * The user's app-theme choice. [SYSTEM] follows the device's light/dark setting
 * live; [LIGHT]/[DARK] pin it regardless of the OS.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromNameOrDefault(raw: String?): ThemePreference =
            entries.firstOrNull { it.name == raw } ?: DEFAULT
    }
}
