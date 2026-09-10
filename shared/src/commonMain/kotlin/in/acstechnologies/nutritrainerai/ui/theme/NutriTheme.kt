package `in`.acstechnologies.nutritrainerai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import `in`.acstechnologies.nutritrainerai.domain.model.ThemePreference

/**
 * The app's Material 3 theme. Calm greens (PRD §4.1.3 "green border, pale-green
 * fill" for selection; "Calm guidance — no guilt or alarm"). One entry point so
 * every screen renders consistently.
 */
private val Green = Color(0xFF2E7D32)
private val GreenDark = Color(0xFF7CC47F)
private val PaleGreen = Color(0xFFDDF0DD)

private val Light = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = PaleGreen,
    onPrimaryContainer = Color(0xFF0B3D0B),
    secondary = Color(0xFF4E6E4F),
    secondaryContainer = PaleGreen,
    onSecondaryContainer = Color(0xFF0B3D0B),
    background = Color(0xFFFBFDF7),
    surface = Color(0xFFFBFDF7),
    error = Color(0xFFB3261E),
)

private val Dark = darkColorScheme(
    primary = GreenDark,
    onPrimary = Color(0xFF06380A),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = PaleGreen,
    secondary = Color(0xFFB6CCB6),
    secondaryContainer = Color(0xFF33482F),
    onSecondaryContainer = PaleGreen,
    background = Color(0xFF10140F),
    surface = Color(0xFF10140F),
    error = Color(0xFFF2B8B5),
)

@Composable
fun NutriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        content = content,
    )
}

/** Resolve a [ThemePreference] to light/dark, tracking the OS live for [ThemePreference.SYSTEM]. */
@Composable
fun ThemePreference.isDark(): Boolean = when (this) {
    ThemePreference.SYSTEM -> isSystemInDarkTheme()
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}
