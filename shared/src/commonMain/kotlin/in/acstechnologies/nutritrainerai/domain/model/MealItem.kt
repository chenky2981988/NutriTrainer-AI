package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * One logged food item — the atom that Coach and Today both render (PRD §4.2–4.3).
 *
 * [nutrients] is a stored result of a specific [calculationVersion], not a live
 * computation: it must not silently change if reference data is updated later
 * (PRD §6 "Version and roll back food database changes").
 *
 * Removal is soft ([deletedAtEpochMillis]) so undo works (PRD FR07); a removed
 * row keeps its [revision] history.
 */
@Serializable
data class MealItem(
    val id: String,
    val dayEpochDay: Long,
    val status: MealItemStatus,
    /** The user has accepted this item; unconfirmed items never count toward totals. */
    val confirmed: Boolean,
    val foodName: String,
    /** Resolved quantity in grams, when known. */
    val quantityGrams: Double?,
    val nutrients: NutrientVector,
    val confidence: ConfidenceBand,
    val source: SourceType,
    val revision: Int,
    val calculationVersion: Int,
    val createdAtEpochMillis: Long,
    val deletedAtEpochMillis: Long? = null,
) {
    val isDeleted: Boolean get() = deletedAtEpochMillis != null

    /** Whether this item contributes to the day's consumed totals (PRD §5). */
    val countsTowardConsumed: Boolean
        get() = !isDeleted && confirmed && status == MealItemStatus.CONSUMED
}
