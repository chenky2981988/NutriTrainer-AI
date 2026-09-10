package `in`.acstechnologies.nutritrainerai.ui.onboarding

import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal

/**
 * Interim in-code strings. These move to versioned localization packs before
 * launch (PRD §4.1.2 "labels come from versioned localization packs"). Tone per
 * PRD §4.1.3 "Canonical screen copy": neutral, specific, no "pure veg", no moral
 * labels, no guilt.
 */
internal object OnboardingCopy {

    data class StepCopy(
        val title: String,
        val helper: String?,
        val placeholderBody: String,
    )

    fun forStep(step: OnboardingStep): StepCopy = when (step) {
        OnboardingStep.WELCOME -> StepCopy(
            "NutriTrainer AI",
            "Describe meals in your own words. Estimates are editable, your data stays on this device, and everything works offline.",
            "Tap Continue to get started.",
        )
        OnboardingStep.FOOD_PATTERN -> StepCopy(
            "Which foods do you eat?",
            "This is the one setting we can't skip — it keeps suggestions safe.",
            "",
        )
        OnboardingStep.FOOD_DETAILS -> StepCopy(
            "Any specifics?",
            "Pick the animal foods you eat, or how you handle dairy and eggs.",
            "Food details — content coming next.",
        )
        OnboardingStep.SAFETY_EXCLUSIONS -> StepCopy(
            "Anything to keep out?",
            "Allergies are treated as hard filters. Foods you simply avoid are kept separate.",
            "Safety exclusions — content coming next.",
        )
        OnboardingStep.STATE_UT -> StepCopy(
            "Which region's food feels like home?",
            "This helps us recognise local names and portions. It does not limit what you can eat.",
            "",
        )
        OnboardingStep.FOOD_TRADITIONS -> StepCopy(
            "Any food traditions you follow?",
            "Choose as many as you like. We never assume your diet from your community.",
            "Food traditions — content coming next.",
        )
        OnboardingStep.HOME_FOOD_PROFILE -> StepCopy(
            "How does your kitchen usually cook?",
            "All optional — you can teach the app later in chat.",
            "Home food profile — content coming next.",
        )
        OnboardingStep.GOAL_BODY -> StepCopy(
            "What are you here for?",
            "Pick one. You can change it any time.",
            "",
        )
        OnboardingStep.VOICE_LANGUAGE -> StepCopy(
            "Voice and language",
            "Choose your app language, then the languages you'll log in.",
            "Voice and language — content coming next.",
        )
        OnboardingStep.REVIEW -> StepCopy(
            "Does this look right?",
            "Your food category always takes priority over regional suggestions.",
            "",
        )
    }

    fun foodPatternLabel(p: FoodPattern): String = when (p) {
        FoodPattern.VEGETARIAN -> "Vegetarian"
        FoodPattern.VEG_PLUS_NON_VEG -> "Veg + non-veg"
        FoodPattern.VEGAN -> "Vegan"
        FoodPattern.VEG_PLUS_EGGS -> "Veg + eggs (eggetarian)"
    }

    fun goalLabel(g: PrimaryGoal): String = when (g) {
        PrimaryGoal.MAINTAIN -> "Maintain"
        PrimaryGoal.LOSE_FAT -> "Lose fat"
        PrimaryGoal.GAIN_MUSCLE_OR_WEIGHT -> "Gain muscle or weight"
        PrimaryGoal.LOSE_FAT_GAIN_MUSCLE -> "Lose fat + gain muscle"
        PrimaryGoal.IMPROVE_NUTRITION -> "Improve nutrition / protein"
        PrimaryGoal.TRACK_ONLY -> "Track only"
    }
}
