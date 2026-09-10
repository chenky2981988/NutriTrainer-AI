package `in`.acstechnologies.nutritrainerai.ui.onboarding

import `in`.acstechnologies.nutritrainerai.domain.model.AnimalFood
import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.LanguageMode
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.allergy_egg
import nutritrainerai.shared.generated.resources.allergy_fish
import nutritrainerai.shared.generated.resources.allergy_milk
import nutritrainerai.shared.generated.resources.allergy_peanut
import nutritrainerai.shared.generated.resources.allergy_sesame
import nutritrainerai.shared.generated.resources.allergy_shellfish
import nutritrainerai.shared.generated.resources.allergy_soy
import nutritrainerai.shared.generated.resources.allergy_tree_nut
import nutritrainerai.shared.generated.resources.allergy_wheat
import nutritrainerai.shared.generated.resources.animal_beef_buffalo
import nutritrainerai.shared.generated.resources.animal_chicken
import nutritrainerai.shared.generated.resources.animal_eggs
import nutritrainerai.shared.generated.resources.animal_fish_seafood
import nutritrainerai.shared.generated.resources.animal_mutton_goat
import nutritrainerai.shared.generated.resources.animal_other
import nutritrainerai.shared.generated.resources.animal_pork
import nutritrainerai.shared.generated.resources.food_pattern_veg_eggs
import nutritrainerai.shared.generated.resources.food_pattern_veg_nonveg
import nutritrainerai.shared.generated.resources.food_pattern_vegan
import nutritrainerai.shared.generated.resources.food_pattern_vegetarian
import nutritrainerai.shared.generated.resources.goal_gain
import nutritrainerai.shared.generated.resources.goal_improve_nutrition
import nutritrainerai.shared.generated.resources.goal_lose_fat
import nutritrainerai.shared.generated.resources.goal_maintain
import nutritrainerai.shared.generated.resources.goal_recomp
import nutritrainerai.shared.generated.resources.goal_track_only
import nutritrainerai.shared.generated.resources.grain_bajra
import nutritrainerai.shared.generated.resources.grain_jowar
import nutritrainerai.shared.generated.resources.grain_maize
import nutritrainerai.shared.generated.resources.grain_millet
import nutritrainerai.shared.generated.resources.grain_ragi
import nutritrainerai.shared.generated.resources.grain_rice
import nutritrainerai.shared.generated.resources.grain_wheat
import nutritrainerai.shared.generated.resources.lang_bn
import nutritrainerai.shared.generated.resources.lang_en
import nutritrainerai.shared.generated.resources.lang_gu
import nutritrainerai.shared.generated.resources.lang_hi
import nutritrainerai.shared.generated.resources.lang_kn
import nutritrainerai.shared.generated.resources.lang_ml
import nutritrainerai.shared.generated.resources.lang_mr
import nutritrainerai.shared.generated.resources.lang_pa
import nutritrainerai.shared.generated.resources.lang_ta
import nutritrainerai.shared.generated.resources.lang_te
import nutritrainerai.shared.generated.resources.language_mode_english_only
import nutritrainerai.shared.generated.resources.language_mode_english_plus
import nutritrainerai.shared.generated.resources.language_mode_regional_only
import nutritrainerai.shared.generated.resources.ob_food_details_helper
import nutritrainerai.shared.generated.resources.ob_food_details_title
import nutritrainerai.shared.generated.resources.ob_food_pattern_helper
import nutritrainerai.shared.generated.resources.ob_food_pattern_title
import nutritrainerai.shared.generated.resources.ob_goal_helper
import nutritrainerai.shared.generated.resources.ob_goal_title
import nutritrainerai.shared.generated.resources.ob_home_food_helper
import nutritrainerai.shared.generated.resources.ob_home_food_title
import nutritrainerai.shared.generated.resources.ob_region_helper
import nutritrainerai.shared.generated.resources.ob_region_title
import nutritrainerai.shared.generated.resources.ob_review_helper
import nutritrainerai.shared.generated.resources.ob_review_title
import nutritrainerai.shared.generated.resources.ob_safety_helper
import nutritrainerai.shared.generated.resources.ob_safety_title
import nutritrainerai.shared.generated.resources.ob_traditions_helper
import nutritrainerai.shared.generated.resources.ob_traditions_title
import nutritrainerai.shared.generated.resources.ob_voice_helper
import nutritrainerai.shared.generated.resources.ob_voice_title
import nutritrainerai.shared.generated.resources.ob_welcome_helper
import nutritrainerai.shared.generated.resources.ob_welcome_title
import org.jetbrains.compose.resources.StringResource

/**
 * String-resource lookups for onboarding. Values live in
 * `composeResources/values/strings.xml`; add `values-<lang>/` to localise
 * (PRD §4.1.2). Region names and food-tradition labels are *not* here — those
 * are data, keyed by stable IDs.
 */
internal object OnboardingCopy {

    data class StepCopy(val title: StringResource, val helper: StringResource?)

    fun forStep(step: OnboardingStep): StepCopy = when (step) {
        OnboardingStep.WELCOME -> StepCopy(Res.string.ob_welcome_title, Res.string.ob_welcome_helper)
        OnboardingStep.FOOD_PATTERN -> StepCopy(Res.string.ob_food_pattern_title, Res.string.ob_food_pattern_helper)
        OnboardingStep.FOOD_DETAILS -> StepCopy(Res.string.ob_food_details_title, Res.string.ob_food_details_helper)
        OnboardingStep.SAFETY_EXCLUSIONS -> StepCopy(Res.string.ob_safety_title, Res.string.ob_safety_helper)
        OnboardingStep.STATE_UT -> StepCopy(Res.string.ob_region_title, Res.string.ob_region_helper)
        OnboardingStep.FOOD_TRADITIONS -> StepCopy(Res.string.ob_traditions_title, Res.string.ob_traditions_helper)
        OnboardingStep.HOME_FOOD_PROFILE -> StepCopy(Res.string.ob_home_food_title, Res.string.ob_home_food_helper)
        OnboardingStep.GOAL_BODY -> StepCopy(Res.string.ob_goal_title, Res.string.ob_goal_helper)
        OnboardingStep.VOICE_LANGUAGE -> StepCopy(Res.string.ob_voice_title, Res.string.ob_voice_helper)
        OnboardingStep.REVIEW -> StepCopy(Res.string.ob_review_title, Res.string.ob_review_helper)
    }

    fun foodPatternLabel(p: FoodPattern): StringResource = when (p) {
        FoodPattern.VEGETARIAN -> Res.string.food_pattern_vegetarian
        FoodPattern.VEG_PLUS_NON_VEG -> Res.string.food_pattern_veg_nonveg
        FoodPattern.VEGAN -> Res.string.food_pattern_vegan
        FoodPattern.VEG_PLUS_EGGS -> Res.string.food_pattern_veg_eggs
    }

    fun goalLabel(g: PrimaryGoal): StringResource = when (g) {
        PrimaryGoal.MAINTAIN -> Res.string.goal_maintain
        PrimaryGoal.LOSE_FAT -> Res.string.goal_lose_fat
        PrimaryGoal.GAIN_MUSCLE_OR_WEIGHT -> Res.string.goal_gain
        PrimaryGoal.LOSE_FAT_GAIN_MUSCLE -> Res.string.goal_recomp
        PrimaryGoal.IMPROVE_NUTRITION -> Res.string.goal_improve_nutrition
        PrimaryGoal.TRACK_ONLY -> Res.string.goal_track_only
    }

    fun animalFoodLabel(f: AnimalFood): StringResource = when (f) {
        AnimalFood.EGGS -> Res.string.animal_eggs
        AnimalFood.CHICKEN -> Res.string.animal_chicken
        AnimalFood.MUTTON_GOAT -> Res.string.animal_mutton_goat
        AnimalFood.FISH_SEAFOOD -> Res.string.animal_fish_seafood
        AnimalFood.PORK -> Res.string.animal_pork
        AnimalFood.BEEF_BUFFALO -> Res.string.animal_beef_buffalo
        AnimalFood.OTHER -> Res.string.animal_other
    }

    fun languageModeLabel(m: LanguageMode): StringResource = when (m) {
        LanguageMode.ENGLISH_ONLY -> Res.string.language_mode_english_only
        LanguageMode.ENGLISH_PLUS_REGIONAL -> Res.string.language_mode_english_plus
        LanguageMode.REGIONAL_ONLY -> Res.string.language_mode_regional_only
    }

    /** id -> label resource. ids are stable; labels localise via strings.xml. */
    val COMMON_ALLERGIES: List<Pair<String, StringResource>> = listOf(
        "milk" to Res.string.allergy_milk,
        "egg" to Res.string.allergy_egg,
        "peanut" to Res.string.allergy_peanut,
        "tree_nut" to Res.string.allergy_tree_nut,
        "soy" to Res.string.allergy_soy,
        "wheat" to Res.string.allergy_wheat,
        "fish" to Res.string.allergy_fish,
        "shellfish" to Res.string.allergy_shellfish,
        "sesame" to Res.string.allergy_sesame,
    )

    val STAPLE_GRAINS: List<Pair<String, StringResource>> = listOf(
        "rice" to Res.string.grain_rice,
        "wheat" to Res.string.grain_wheat,
        "millet" to Res.string.grain_millet,
        "maize" to Res.string.grain_maize,
        "ragi" to Res.string.grain_ragi,
        "jowar" to Res.string.grain_jowar,
        "bajra" to Res.string.grain_bajra,
    )

    val LOGGING_LANGUAGES: List<Pair<String, StringResource>> = listOf(
        "en" to Res.string.lang_en,
        "hi" to Res.string.lang_hi,
        "ta" to Res.string.lang_ta,
        "te" to Res.string.lang_te,
        "bn" to Res.string.lang_bn,
        "mr" to Res.string.lang_mr,
        "kn" to Res.string.lang_kn,
        "ml" to Res.string.lang_ml,
        "gu" to Res.string.lang_gu,
        "pa" to Res.string.lang_pa,
    )
}
