package `in`.acstechnologies.nutritrainerai.data.settings

import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightAppSettingsRepositoryTest {

    private fun repo(db: NutriDb) = SqlDelightAppSettingsRepository(db, Dispatchers.Unconfined)

    @Test
    fun defaultsToSystemWhenUnset() = runTest {
        assertEquals(ThemePreference.SYSTEM, repo(inMemoryNutriDb()).themePreference().first())
    }

    @Test
    fun setThenObserveEmitsTheChoice() = runTest {
        val r = repo(inMemoryNutriDb())
        r.setThemePreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, r.themePreference().first())
        r.setThemePreference(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, r.themePreference().first())
    }

    @Test
    fun choicePersistsAcrossRepositoryInstances() = runTest {
        val db = inMemoryNutriDb()
        repo(db).setThemePreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, repo(db).themePreference().first())
    }

    @Test
    fun unrecognisedStoredValueFallsBackToDefault() = runTest {
        val db = inMemoryNutriDb()
        db.appSettingEntityQueries.upsert("theme", "NEON")
        assertEquals(ThemePreference.SYSTEM, repo(db).themePreference().first())
    }
}
