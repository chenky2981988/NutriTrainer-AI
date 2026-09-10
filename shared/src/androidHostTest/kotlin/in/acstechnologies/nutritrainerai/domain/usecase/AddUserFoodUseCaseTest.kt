package `in`.acstechnologies.nutritrainerai.domain.usecase

import `in`.acstechnologies.nutritrainerai.ai.IntentKind
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntent
import `in`.acstechnologies.nutritrainerai.ai.ParsedItem
import `in`.acstechnologies.nutritrainerai.ai.ParsedQuantity
import `in`.acstechnologies.nutritrainerai.ai.pipeline.LogParsedIntentUseCase
import `in`.acstechnologies.nutritrainerai.data.food.SqlDelightFoodRepository
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.data.meallog.SqlDelightMealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import `in`.acstechnologies.nutritrainerai.domain.resolve.CompositeFoodResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.QuantityResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.SeedFoodResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddUserFoodUseCaseTest {

    private class Fixture {
        val db = inMemoryNutriDb()
        val mealLog = SqlDelightMealLogRepository(db, Dispatchers.Unconfined)
        val foods = SqlDelightFoodRepository(db, Dispatchers.Unconfined)
        val quantities = QuantityResolver()
        private var seq = 0
        val log = LogParsedIntentUseCase(
            mealLog, CompositeFoodResolver(foods, SeedFoodResolver()), quantities,
            now = { 1_000L }, idFactory = { "id-${seq++}" },
        )
        val addFood = AddUserFoodUseCase(
            foods, mealLog, quantities, now = { 2_000L }, idFactory = { "uf-${seq++}" },
        )
    }

    private fun consumed(name: String, amount: Double, unit: String) = NutritionIntent(
        kind = IntentKind.CONSUMED,
        items = listOf(
            ParsedItem(
                rawText = "$amount $unit $name",
                foodName = name,
                quantity = ParsedQuantity(amount, unit),
                confidence = ConfidenceBand.LOW,
            ),
        ),
        overallConfidence = ConfidenceBand.LOW,
        sourceTranscript = "test",
    )

    @Test
    fun teachesFood_thenReResolvesTheEntry_soItCounts() = runTest {
        val f = Fixture()
        val day = 20_000L

        val outcome = f.log.log(consumed("provilac milk", 250.0, "ml"), day)
        val pending = outcome.created.single()
        assertEquals(ConfidenceBand.UNRESOLVED, pending.confidence)
        assertTrue(!pending.countsTowardConsumed)

        val perBase = NutrientVector.ofKcal(62.0, proteinG = 3.2, carbohydrateG = 4.8, fatG = 3.3, fibreG = 0.0)
        val updated = f.addFood.addAndResolve(
            dayEpochDay = day,
            mealItemId = pending.id,
            details = NewFoodDetails(
                name = "Provilac Milk",
                brand = "Provilac",
                basis = MeasurementBasis.PER_100_ML,
                nutrientsPerBase = perBase,
                entryAmount = 250.0,
                entryUnit = "ml",
            ),
        )!!

        assertEquals(SourceType.USER_CONFIRMED, updated.source)
        assertEquals(ConfidenceBand.VERY_HIGH, updated.confidence)
        assertTrue(updated.confirmed && updated.countsTowardConsumed)
        assertEquals(2, updated.revision)
        assertEquals(NutritionMath.ingredientNutrients(perBase, 250.0), updated.nutrients)

        // and it's remembered for next time
        assertEquals("Provilac Milk", f.foods.findByName("provilac milk")?.canonicalName)
        val second = f.log.log(consumed("Provilac Milk", 100.0, "ml"), day)
        assertEquals(ConfidenceBand.VERY_HIGH, second.created.single().confidence)
    }
}
