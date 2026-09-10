package `in`.acstechnologies.nutritrainerai.data.settings

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.ThemePreference
import `in`.acstechnologies.nutritrainerai.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SqlDelightAppSettingsRepository(
    db: NutriDb,
    private val ioContext: CoroutineContext,
) : AppSettingsRepository {

    private val queries = db.appSettingEntityQueries

    override fun themePreference(): Flow<ThemePreference> =
        queries.selectValue(KEY_THEME)
            .asFlow()
            .mapToOneOrNull(ioContext)
            .map { ThemePreference.fromNameOrDefault(it) }

    override suspend fun setThemePreference(preference: ThemePreference) {
        withContext(ioContext) { queries.upsert(KEY_THEME, preference.name) }
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
