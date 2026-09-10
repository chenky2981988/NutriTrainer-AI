package `in`.acstechnologies.nutritrainerai.domain.usecase

import `in`.acstechnologies.nutritrainerai.domain.calc.DailyContribution
import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.sum
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Consumed and planned totals for a day, kept strictly separate (PRD §4.3). */
data class DayTotals(
    val consumed: NutrientVector,
    val planned: NutrientVector,
    val consumedItemCount: Int,
    val plannedItemCount: Int,
) {
    companion object {
        val EMPTY = DayTotals(NutrientVector.ZERO, NutrientVector.ZERO, 0, 0)
    }
}

/** One day's items plus their totals — the shape Coach and Today both render. */
data class DayView(
    val dayEpochDay: Long,
    val items: List<MealItem>,
    val totals: DayTotals,
)

/**
 * The single stream Coach and Today share. Because both derive their numbers
 * from this one [Flow], "Coach and Today totals match" is guaranteed by
 * construction (PRD §2A, meal Definition of Done).
 */
class ObserveDayUseCase(
    private val mealLog: MealLogRepository,
) {
    fun observe(dayEpochDay: Long): Flow<DayView> =
        mealLog.observeDay(dayEpochDay).map { items ->
            DayView(dayEpochDay, items, totalsOf(items))
        }

    companion object {
        fun totalsOf(items: List<MealItem>): DayTotals {
            val consumed = NutritionMath.dailyConsumedTotal(
                items.map { DailyContribution(it.nutrients, it.status, it.confirmed) },
            )
            val plannedItems = items.filter { it.status == MealItemStatus.PLANNED && !it.isDeleted }
            return DayTotals(
                consumed = consumed,
                planned = plannedItems.map { it.nutrients }.sum(),
                consumedItemCount = items.count { it.countsTowardConsumed },
                plannedItemCount = plannedItems.size,
            )
        }
    }
}
