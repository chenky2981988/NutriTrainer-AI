package `in`.acstechnologies.nutritrainerai.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.usecase.DayTotals
import `in`.acstechnologies.nutritrainerai.domain.usecase.ObserveDayUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Today — the authoritative daily record (PRD §4.3). */
data class TodayUiState(
    val dayEpochDay: Long,
    val totals: DayTotals = DayTotals.EMPTY,
    val items: List<MealItem> = emptyList(),
)

sealed interface TodayIntent {
    /** Soft-delete a logged item; keeps it recoverable (PRD FR07). */
    data class Delete(val id: String) : TodayIntent

    /** Undo the last [Delete] for that id. */
    data class UndoDelete(val id: String) : TodayIntent
}

@OptIn(ExperimentalTime::class)
class TodayViewModel(
    observeDay: ObserveDayUseCase,
    private val mealLog: MealLogRepository,
    dayEpochDay: Long,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {

    val state: StateFlow<TodayUiState> =
        observeDay.observe(dayEpochDay)
            .map { TodayUiState(it.dayEpochDay, it.totals, it.items) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TodayUiState(dayEpochDay))

    fun onIntent(intent: TodayIntent) {
        when (intent) {
            is TodayIntent.Delete -> viewModelScope.launch { mealLog.softDelete(intent.id, now()) }
            is TodayIntent.UndoDelete -> viewModelScope.launch { mealLog.restore(intent.id) }
        }
    }
}
