package `in`.acstechnologies.nutritrainerai.domain.resolve

import `in`.acstechnologies.nutritrainerai.data.food.SqlDelightFoodRepository
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.Food
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompositeFoodResolverTest {

    private val foods = SqlDelightFoodRepository(inMemoryNutriDb(), Dispatchers.Unconfined)
    private val resolver = CompositeFoodResolver(foods, SeedFoodResolver())

    @Test
    fun userFoodWinsOverSeed() = runTest {
        foods.upsert(
            Food(
                id = "f1",
                canonicalName = "rice", // same name as a seed food
                nutrientsPerBase = NutrientVector.ofKcal(111.0, 2.6, 23.0, 0.9, 1.6),
                basis = MeasurementBasis.PER_100_G,
                source = SourceType.USER_CONFIRMED,
                confidence = ConfidenceBand.VERY_HIGH,
                createdAtEpochMillis = 1_000L,
            ),
        )
        val r = resolver.resolve("rice")!!
        assertEquals(SourceType.USER_CONFIRMED, r.source)
        assertEquals(111.0, r.nutrientsPerBase.energyKcal)
    }

    @Test
    fun fallsThroughToSeed() = runTest {
        assertEquals(SourceType.CURATED_REGIONAL, resolver.resolve("dal")?.source)
    }

    @Test
    fun nullWhenNeitherKnows() = runTest {
        assertNull(resolver.resolve("dragonfruit smoothie"))
    }
}
