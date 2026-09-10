package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * A resolvable food (PRD §9 "Food" entity). When the app can't identify
 * something the user logged, they add it here — stored as
 * [SourceType.USER_CONFIRMED] / [ConfidenceBand.VERY_HIGH], which outranks every
 * other source for that user (PRD §6 rank 1).
 *
 * [nutrientsPerBase] is measured against [basis]; [servingGrams] is only set
 * when [basis] is [MeasurementBasis.PER_SERVING].
 */
@Serializable
data class Food(
    val id: String,
    val canonicalName: String,
    val aliases: List<String> = emptyList(),
    val nutrientsPerBase: NutrientVector,
    val basis: MeasurementBasis,
    val source: SourceType,
    val confidence: ConfidenceBand,
    val brand: String? = null,
    val pack: String? = null,
    val servingGrams: Double? = null,
    val createdAtEpochMillis: Long,
) {
    /** Normalised lookup key. */
    val nameKey: String get() = canonicalName.trim().lowercase()
}
