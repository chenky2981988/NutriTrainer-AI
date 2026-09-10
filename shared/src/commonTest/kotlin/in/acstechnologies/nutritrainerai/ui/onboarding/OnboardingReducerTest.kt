package `in`.acstechnologies.nutritrainerai.ui.onboarding

import `in`.acstechnologies.nutritrainerai.domain.model.AnimalFood
import `in`.acstechnologies.nutritrainerai.domain.model.BodyProfile
import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingReducerTest {

    private fun reduce(state: OnboardingUiState, vararg intents: OnboardingIntent): OnboardingReduction {
        var reduction = OnboardingReduction(state)
        for (intent in intents) reduction = OnboardingReducer.reduce(reduction.state, intent)
        return reduction
    }

    private val start = OnboardingUiState()

    private fun atStep(step: OnboardingStep) = OnboardingUiState(step = step)

    // --- food pattern + animal-food revalidation (PRD p.11) --------------------

    @Test
    fun selectFoodPattern_setsItAndEnablesContinue() {
        val s = reduce(atStep(OnboardingStep.FOOD_PATTERN), OnboardingIntent.SelectFoodPattern(FoodPattern.VEGETARIAN)).state
        assertEquals(FoodPattern.VEGETARIAN, s.draft.foodPattern)
        assertTrue(s.canContinue)
    }

    @Test
    fun switchingAwayFromNonVeg_clearsAnimalFoods() {
        val s = reduce(
            atStep(OnboardingStep.FOOD_DETAILS),
            OnboardingIntent.SelectFoodPattern(FoodPattern.VEG_PLUS_NON_VEG),
            OnboardingIntent.ToggleAnimalFood(AnimalFood.CHICKEN),
            OnboardingIntent.ToggleAnimalFood(AnimalFood.FISH_SEAFOOD),
            OnboardingIntent.SelectFoodPattern(FoodPattern.VEGETARIAN),
        ).state
        assertEquals(emptySet(), s.draft.allowedAnimalFoods)
        assertTrue(s.draft.dairyAllowed)
    }

    @Test
    fun vegan_disallowsDairy() {
        val s = reduce(start, OnboardingIntent.SelectFoodPattern(FoodPattern.VEGAN)).state
        assertFalse(s.draft.dairyAllowed)
    }

    @Test
    fun toggleAnimalFood_isNoOpWhenPatternDisallowsIt() {
        val s = reduce(
            start,
            OnboardingIntent.SelectFoodPattern(FoodPattern.VEGETARIAN),
            OnboardingIntent.ToggleAnimalFood(AnimalFood.CHICKEN),
        ).state
        assertEquals(emptySet(), s.draft.allowedAnimalFoods)
    }

    // --- navigation ---------------------------------------------------------------

    @Test
    fun continue_advancesAndMarksStepCompleted() {
        val s = reduce(start, OnboardingIntent.Continue).state // WELCOME -> FOOD_PATTERN
        assertEquals(OnboardingStep.FOOD_PATTERN, s.step)
        assertTrue(OnboardingStep.WELCOME.id in s.draft.completedStepIds)
        assertEquals(OnboardingStep.FOOD_PATTERN.id, s.draft.currentStepId)
    }

    @Test
    fun continue_isBlockedWhenStepInvalid() {
        val r = reduce(atStep(OnboardingStep.FOOD_PATTERN), OnboardingIntent.Continue)
        assertEquals(OnboardingStep.FOOD_PATTERN, r.state.step) // did not move
        assertNull(r.effect)
    }

    @Test
    fun skip_recordsExplicitSkipAndAdvances_forOptionalSteps() {
        val s = reduce(atStep(OnboardingStep.FOOD_DETAILS), OnboardingIntent.Skip).state
        assertEquals(OnboardingStep.SAFETY_EXCLUSIONS, s.step)
        assertTrue(OnboardingStep.FOOD_DETAILS.id in s.draft.skippedStepIds)
        assertFalse(OnboardingStep.FOOD_DETAILS.id in s.draft.completedStepIds)
    }

    @Test
    fun skip_isNoOpForRequiredSteps() {
        val s = reduce(atStep(OnboardingStep.FOOD_PATTERN), OnboardingIntent.Skip).state
        assertEquals(OnboardingStep.FOOD_PATTERN, s.step)
        assertTrue(s.draft.skippedStepIds.isEmpty())
    }

    @Test
    fun back_returnsToPreviousStepAndKeepsAnswers() {
        val s = reduce(
            atStep(OnboardingStep.FOOD_PATTERN),
            OnboardingIntent.SelectFoodPattern(FoodPattern.VEG_PLUS_EGGS),
            OnboardingIntent.Continue, // -> FOOD_DETAILS
            OnboardingIntent.Back, // -> FOOD_PATTERN
        ).state
        assertEquals(OnboardingStep.FOOD_PATTERN, s.step)
        assertEquals(FoodPattern.VEG_PLUS_EGGS, s.draft.foodPattern)
    }

    @Test
    fun goToStep_jumpsForReviewEditing() {
        val s = reduce(atStep(OnboardingStep.REVIEW), OnboardingIntent.GoToStep(OnboardingStep.STATE_UT)).state
        assertEquals(OnboardingStep.STATE_UT, s.step)
    }

    @Test
    fun advancingIntoStateStep_keepsQueryButLeavingClearsIt() {
        val onStateStep = reduce(
            atStep(OnboardingStep.STATE_UT),
            OnboardingIntent.SetRegionQuery("tamil"),
        ).state
        assertEquals("tamil", onStateStep.regionQuery)
        val afterContinue = OnboardingReducer.reduce(onStateStep, OnboardingIntent.Continue).state
        assertEquals("", afterContinue.regionQuery)
    }

    // --- region -----------------------------------------------------------------

    @Test
    fun selectRegion_setsCountryAndCode() {
        val s = reduce(start, OnboardingIntent.SelectRegion("IN-KA")).state
        assertEquals("IN", s.draft.countryCode)
        assertEquals("IN-KA", s.draft.stateUtCode)
    }

    // --- finishing -------------------------------------------------------------

    @Test
    fun finish_requiresPatternAndGoal() {
        val r = reduce(atStep(OnboardingStep.REVIEW), OnboardingIntent.Finish(epochMillis = 123L))
        assertNull(r.effect)
        assertNull(r.state.draft.completedAtEpochMillis)
    }

    @Test
    fun finish_emitsCompletedWhenMandatoryAnswersPresent() {
        val ready = start.copy(
            step = OnboardingStep.REVIEW,
            draft = start.draft.copy(
                foodPattern = FoodPattern.VEG_PLUS_NON_VEG,
                primaryGoal = PrimaryGoal.TRACK_ONLY,
                bodyProfile = BodyProfile(heightCm = 170.0, weightKg = 68.0),
            ),
        )
        val r = OnboardingReducer.reduce(ready, OnboardingIntent.Finish(epochMillis = 999L))
        assertEquals(OnboardingEffect.Completed, r.effect)
        assertEquals(999L, r.state.draft.completedAtEpochMillis)
    }

    @Test
    fun implausibleBody_blocksFinishAndGoalContinue() {
        val bad = start.copy(
            step = OnboardingStep.GOAL_BODY,
            draft = start.draft.copy(
                foodPattern = FoodPattern.VEGAN,
                primaryGoal = PrimaryGoal.LOSE_FAT,
                bodyProfile = BodyProfile(heightCm = 30.0),
            ),
        )
        assertFalse(bad.canContinue)
        assertFalse(bad.canFinish)
    }

    // --- resume (PRD p.11) ----------------------------------------------------

    @Test
    fun resumeStep_goesToFirstIncompleteRequiredStep() {
        val fresh = start.draft
        assertEquals(OnboardingStep.FOOD_PATTERN, OnboardingReducer.resumeStep(fresh))

        val patternDone = fresh.copy(completedStepIds = setOf(OnboardingStep.FOOD_PATTERN.id))
        assertEquals(OnboardingStep.GOAL_BODY, OnboardingReducer.resumeStep(patternDone))

        val bothDone = fresh.copy(
            completedStepIds = setOf(OnboardingStep.FOOD_PATTERN.id, OnboardingStep.GOAL_BODY.id),
            currentStepId = OnboardingStep.VOICE_LANGUAGE.id,
        )
        assertEquals(OnboardingStep.VOICE_LANGUAGE, OnboardingReducer.resumeStep(bothDone))
    }
}
