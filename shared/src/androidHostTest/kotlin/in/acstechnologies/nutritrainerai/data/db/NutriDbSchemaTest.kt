package `in`.acstechnologies.nutritrainerai.data.db

import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Minimal migration harness. When the first `.sqm` lands, add tests that migrate
 * an old-version database forward and assert the resulting rows; the Gradle
 * `verifyNutriDbMigration` task guards the SQL itself.
 */
class NutriDbSchemaTest {

    @Test
    fun schemaVersionIsOne() {
        assertEquals(1L, NutriDb.Schema.version)
    }

    @Test
    fun freshSchemaHasAllTables() {
        val db = inMemoryNutriDb()
        // No exception ⇒ the tables exist and the generated SQL is valid.
        db.mealItemEntityQueries.selectByDay(0L).executeAsList()
        db.measurementEntityQueries.selectAll().executeAsList()
    }
}
