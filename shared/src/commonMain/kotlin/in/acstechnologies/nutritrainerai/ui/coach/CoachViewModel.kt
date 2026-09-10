package `in`.acstechnologies.nutritrainerai.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.acstechnologies.nutritrainerai.ai.CompactDailySummary
import `in`.acstechnologies.nutritrainerai.ai.InterpretRequest
import `in`.acstechnologies.nutritrainerai.ai.InterpretResult
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntent
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntentValidator
import `in`.acstechnologies.nutritrainerai.ai.NutritionLanguageEngine
import `in`.acstechnologies.nutritrainerai.ai.pipeline.LogOutcome
import `in`.acstechnologies.nutritrainerai.ai.pipeline.LogParsedIntentUseCase
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.FoodSearchResult
import `in`.acstechnologies.nutritrainerai.domain.repository.OnlineFoodSource
import `in`.acstechnologies.nutritrainerai.domain.usecase.AddUserFoodUseCase
import `in`.acstechnologies.nutritrainerai.domain.usecase.DayTotals
import `in`.acstechnologies.nutritrainerai.domain.usecase.NewFoodDetails
import `in`.acstechnologies.nutritrainerai.domain.usecase.ObserveDayUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One exchange in the Coach transcript. Kept to PRD §4.2's four short beats. */
data class CoachTurn(
    val userText: String,
    val understanding: String,
    val result: String,
    val observation: String? = null,
    val nextAction: String? = null,
    val needsConfirmation: Boolean = false,
)

/** A logged item the app couldn't identify — the user is asked to teach it. */
data class PendingFood(
    val mealItemId: String,
    val dayEpochDay: Long,
    val guessedName: String,
    val guessedAmount: Double,
    val guessedUnit: String,
    val onlineSearching: Boolean = false,
    val onlineResults: List<FoodSearchResult> = emptyList(),
)

data class CoachUiState(
    val composerText: String = "",
    val turns: List<CoachTurn> = emptyList(),
    /** Same numbers Today shows — both derive from [ObserveDayUseCase]. */
    val dayTotals: DayTotals = DayTotals.EMPTY,
    val submitting: Boolean = false,
    /** Non-null ⇒ show the "add this food" form. */
    val pendingFood: PendingFood? = null,
)

sealed interface CoachIntent {
    data class ComposerChanged(val text: String) : CoachIntent
    data object Submit : CoachIntent
    data class SubmitNewFood(val details: NewFoodDetails) : CoachIntent
    data object DismissAddFood : CoachIntent
}

/**
 * Coach — conversational entry (PRD §4.2). Interprets an utterance with the
 * [engine], validates, and lands it through [LogParsedIntentUseCase]. Its
 * running totals come from the *same* [ObserveDayUseCase] stream as Today, so
 * the two never disagree (PRD §2A).
 *
 * When a food can't be resolved it surfaces a [PendingFood]; the user teaches
 * the app via [CoachIntent.SubmitNewFood], which persists a user-confirmed food
 * and re-resolves the entry (PRD §6 rank 1, §7 unknown-product workflow).
 */
class CoachViewModel(
    private val engine: NutritionLanguageEngine,
    private val logUseCase: LogParsedIntentUseCase,
    private val addUserFood: AddUserFoodUseCase,
    private val onlineFoods: OnlineFoodSource,
    observeDay: ObserveDayUseCase,
    private val dayEpochDay: Long,
) : ViewModel() {

    private val composer = MutableStateFlow("")
    private val turns = MutableStateFlow<List<CoachTurn>>(emptyList())
    private val submitting = MutableStateFlow(false)
    private val pendingFood = MutableStateFlow<PendingFood?>(null)

    private val dayTotals: StateFlow<DayTotals> =
        observeDay.observe(dayEpochDay)
            .map { it.totals }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DayTotals.EMPTY)

    val state: StateFlow<CoachUiState> =
        combine(composer, turns, dayTotals, submitting, pendingFood) { text, log, totals, busy, pending ->
            CoachUiState(
                composerText = text,
                turns = log,
                dayTotals = totals,
                submitting = busy,
                pendingFood = pending,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CoachUiState())

    fun onIntent(intent: CoachIntent) {
        when (intent) {
            is CoachIntent.ComposerChanged -> composer.value = intent.text
            CoachIntent.Submit -> submit()
            CoachIntent.DismissAddFood -> pendingFood.value = null
            is CoachIntent.SubmitNewFood -> submitNewFood(intent.details)
        }
    }

    private fun submit() {
        val text = composer.value.trim()
        if (text.isEmpty() || submitting.value) return
        composer.value = ""
        submitting.value = true
        viewModelScope.launch {
            try {
                turns.update { it + runTurn(text) }
            } finally {
                submitting.value = false
            }
        }
    }

    private fun submitNewFood(details: NewFoodDetails) {
        val pending = pendingFood.value ?: return
        pendingFood.value = null
        submitting.value = true
        viewModelScope.launch {
            try {
                val updated = addUserFood.addAndResolve(pending.dayEpochDay, pending.mealItemId, details)
                turns.update {
                    it + if (updated != null) {
                        CoachTurn(
                            userText = "Added ${details.name}",
                            understanding = "Saved ${details.name} to your foods",
                            result = "+${updated.nutrients.energyKcal.roundToInt()} kcal · now counted",
                            observation = "I'll recognise it next time.",
                        )
                    } else {
                        CoachTurn(
                            userText = "Added ${details.name}",
                            understanding = "Saved ${details.name}",
                            result = "—",
                            observation = "Couldn't re-apply it to that entry; edit it in Today.",
                            needsConfirmation = true,
                        )
                    }
                }
            } finally {
                submitting.value = false
            }
        }
    }

    private suspend fun runTurn(text: String): CoachTurn {
        val totals = dayTotals.value
        val request = InterpretRequest(
            utterance = text,
            dailySummary = CompactDailySummary(
                consumedKcal = totals.consumed.energyKcal,
                consumedProteinG = totals.consumed.proteinG,
                mealsLogged = totals.consumedItemCount,
            ),
        )
        return when (val result = engine.interpret(request)) {
            is InterpretResult.Unavailable ->
                CoachTurn(text, "I couldn't process that", result.reason, needsConfirmation = true)

            is InterpretResult.Invalid ->
                CoachTurn(text, "I need a bit more", result.violations.joinToString("; "), needsConfirmation = true)

            is InterpretResult.Success -> {
                val violations = NutritionIntentValidator.validate(result.intent)
                if (violations.isNotEmpty()) {
                    CoachTurn(text, "I need a bit more", violations.joinToString("; "), needsConfirmation = true)
                } else {
                    val outcome = logUseCase.log(result.intent, dayEpochDay)
                    queuePendingFood(outcome, result.intent)
                    outcome.toTurn(text, result.intent)
                }
            }
        }
    }

    /** First unresolved item of the log becomes the [PendingFood] prompt + an online search. */
    private fun queuePendingFood(outcome: LogOutcome, intent: NutritionIntent) {
        if (pendingFood.value != null) return
        val unresolved = outcome.created.firstOrNull { it.confidence == ConfidenceBand.UNRESOLVED } ?: return
        val parsed = intent.items.firstOrNull { it.foodName.equals(unresolved.foodName, ignoreCase = true) }
        pendingFood.value = PendingFood(
            mealItemId = unresolved.id,
            dayEpochDay = dayEpochDay,
            guessedName = unresolved.foodName,
            guessedAmount = parsed?.quantity?.amount ?: unresolved.quantityGrams ?: 1.0,
            guessedUnit = parsed?.quantity?.unit ?: "g",
            onlineSearching = true,
        )
        viewModelScope.launch {
            val results = onlineFoods.searchByName(unresolved.foodName)
            pendingFood.update { it?.copy(onlineSearching = false, onlineResults = results) }
        }
    }

    private fun LogOutcome.toTurn(text: String, intent: NutritionIntent): CoachTurn {
        val addedKcal = created.sumOf { it.nutrients.energyKcal }.roundToInt()
        val understanding = when {
            replaced.isNotEmpty() -> "Updated ${replaced.joinToString { it.foodName }}"
            removedIds.isNotEmpty() -> "Removed ${removedIds.size} item(s)"
            created.isNotEmpty() -> "Logged ${created.joinToString { it.foodName }}"
            else -> "No change (${intent.kind})"
        }
        val result = when {
            created.isNotEmpty() -> "+$addedKcal kcal"
            replaced.isNotEmpty() -> "recalculated"
            else -> "—"
        }
        val observation = when {
            unresolvedFoods.isNotEmpty() -> "I couldn't find ${unresolvedFoods.joinToString()} — add its nutrition below."
            unmatched.isNotEmpty() -> note
            else -> null
        }
        return CoachTurn(
            userText = text,
            understanding = understanding,
            result = result,
            observation = observation,
            needsConfirmation = unresolvedFoods.isNotEmpty() || unmatched.isNotEmpty(),
        )
    }
}
