package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** Per-device UI preferences. Reactive so the app re-themes the instant it changes. */
interface AppSettingsRepository {
    fun themePreference(): Flow<ThemePreference>
    suspend fun setThemePreference(preference: ThemePreference)
}
