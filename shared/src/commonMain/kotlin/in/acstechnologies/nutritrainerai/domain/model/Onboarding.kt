package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * The primary dietary classification and safety filter (PRD §4.1). Region and
 * culture personalise only *within* the pattern; they never widen it.
 */
@Serializable
enum class FoodPattern {
    VEGETARIAN,
    VEG_PLUS_NON_VEG,
    VEGAN,
    VEG_PLUS_EGGS,
    ;

    /** Whether the user may additionally select specific animal foods (OB-03). */
    val allowsAnimalFoodSelection: Boolean get() = this == VEG_PLUS_NON_VEG

    /** Vegan excludes dairy, honey and other animal-derived foods (PRD §4.1). */
    val allowsDairy: Boolean get() = this != VEGAN
}

/** Animal foods a veg-plus-non-veg user may eat (PRD OB-03 multi-select). */
@Serializable
enum class AnimalFood { EGGS, CHICKEN, MUTTON_GOAT, FISH_SEAFOOD, PORK, BEEF_BUFFALO, OTHER }

/** One primary goal (PRD OB-08). Body recomposition is its own code. */
@Serializable
enum class PrimaryGoal {
    MAINTAIN,
    LOSE_FAT,
    GAIN_MUSCLE_OR_WEIGHT,
    LOSE_FAT_GAIN_MUSCLE,
    IMPROVE_NUTRITION,
    TRACK_ONLY,
}

@Serializable
enum class BodyUnits { METRIC, IMPERIAL }

@Serializable
enum class AgeBand { UNDER_18, A18_24, A25_34, A35_44, A45_54, A55_64, A65_PLUS }

/** Optional, only for energy/BMR equations (PRD OB-02 "optional sex for equations"). */
@Serializable
enum class SexForEquations { FEMALE, MALE, UNSPECIFIED }

@Serializable
data class BodyProfile(
    val units: BodyUnits = BodyUnits.METRIC,
    val ageBand: AgeBand? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val sexForEquations: SexForEquations = SexForEquations.UNSPECIFIED,
) {
    /** Plausible-range check for OB-08's "validate plausible ranges" rule. */
    val isPlausible: Boolean
        get() = (heightCm == null || heightCm in 90.0..250.0) &&
            (weightKg == null || weightKg in 20.0..400.0)
}

/** PRD OB-09: app display language is chosen separately from logging languages. */
@Serializable
enum class LanguageMode { ENGLISH_ONLY, ENGLISH_PLUS_REGIONAL, REGIONAL_ONLY }

/** PRD OB-07 — all optional, "teach later in chat" is always available. */
@Serializable
data class HouseholdFoodProfile(
    val stapleGrainIds: List<String> = emptyList(),
    val oilIds: List<String> = emptyList(),
    val dairyType: String? = null,
    val spiceLevel: String? = null,
    val usualMealTimes: List<String> = emptyList(),
    val fastingPattern: String? = null,
)

/**
 * The whole onboarding answer set, saved locally as a draft after every step so
 * termination never forces a restart (PRD §4.1.2). Stores stable IDs only;
 * display labels come from versioned localization packs.
 */
@Serializable
data class OnboardingState(
    val version: Int = SCHEMA_VERSION,
    val currentStepId: String = OnboardingStep.entries.first().id,
    val completedStepIds: Set<String> = emptySet(),
    val skippedStepIds: Set<String> = emptySet(),

    // OB-02 / OB-03
    val foodPattern: FoodPattern? = null,
    val allowedAnimalFoods: Set<AnimalFood> = emptySet(),
    val dairyAllowed: Boolean = true,
    val foodDetailIds: Set<String> = emptySet(),

    // OB-04
    val allergyIds: Set<String> = emptySet(),
    val avoidanceText: String = "",

    // OB-05 / OB-06
    val countryCode: String? = null,
    val stateUtCode: String? = null,
    val subRegionIds: Set<String> = emptySet(),
    val traditionIds: Set<String> = emptySet(),
    val customTraditionText: String = "",

    // OB-07
    val householdProfile: HouseholdFoodProfile = HouseholdFoodProfile(),

    // OB-08
    val primaryGoal: PrimaryGoal? = null,
    val bodyProfile: BodyProfile = BodyProfile(),

    // OB-09
    val appLocale: String? = null,
    val languageMode: LanguageMode = LanguageMode.ENGLISH_ONLY,
    val loggingLanguageCodes: List<String> = emptyList(),

    val consentTimestamps: Map<String, Long> = emptyMap(),
    val completedAtEpochMillis: Long? = null,
) {
    val isComplete: Boolean get() = completedAtEpochMillis != null

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}
