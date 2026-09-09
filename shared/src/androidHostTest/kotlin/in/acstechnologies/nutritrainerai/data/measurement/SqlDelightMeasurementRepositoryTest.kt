package `in`.acstechnologies.nutritrainerai.data.measurement

import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.Measurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightMeasurementRepositoryTest {

    private fun repo(db: NutriDb = inMemoryNutriDb()) =
        SqlDelightMeasurementRepository(db, Dispatchers.Unconfined)

    private fun weight(id: String, day: Long, kg: Double?) =
        Measurement(id = id, takenEpochDay = day, weightKg = kg, createdAtEpochMillis = day)

    @Test
    fun weightSeries_dropsNullsAndRowsBeforeSince() = runTest {
        val r = repo()
        r.upsert(weight("a", day = 5, kg = 82.0))
        r.upsert(weight("b", day = 10, kg = 80.0))
        r.upsert(weight("c", day = 11, kg = null)) // waist-only reading, excluded
        r.upsert(weight("d", day = 12, kg = 79.8))
        val series = r.weightSeries(sinceEpochDay = 6)
        assertEquals(listOf(10L to 80.0, 12L to 79.8), series.map { it.epochDay to it.kg })
    }

    @Test
    fun delete_removesRow() = runTest {
        val r = repo()
        r.upsert(weight("a", day = 5, kg = 82.0))
        r.delete("a")
        assertEquals(emptyList(), r.weightSeries(0))
    }

    @Test
    fun upsert_sameId_replaces() = runTest {
        val r = repo()
        r.upsert(weight("a", day = 5, kg = 82.0))
        r.upsert(weight("a", day = 5, kg = 81.4))
        assertEquals(listOf(81.4), r.weightSeries(0).map { it.kg })
    }
}
