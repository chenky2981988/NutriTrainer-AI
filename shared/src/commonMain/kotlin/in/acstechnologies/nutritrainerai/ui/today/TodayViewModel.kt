package `in`.acstechnologies.nutritrainerai.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.usecase.DayTotals
import `in`.acstechnologies.nutritrainerai.domain.usecase.ObserveDayUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Today — the authoritative daily record (PRD §4.3). Read-only view of one day. */
data class TodayUiState(
    val dayEpochDay: Long,
    val totals: DayTotals = DayTotals.EMPTY,
    val items: List<MealItem> = emptyList(),
)

class TodayViewModel(
    observeDay: ObserveDayUseCase,
    dayEpochDay: Long,
) : ViewModel() {

    val state: StateFlow<TodayUiState> =
        observeDay.observe(dayEpochDay)
            .map { TodayUiState(it.dayEpochDay, it.totals, it.items) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TodayUiState(dayEpochDay))
}
