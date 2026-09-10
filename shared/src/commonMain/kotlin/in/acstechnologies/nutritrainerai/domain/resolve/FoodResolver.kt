package `in`.acstechnologies.nutritrainerai.domain.resolve

import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType

/**
 * A resolved food: a canonical identity plus reference nutrition, ready for the
 * deterministic calculator. The AI never produces this — only software does
 * (PRD "Primary decision").
 */
data class ResolvedFood(
    val canonicalName: String,
    val nutrientsPerBase: NutrientVector,
    val basis: MeasurementBasis, // PER_100_G or PER_100_ML
    val source: SourceType,
    val confidence: ConfidenceBand,
    /** Grams for one "piece" / "slice" of this food, when it is naturally counted. */
    val pieceGrams: Double? = null,
    /** Food-specific household-unit weights, e.g. "katori" -> 150.0. */
    val householdUnitGrams: Map<String, Double> = emptyMap(),
)

/**
 * Maps a spoken food name to a [ResolvedFood], following the source precedence of
 * PRD §6. Returns `null` when the food cannot be identified — the caller then
 * records an unresolved item that stays out of totals until the user picks.
 */
interface FoodResolver {
    fun resolve(foodName: String): ResolvedFood?
}
