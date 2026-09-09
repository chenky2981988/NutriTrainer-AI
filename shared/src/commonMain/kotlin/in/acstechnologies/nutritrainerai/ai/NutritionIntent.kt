package `in`.acstechnologies.nutritrainerai.ai

import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import kotlinx.serialization.Serializable

/**
 * The structured result of interpreting one user utterance — the "response
 * schema" of PRD §8 ("intent, status, foods, quantities, units, modifiers,
 * confidence and clarification").
 *
 * This is **model-neutral**: Gemini Nano, Apple Foundation Models, a
 * deterministic parser and a future Gemma engine all produce this same shape.
 * It carries *interpretation only* — no nutrition numbers. Deterministic
 * software resolves foods and computes nutrition afterwards (PRD "Primary
 * decision"). Output that fails [NutritionIntentValidator] is never written
 * (PRD §8).
 */
@Serializable
data class NutritionIntent(
    val schemaVersion: Int = SCHEMA_VERSION,
    val kind: IntentKind,
    val items: List<ParsedItem> = emptyList(),
    /** At most one question, asked only when it changes advice materially (PRD §5). */
    val clarification: Clarification? = null,
    val overallConfidence: ConfidenceBand,
    /** Verbatim recognised text this intent was derived from (PRD FR01). */
    val sourceTranscript: String,
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

/** The eight conversational actions of PRD §4.2, plus an explicit "not understood". */
@Serializable
enum class IntentKind {
    /** "I had two fulka and dal" — create consumed items. */
    CONSUMED,

    /** "I may eat dal dhokli" — create planned items, excluded from actual totals. */
    PLAN,

    /** "Rice was 120 g, not 200" — replace quantity/attrs on an existing item, recalculate. */
    CORRECT,

    /** "Remove the peanuts" — soft-delete with undo. */
    REMOVE,

    /** "Same breakfast as Tuesday" — preview cloned items before saving. */
    REPEAT,

    /** "Are my carbs too high?" — no data mutation. */
    ASK,

    /** "What should I eat next?" — rank validated candidates; save only after selection. */
    SUGGEST,

    /** Utterance not understood. Must carry a [Clarification]; never a silent guess (PRD §8). */
    UNKNOWN,
}

/** One food the engine extracted from the utterance, before resolution to a [Food]/grams. */
@Serializable
data class ParsedItem(
    /** The span of the utterance the engine attributed to this item. */
    val rawText: String,
    val foodName: String,
    val quantity: ParsedQuantity? = null,
    /** "boiled", "with ghee", "deep fried"… — affects which reference nutrition applies. */
    val preparation: String? = null,
    val meal: MealSlot = MealSlot.UNSPECIFIED,
    val status: MealItemStatus = MealItemStatus.CONSUMED,
    /** Per-entry modifiers: "restaurant", "extra oil", "large" (PRD §5 "Household calibration"). */
    val modifiers: List<String> = emptyList(),
    val confidence: ConfidenceBand,
    /** For [IntentKind.CORRECT]/[IntentKind.REMOVE]: the existing item this refers to, if identified. */
    val targetItemRef: String? = null,
)

/** A quantity exactly as spoken, not yet normalised (PRD §8 "quantities, units"). */
@Serializable
data class ParsedQuantity(
    val amount: Double,
    /** As uttered: "g", "katori", "piece", "cup", "ml", "handful"… resolved downstream. */
    val unit: String,
    /** The speaker hedged ("about", "a couple") — pairs with lower confidence and ranges. */
    val approximate: Boolean = false,
)

@Serializable
enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK, UNSPECIFIED }

/**
 * The single question the engine wants answered before the record is trustworthy
 * (PRD §5 "Clarification policy" — ask only when it changes advice materially).
 */
@Serializable
data class Clarification(
    val question: String,
    val reason: ClarificationReason,
    /** Optional preset answers to offer as chips. */
    val options: List<String> = emptyList(),
    /** Index into [NutritionIntent.items] the question is about, when item-specific. */
    val itemIndex: Int? = null,
)

@Serializable
enum class ClarificationReason {
    /** Raw versus cooked materially changes the result. */
    RAW_VS_COOKED,

    /** Several products match with different nutrition. */
    MULTIPLE_PRODUCTS,

    /** Serving ambiguity changes today's advice. */
    SERVING_AMBIGUITY,

    /** Allergen interpretation is uncertain. */
    ALLERGEN_UNCERTAIN,

    /**
     * A CORRECT / REMOVE / REPEAT refers to an existing record the engine could
     * not pin down (no row id). The resolver may auto-bind when exactly one
     * recent item matches; otherwise the user picks.
     */
    TARGET_AMBIGUOUS,

    /** The utterance itself is unclear (pairs with [IntentKind.UNKNOWN]). */
    UTTERANCE_UNCLEAR,
}
