package `in`.acstechnologies.nutritrainerai.domain.model

/**
 * The ten single-task onboarding screens (PRD §4.1.2 "OB-01 … OB-10").
 * Progress is "Step n of 10" where n is [number]. Only [FOOD_PATTERN] and
 * [GOAL_BODY] must be answered to finish; everything else is skippable
 * (PRD §4.1.2, page 16).
 */
enum class OnboardingStep(val id: String, val required: Boolean) {
    WELCOME("OB-01", required = false),
    FOOD_PATTERN("OB-02", required = true),
    FOOD_DETAILS("OB-03", required = false),
    SAFETY_EXCLUSIONS("OB-04", required = false),
    STATE_UT("OB-05", required = false),
    FOOD_TRADITIONS("OB-06", required = false),
    HOME_FOOD_PROFILE("OB-07", required = false),
    GOAL_BODY("OB-08", required = true),
    VOICE_LANGUAGE("OB-09", required = false),
    REVIEW("OB-10", required = false),
    ;

    /** 1-based position for the "Step n of 10" indicator. */
    val number: Int get() = ordinal + 1

    val next: OnboardingStep? get() = entries.getOrNull(ordinal + 1)
    val previous: OnboardingStep? get() = entries.getOrNull(ordinal - 1)

    companion object {
        const val TOTAL: Int = 10

        fun fromId(id: String): OnboardingStep =
            entries.firstOrNull { it.id == id } ?: WELCOME
    }
}
