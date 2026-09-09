package `in`.acstechnologies.nutritrainerai.domain.calc

import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector

/** One item's contribution to a day's totals. */
data class DailyContribution(
    val nutrients: NutrientVector,
    val status: MealItemStatus,
    /** Confirmed by the user (not a pending / unresolved estimate). */
    val confirmed: Boolean,
)

/** A single weight reading, keyed by epoch day so the domain needs no calendar API. */
data class WeightPoint(
    val epochDay: Long,
    val kg: Double,
)

/**
 * The deterministic nutrition/fitness arithmetic (PRD §5 "Calculation rules").
 *
 * This is the software that "owns" calories, BMI and trends — the AI never does
 * (PRD "Primary decision"). Every function here is pure and golden-tested.
 */
object NutritionMath {

    /**
     * Ingredient nutrients: `reference per 100 g × edible grams ÷ 100` (PRD §5).
     */
    fun ingredientNutrients(per100g: NutrientVector, edibleGrams: Double): NutrientVector {
        require(edibleGrams.isFinite() && edibleGrams >= 0.0) {
            "edibleGrams must be finite and >= 0, was $edibleGrams"
        }
        return per100g * (edibleGrams / 100.0)
    }

    /**
     * Recipe serving nutrients: `batch nutrients × serving grams ÷ final cooked
     * yield` (PRD §5). [batchNutrients] already includes explicit cooking fats
     * and is measured against the whole cooked batch weight [cookedYieldGrams].
     */
    fun recipeServingNutrients(
        batchNutrients: NutrientVector,
        servingGrams: Double,
        cookedYieldGrams: Double,
    ): NutrientVector {
        require(servingGrams.isFinite() && servingGrams >= 0.0) {
            "servingGrams must be finite and >= 0, was $servingGrams"
        }
        require(cookedYieldGrams.isFinite() && cookedYieldGrams > 0.0) {
            "cookedYieldGrams must be finite and > 0, was $cookedYieldGrams"
        }
        return batchNutrients * (servingGrams / cookedYieldGrams)
    }

    /**
     * Body mass index: `weight kg ÷ (height m)²` (PRD §5). Informational only —
     * separated from personalized medical targets (PRD §10).
     */
    fun bmi(weightKg: Double, heightMetres: Double): Double {
        require(weightKg.isFinite() && weightKg > 0.0) { "weightKg must be finite and > 0, was $weightKg" }
        require(heightMetres.isFinite() && heightMetres > 0.0) {
            "heightMetres must be finite and > 0, was $heightMetres"
        }
        return weightKg / (heightMetres * heightMetres)
    }

    /**
     * Daily total: sum of **confirmed CONSUMED** contributions only. Planned and
     * unconfirmed items never count (PRD §4.3, §5).
     */
    fun dailyConsumedTotal(contributions: Iterable<DailyContribution>): NutrientVector =
        contributions
            .asSequence()
            .filter { it.status == MealItemStatus.CONSUMED && it.confirmed }
            .map { it.nutrients }
            .fold(NutrientVector.ZERO, NutrientVector::plus)

    /**
     * Rolling weight average over the [windowDays] ending at [asOfEpochDay]
     * (inclusive), when enough data exists (PRD §5 "Weight trend"; §4.4 default
     * seven-day average). Returns `null` when no readings fall in the window.
     */
    fun rollingWeightAverageKg(
        points: Iterable<WeightPoint>,
        windowDays: Int,
        asOfEpochDay: Long,
    ): Double? {
        require(windowDays > 0) { "windowDays must be > 0, was $windowDays" }
        val firstDay = asOfEpochDay - (windowDays - 1)
        val inWindow = points.filter { it.epochDay in firstDay..asOfEpochDay }
        if (inWindow.isEmpty()) return null
        return inWindow.sumOf { it.kg } / inWindow.size
    }
}
