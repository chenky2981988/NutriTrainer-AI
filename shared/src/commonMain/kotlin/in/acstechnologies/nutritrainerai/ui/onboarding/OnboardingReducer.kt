package `in`.acstechnologies.nutritrainerai.ui.onboarding

import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep

/** Result of applying one intent: the next state and an optional one-off effect. */
data class OnboardingReduction(
    val state: OnboardingUiState,
    val effect: OnboardingEffect? = null,
)

/**
 * The pure heart of onboarding. No coroutines, no I/O — `(state, intent) -> next`.
 * The [OnboardingViewModel] owns side effects (persisting the draft, emitting
 * effects). Encodes the branching + back-navigation rules of PRD page 11.
 */
object OnboardingReducer {

    fun reduce(current: OnboardingUiState, intent: OnboardingIntent): OnboardingReduction {
        val next = when (intent) {
            is OnboardingIntent.SelectFoodPattern -> current.copy(
                draft = current.draft.copy(
                    foodPattern = intent.pattern,
                    // Changing category revalidates saved animal-food details (PRD p.11).
                    allowedAnimalFoods = if (intent.pattern.allowsAnimalFoodSelection) {
                        current.draft.allowedAnimalFoods
                    } else {
                        emptySet()
                    },
                    dairyAllowed = intent.pattern.allowsDairy,
                ),
            )

            is OnboardingIntent.ToggleAnimalFood -> {
                if (current.draft.foodPattern?.allowsAnimalFoodSelection != true) {
                    current
                } else {
                    current.copy(draft = current.draft.copy(allowedAnimalFoods = current.draft.allowedAnimalFoods.toggle(intent.food)))
                }
            }

            is OnboardingIntent.SetDairyAllowed ->
                current.copy(draft = current.draft.copy(dairyAllowed = intent.allowed))

            is OnboardingIntent.ToggleAllergy ->
                current.copy(draft = current.draft.copy(allergyIds = current.draft.allergyIds.toggle(intent.id)))

            is OnboardingIntent.SetAvoidanceText ->
                current.copy(draft = current.draft.copy(avoidanceText = intent.text))

            is OnboardingIntent.SetRegionQuery -> current.copy(regionQuery = intent.query)

            is OnboardingIntent.SelectRegion -> current.copy(
                draft = current.draft.copy(countryCode = "IN", stateUtCode = intent.code),
            )

            OnboardingIntent.ClearRegion -> current.copy(
                draft = current.draft.copy(stateUtCode = null, subRegionIds = emptySet()),
            )

            is OnboardingIntent.ToggleTradition ->
                current.copy(draft = current.draft.copy(traditionIds = current.draft.traditionIds.toggle(intent.id)))

            is OnboardingIntent.SetCustomTraditionText ->
                current.copy(draft = current.draft.copy(customTraditionText = intent.text))

            is OnboardingIntent.UpdateHouseholdProfile ->
                current.copy(draft = current.draft.copy(householdProfile = intent.profile))

            is OnboardingIntent.SetGoal ->
                current.copy(draft = current.draft.copy(primaryGoal = intent.goal))

            is OnboardingIntent.UpdateBodyProfile ->
                current.copy(draft = current.draft.copy(bodyProfile = intent.body))

            is OnboardingIntent.SetLanguageMode ->
                current.copy(draft = current.draft.copy(languageMode = intent.mode))

            is OnboardingIntent.ToggleLoggingLanguage -> current.copy(
                draft = current.draft.copy(
                    loggingLanguageCodes = current.draft.loggingLanguageCodes.toggleList(intent.code),
                ),
            )

            is OnboardingIntent.SetAppLocale ->
                current.copy(draft = current.draft.copy(appLocale = intent.code))

            is OnboardingIntent.RecordConsent -> current.copy(
                draft = current.draft.copy(
                    consentTimestamps = current.draft.consentTimestamps + (intent.kind to intent.epochMillis),
                ),
            )

            OnboardingIntent.Continue -> {
                if (!current.canContinue) return OnboardingReduction(current)
                current.advance(markCompleted = true)
            }

            OnboardingIntent.Skip -> {
                if (current.step.required) return OnboardingReduction(current)
                current.advance(markSkipped = true)
            }

            OnboardingIntent.Back -> {
                val prev = current.step.previous ?: return OnboardingReduction(current)
                current.copy(step = prev, draft = current.draft.copy(currentStepId = prev.id))
            }

            is OnboardingIntent.GoToStep ->
                current.copy(step = intent.step, draft = current.draft.copy(currentStepId = intent.step.id))

            is OnboardingIntent.Finish -> {
                if (!current.canFinish) return OnboardingReduction(current)
                return OnboardingReduction(
                    state = current.copy(draft = current.draft.copy(completedAtEpochMillis = intent.epochMillis)),
                    effect = OnboardingEffect.Completed,
                )
            }
        }
        return OnboardingReduction(next)
    }

    /** First incomplete required step, else the last step the user was on (PRD p.11). */
    fun resumeStep(draft: OnboardingState): OnboardingStep =
        OnboardingStep.entries.firstOrNull { it.required && it.id !in draft.completedStepIds }
            ?: OnboardingStep.fromId(draft.currentStepId)

    private fun OnboardingUiState.advance(
        markCompleted: Boolean = false,
        markSkipped: Boolean = false,
    ): OnboardingUiState {
        val target = step.next ?: step
        val completed = if (markCompleted) draft.completedStepIds + step.id else draft.completedStepIds - step.id
        val skipped = if (markSkipped) draft.skippedStepIds + step.id else draft.skippedStepIds - step.id
        return copy(
            step = target,
            regionQuery = if (target == OnboardingStep.STATE_UT) regionQuery else "",
            draft = draft.copy(
                currentStepId = target.id,
                completedStepIds = completed,
                skippedStepIds = skipped,
            ),
        )
    }

    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

    private fun List<String>.toggleList(value: String): List<String> =
        if (value in this) this - value else this + value
}
