package `in`.acstechnologies.nutritrainerai.ui.onboarding

import `in`.acstechnologies.nutritrainerai.domain.model.AnimalFood
import `in`.acstechnologies.nutritrainerai.domain.model.BodyProfile
import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.LanguageMode
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal
import `in`.acstechnologies.nutritrainerai.domain.region.IndiaRegionCatalog
import `in`.acstechnologies.nutritrainerai.domain.region.RegionEntry

/** The whole render input for the onboarding flow (MVI — see plan §2A). Immutable. */
data class OnboardingUiState(
    val draft: OnboardingState = OnboardingState(),
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val regionQuery: String = "",
) {
    val stepNumber: Int get() = step.number
    val totalSteps: Int get() = OnboardingStep.TOTAL

    /** OB-05 list, filtered live by [regionQuery]; blank query shows everything. */
    val regionResults: List<RegionEntry> get() = IndiaRegionCatalog.search(regionQuery)

    /** Whether "Continue" is enabled on the current step (PRD §4.1.3). */
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.FOOD_PATTERN -> draft.foodPattern != null
            OnboardingStep.GOAL_BODY -> draft.primaryGoal != null && draft.bodyProfile.isPlausible
            else -> true
        }

    /** The two mandatory answers are present and valid (PRD §4.1.2, page 16). */
    val canFinish: Boolean
        get() = draft.foodPattern != null &&
            draft.primaryGoal != null &&
            draft.bodyProfile.isPlausible
}

/** Everything the user can do. One entry point: [OnboardingViewModel.onIntent]. */
sealed interface OnboardingIntent {
    data class SelectFoodPattern(val pattern: FoodPattern) : OnboardingIntent
    data class ToggleAnimalFood(val food: AnimalFood) : OnboardingIntent
    data class SetDairyAllowed(val allowed: Boolean) : OnboardingIntent

    data class ToggleAllergy(val id: String) : OnboardingIntent
    data class SetAvoidanceText(val text: String) : OnboardingIntent

    data class SetRegionQuery(val query: String) : OnboardingIntent
    data class SelectRegion(val code: String) : OnboardingIntent
    data object ClearRegion : OnboardingIntent

    data class ToggleTradition(val id: String) : OnboardingIntent
    data class SetCustomTraditionText(val text: String) : OnboardingIntent

    data class SetGoal(val goal: PrimaryGoal) : OnboardingIntent
    data class UpdateBodyProfile(val body: BodyProfile) : OnboardingIntent

    data class SetLanguageMode(val mode: LanguageMode) : OnboardingIntent
    data class ToggleLoggingLanguage(val code: String) : OnboardingIntent
    data class SetAppLocale(val code: String) : OnboardingIntent

    data class RecordConsent(val kind: String, val epochMillis: Long) : OnboardingIntent

    /** "Continue" — advance to the next step. */
    data object Continue : OnboardingIntent

    /** "Skip for now" — record an explicit skip, then advance. No-op on required steps. */
    data object Skip : OnboardingIntent

    data object Back : OnboardingIntent

    /** Review → edit a section → return without losing later answers. */
    data class GoToStep(val step: OnboardingStep) : OnboardingIntent

    data class Finish(val epochMillis: Long) : OnboardingIntent
}

/** One-off signals — never part of state. */
sealed interface OnboardingEffect {
    data object Completed : OnboardingEffect
}
