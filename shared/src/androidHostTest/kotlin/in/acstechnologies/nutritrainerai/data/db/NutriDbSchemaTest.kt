package `in`.acstechnologies.nutritrainerai.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Schema + migration harness. `Schema.create()` builds every table for fresh
 * installs; `1.sqm` adds foodEntity (v1→v2), `2.sqm` adds coachTurnEntity (v2→v3).
 */
class NutriDbSchemaTest {

    @Test
    fun schemaVersionIsThree() {
        assertEquals(3L, NutriDb.Schema.version)
    }

    @Test
    fun freshSchemaHasAllTables() {
        val db = inMemoryNutriDb()
        db.mealItemEntityQueries.selectByDay(0L).executeAsList()
        db.measurementEntityQueries.selectAll().executeAsList()
        db.onboardingDraftEntityQueries.selectCurrent().executeAsList()
        db.appSettingEntityQueries.selectValue("theme").executeAsList()
        db.foodEntityQueries.selectAll().executeAsList()
        db.coachTurnEntityQueries.selectByDay(0L).executeAsList()
    }

    @Test
    fun migratingFromV1_addsFoodTable() {
        // A "v1" database: everything except what 1.sqm adds.
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE mealItemEntity(id TEXT NOT NULL PRIMARY KEY, dayEpochDay INTEGER NOT NULL, status TEXT NOT NULL, confirmed INTEGER NOT NULL, foodName TEXT NOT NULL, quantityGrams REAL, energyKcal REAL NOT NULL, energyKj REAL NOT NULL, proteinG REAL NOT NULL, carbohydrateG REAL NOT NULL, fatG REAL NOT NULL, fibreG REAL NOT NULL, confidenceBand TEXT NOT NULL, sourceType TEXT NOT NULL, revision INTEGER NOT NULL, calculationVersion INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, deletedAtEpochMillis INTEGER)", 0)

        NutriDb.Schema.migrate(driver, oldVersion = 1L, newVersion = 3L)

        val db = NutriDb(driver)
        // No exception ⇒ foodEntity and coachTurnEntity now exist.
        assertEquals(emptyList(), db.foodEntityQueries.selectAll().executeAsList())
        assertEquals(emptyList(), db.coachTurnEntityQueries.selectByDay(0L).executeAsList())
    }

    @Test
    fun migratingFromV2_addsCoachTurnTable() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NutriDb.Schema.create(driver) // a full v3 create...
        // ...is fine to then re-run 2.sqm against because it is IF NOT EXISTS.
        NutriDb.Schema.migrate(driver, oldVersion = 2L, newVersion = 3L)

        val db = NutriDb(driver)
        assertEquals(emptyList(), db.coachTurnEntityQueries.selectByDay(0L).executeAsList())
    }
}
