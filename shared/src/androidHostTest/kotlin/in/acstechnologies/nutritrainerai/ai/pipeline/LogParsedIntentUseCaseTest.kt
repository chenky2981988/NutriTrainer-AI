package `in`.acstechnologies.nutritrainerai.ai.pipeline

import `in`.acstechnologies.nutritrainerai.ai.IntentKind
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntent
import `in`.acstechnologies.nutritrainerai.ai.ParsedItem
import `in`.acstechnologies.nutritrainerai.ai.ParsedQuantity
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.data.meallog.SqlDelightMealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.resolve.QuantityResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.SeedFoodResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val DAY = 20_000L

class LogParsedIntentUseCaseTest {

    private class Fixture {
        val mealLog: MealLogRepository =
            SqlDelightMealLogRepository(inMemoryNutriDb(), Dispatchers.Unconfined)
        private var seq = 0
        val useCase = LogParsedIntentUseCase(
            mealLog = mealLog,
            foods = SeedFoodResolver(),
            quantities = QuantityResolver(),
            now = { 1_000L },
            idFactory = { "id-${seq++}" },
        )
    }

    private fun item(name: String, amount: Double, unit: String) =
        ParsedItem(
            rawText = "$amount $unit $name",
            foodName = name,
            quantity = ParsedQuantity(amount, unit),
            confidence = ConfidenceBand.LOW,
        )

    private fun intent(kind: IntentKind, vararg items: ParsedItem, transcript: String = "test") =
        NutritionIntent(
            kind = kind,
            items = items.toList(),
            overallConfidence = ConfidenceBand.LOW,
            sourceTranscript = transcript,
        )

    @Test
    fun consumed_resolvesFoodsAndComputesNutritionDeterministically() = runTest {
        val f = Fixture()
        f.useCase.log(
            intent(IntentKind.CONSUMED, item("rice", 150.0, "g"), item("dal", 50.0, "g")),
            DAY,
        )

        val day = f.mealLog.getDay(DAY)
        assertEquals(listOf("rice", "dal"), day.map { it.foodName })
        assertEquals(
            NutritionMath.ingredientNutrients(SeedFoodResolver.RICE.nutrientsPerBase, 150.0),
            day[0].nutrients,
        )
        assertEquals(
            NutritionMath.ingredientNutrients(SeedFoodResolver.DAL.nutrientsPerBase, 50.0),
            day[1].nutrients,
        )
        assertTrue(day.all { it.confirmed && it.countsTowardConsumed })
    }

    @Test
    fun unresolvedFood_isStoredUnconfirmedAndOutOfTotals() = runTest {
        val f = Fixture()
        val outcome = f.useCase.log(intent(IntentKind.CONSUMED, item("unicorn", 100.0, "g")), DAY)

        assertEquals(listOf("unicorn"), outcome.unresolvedFoods)
        val stored = f.mealLog.getDay(DAY).single()
        assertEquals(ConfidenceBand.UNRESOLVED, stored.confidence)
        assertEquals(NutrientVector.ZERO, stored.nutrients)
        assertFalse(stored.confirmed)
        assertFalse(stored.countsTowardConsumed)
    }

    @Test
    fun correct_replacesTheSingleMatchInPlace() = runTest {
        val f = Fixture()
        f.useCase.log(intent(IntentKind.CONSUMED, item("rice", 150.0, "g")), DAY)
        val outcome = f.useCase.log(intent(IntentKind.CORRECT, item("rice", 200.0, "g")), DAY)

        val day = f.mealLog.getDay(DAY)
        assertEquals(1, day.size) // replaced, not duplicated
        assertEquals(200.0, day.single().quantityGrams)
        assertEquals(2, day.single().revision)
        assertEquals(
            NutritionMath.ingredientNutrients(SeedFoodResolver.RICE.nutrientsPerBase, 200.0),
            day.single().nutrients,
        )
        assertEquals(1, outcome.replaced.size)
    }

    @Test
    fun remove_softDeletesTheMatch() = runTest {
        val f = Fixture()
        f.useCase.log(intent(IntentKind.CONSUMED, item("rice", 150.0, "g")), DAY)
        val outcome = f.useCase.log(intent(IntentKind.REMOVE, item("rice", 0.0, "g")), DAY)

        assertEquals(1, outcome.removedIds.size)
        assertEquals(emptyList(), f.mealLog.getDay(DAY))
    }

    @Test
    fun plan_storesPlannedItemsThatNeverCountAsConsumed() = runTest {
        val f = Fixture()
        f.useCase.log(intent(IntentKind.PLAN, item("rice", 100.0, "g")), DAY)

        val stored = f.mealLog.getDay(DAY).single()
        assertEquals(MealItemStatus.PLANNED, stored.status)
        assertTrue(stored.confirmed)
        assertFalse(stored.countsTowardConsumed)
    }
}
