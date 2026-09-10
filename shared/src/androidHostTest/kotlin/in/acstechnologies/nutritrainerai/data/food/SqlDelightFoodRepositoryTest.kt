package `in`.acstechnologies.nutritrainerai.data.food

import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.Food
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightFoodRepositoryTest {

    private fun repo() = SqlDelightFoodRepository(inMemoryNutriDb(), Dispatchers.Unconfined)

    private fun food(name: String, aliases: List<String> = emptyList()) = Food(
        id = "f-$name",
        canonicalName = name,
        aliases = aliases,
        nutrientsPerBase = NutrientVector.ofKcal(62.0, 3.2, 4.8, 3.3, 0.0),
        basis = MeasurementBasis.PER_100_ML,
        source = SourceType.USER_CONFIRMED,
        confidence = ConfidenceBand.VERY_HIGH,
        brand = "Provilac",
        createdAtEpochMillis = 1_000L,
    )

    @Test
    fun upsert_thenFindByName_caseInsensitive() = runTest {
        val r = repo()
        r.upsert(food("Provilac Milk"))
        assertEquals("Provilac Milk", r.findByName("  provilac milk ")?.canonicalName)
        assertEquals("Provilac", r.findByName("provilac milk")?.brand)
    }

    @Test
    fun findByName_matchesAnAlias() = runTest {
        val r = repo()
        r.upsert(food("Provilac Milk", aliases = listOf("provilac", "provilac full cream")))
        assertEquals("Provilac Milk", r.findByName("Provilac Full Cream")?.canonicalName)
    }

    @Test
    fun unknown_isNull() = runTest {
        assertNull(repo().findByName("nothing here"))
    }

    @Test
    fun search_and_observe_and_delete() = runTest {
        val r = repo()
        r.upsert(food("Provilac Milk"))
        r.upsert(food("Amul Paneer"))
        assertEquals(listOf("Provilac Milk"), r.search("provi").map { it.canonicalName })
        assertEquals(2, r.observeAll().first().size)
        r.delete("f-Provilac Milk")
        assertEquals(listOf("Amul Paneer"), r.observeAll().first().map { it.canonicalName })
    }
}
