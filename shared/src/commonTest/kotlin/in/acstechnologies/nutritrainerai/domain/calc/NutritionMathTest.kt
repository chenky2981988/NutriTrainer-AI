package `in`.acstechnologies.nutritrainerai.domain.calc

import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NutritionMathTest {

    // --- ingredientNutrients: per 100 g x edible g / 100 ---------------------

    @Test
    fun ingredientNutrients_scalesByEdibleGrams() {
        val per100g = NutrientVector(350.0, 1464.0, 12.0, 70.0, 1.5, 2.5)
        val result = NutritionMath.ingredientNutrients(per100g, edibleGrams = 250.0)
        assertEquals(NutrientVector(875.0, 3660.0, 30.0, 175.0, 3.75, 6.25), result)
    }

    @Test
    fun ingredientNutrients_zeroGrams_isZero() {
        val per100g = NutrientVector(350.0, 1464.0, 12.0, 70.0, 1.5, 2.5)
        assertEquals(NutrientVector.ZERO, NutritionMath.ingredientNutrients(per100g, 0.0))
    }

    @Test
    fun ingredientNutrients_negativeGrams_rejected() {
        assertFailsWith<IllegalArgumentException> {
            NutritionMath.ingredientNutrients(NutrientVector.ZERO, -1.0)
        }
    }

    // --- recipeServingNutrients: batch x serving g / cooked yield ------------

    @Test
    fun recipeServingNutrients_scalesByServingOverYield() {
        val batch = NutrientVector(1200.0, 5020.8, 40.0, 180.0, 30.0, 12.0)
        val result = NutritionMath.recipeServingNutrients(batch, servingGrams = 150.0, cookedYieldGrams = 600.0)
        assertEquals(NutrientVector(300.0, 1255.2, 10.0, 45.0, 7.5, 3.0), result)
    }

    @Test
    fun recipeServingNutrients_zeroYield_rejected() {
        assertFailsWith<IllegalArgumentException> {
            NutritionMath.recipeServingNutrients(NutrientVector.ZERO, servingGrams = 100.0, cookedYieldGrams = 0.0)
        }
    }

    // --- bmi: weight kg / (height m)^2 --------------------------------------

    @Test
    fun bmi_exactCase() {
        assertEquals(23.4375, NutritionMath.bmi(weightKg = 60.0, heightMetres = 1.6), 1e-9)
    }

    @Test
    fun bmi_typicalCase() {
        assertEquals(22.857142857142858, NutritionMath.bmi(weightKg = 70.0, heightMetres = 1.75), 1e-9)
    }

    @Test
    fun bmi_nonPositiveHeight_rejected() {
        assertFailsWith<IllegalArgumentException> { NutritionMath.bmi(70.0, 0.0) }
    }

    // --- dailyConsumedTotal: confirmed CONSUMED only -----------------------

    @Test
    fun dailyConsumedTotal_countsOnlyConfirmedConsumed() {
        val kcal = { c: Double -> NutrientVector(c, c * NutrientVector.KJ_PER_KCAL, 0.0, 0.0, 0.0, 0.0) }
        val contributions = listOf(
            DailyContribution(kcal(300.0), MealItemStatus.CONSUMED, confirmed = true),
            DailyContribution(kcal(500.0), MealItemStatus.CONSUMED, confirmed = false), // unconfirmed
            DailyContribution(kcal(400.0), MealItemStatus.PLANNED, confirmed = true), // planned
            DailyContribution(kcal(150.0), MealItemStatus.CONSUMED, confirmed = true),
        )
        assertEquals(450.0, NutritionMath.dailyConsumedTotal(contributions).energyKcal, 1e-9)
    }

    @Test
    fun dailyConsumedTotal_empty_isZero() {
        assertEquals(NutrientVector.ZERO, NutritionMath.dailyConsumedTotal(emptyList()))
    }

    // --- rollingWeightAverageKg ------------------------------------------------

    private val points = listOf(
        WeightPoint(epochDay = 5, kg = 82.0),
        WeightPoint(epochDay = 10, kg = 80.0),
        WeightPoint(epochDay = 11, kg = 79.5),
        WeightPoint(epochDay = 12, kg = 79.8),
    )

    @Test
    fun rollingWeightAverage_sevenDayWindow_averagesInWindowOnly() {
        // window = days 6..12 -> includes 10, 11, 12 ; excludes day 5
        val avg = NutritionMath.rollingWeightAverageKg(points, windowDays = 7, asOfEpochDay = 12)
        assertEquals((80.0 + 79.5 + 79.8) / 3.0, avg!!, 1e-9)
    }

    @Test
    fun rollingWeightAverage_singleDayWindow_picksThatDay() {
        assertEquals(82.0, NutritionMath.rollingWeightAverageKg(points, windowDays = 1, asOfEpochDay = 5))
    }

    @Test
    fun rollingWeightAverage_noReadingsInWindow_isNull() {
        assertNull(NutritionMath.rollingWeightAverageKg(points, windowDays = 7, asOfEpochDay = 4))
    }
}
