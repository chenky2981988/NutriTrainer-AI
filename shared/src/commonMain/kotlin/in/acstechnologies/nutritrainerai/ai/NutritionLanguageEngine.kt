package `in`.acstechnologies.nutritrainerai.ai

import kotlinx.serialization.Serializable

/**
 * The one interface every language engine implements — Gemini Nano (ML Kit),
 * Apple Foundation Models, the deterministic parser fallback, and a future
 * Gemma engine (PRD §8 "Migration": swapping the model must not touch storage,
 * calculations or screens).
 *
 * An engine **interprets language**. It must not invent nutrition values or
 * override safety filters (PRD §6, §8). Callers pass every result through
 * [NutritionIntentValidator] and discard anything invalid.
 */
interface NutritionLanguageEngine {

    /** Stable id, e.g. "gemini-nano", "apple-foundation", "deterministic-parser", "gemma-e2b". */
    val id: String

    /** Capability check before use; the app falls back when this is false (PRD §8, §13). */
    suspend fun isAvailable(): Boolean

    /** Interpret one utterance. Never throws for model failure — returns [InterpretResult.Unavailable]. */
    suspend fun interpret(request: InterpretRequest): InterpretResult
}

/**
 * Everything an engine is given for one utterance — and nothing more
 * (PRD §8 "Prompt contract": current utterance, compact daily summary,
 * relevant household defaults, retrieved candidates).
 */
@Serializable
data class InterpretRequest(
    val utterance: String,
    /** BCP-47 tags the user logs in; the engine may answer in these. */
    val loggingLanguages: List<String> = emptyList(),
    val dailySummary: CompactDailySummary,
    val householdDefaults: List<HouseholdDefault> = emptyList(),
    /** Foods pre-retrieved by the resolver for disambiguation. */
    val retrievedCandidates: List<FoodCandidate> = emptyList(),
    /** Recent logged items, so CORRECT / REMOVE / REPEAT can anchor to real rows. */
    val recentItems: List<RecentItemRef> = emptyList(),
)

/** A deliberately small view of the day (PRD §8 "compact daily summary"). */
@Serializable
data class CompactDailySummary(
    val consumedKcal: Double,
    val targetKcal: Double? = null,
    val consumedProteinG: Double,
    val targetProteinG: Double? = null,
    val mealsLogged: Int,
)

@Serializable
data class HouseholdDefault(
    val foodName: String,
    val unitId: String,
    val gramsPerUnit: Double,
)

@Serializable
data class FoodCandidate(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val basisNote: String? = null,
)

@Serializable
data class RecentItemRef(
    val ref: String,
    /** Human label for matching, e.g. "rice · lunch · 200 g". */
    val label: String,
)

/** Outcome of [NutritionLanguageEngine.interpret]. */
sealed interface InterpretResult {

    /** The engine produced an intent. Still subject to [NutritionIntentValidator] by the caller. */
    data class Success(val intent: NutritionIntent) : InterpretResult

    /**
     * The engine ran but its output failed schema/semantic validation. Never
     * written; the caller asks the user to confirm instead (PRD §8).
     */
    data class Invalid(val violations: List<String>, val rawOutput: String? = null) : InterpretResult

    /** Engine unavailable or errored; the caller falls back to the deterministic parser. */
    data class Unavailable(val reason: String) : InterpretResult
}
