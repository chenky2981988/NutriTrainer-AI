package `in`.acstechnologies.nutritrainerai.ai.parser

import `in`.acstechnologies.nutritrainerai.ai.Clarification
import `in`.acstechnologies.nutritrainerai.ai.ClarificationReason
import `in`.acstechnologies.nutritrainerai.ai.InterpretRequest
import `in`.acstechnologies.nutritrainerai.ai.InterpretResult
import `in`.acstechnologies.nutritrainerai.ai.IntentKind
import `in`.acstechnologies.nutritrainerai.ai.MealSlot
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntent
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntentValidator
import `in`.acstechnologies.nutritrainerai.ai.NutritionLanguageEngine
import `in`.acstechnologies.nutritrainerai.ai.ParsedItem
import `in`.acstechnologies.nutritrainerai.ai.ParsedQuantity
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus

/**
 * A rule-based [NutritionLanguageEngine] with **no model dependency** — the
 * always-available fallback the Prototype phase runs on, and the safety net when
 * a system LLM is missing or regresses (PRD §8 "Deterministic parser"; §13
 * "System LLM unavailable → parser fallback").
 *
 * It is intentionally modest: English only, no household-unit resolution, and
 * every result is [ConfidenceBand.LOW] (PRD §6 band 6 "heuristic estimate — last
 * resort, ranged, low confidence"). It never invents nutrition numbers; it only
 * shapes the utterance into a [NutritionIntent] for the deterministic resolver
 * and calculator downstream. Anything it cannot parse becomes
 * [IntentKind.UNKNOWN] with a clarification — never a silent guess.
 */
class DeterministicNutritionParser : NutritionLanguageEngine {

    override val id: String = "deterministic-parser"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun interpret(request: InterpretRequest): InterpretResult {
        val raw = request.utterance.trim()
        if (raw.isEmpty()) return InterpretResult.Invalid(listOf("utterance was blank"))

        val body = raw.lowercase().trimEnd('.', '!', '?', ' ')
        val intent = when (classify(body)) {
            IntentKind.REMOVE -> buildRemoveOrCorrect(raw, body, IntentKind.REMOVE)
            IntentKind.CORRECT -> buildRemoveOrCorrect(raw, body, IntentKind.CORRECT)
            IntentKind.REPEAT -> buildRepeat(raw)
            IntentKind.SUGGEST -> mutationFreeIntent(IntentKind.SUGGEST, raw)
            IntentKind.ASK -> mutationFreeIntent(IntentKind.ASK, raw)
            IntentKind.PLAN -> buildLog(raw, body, IntentKind.PLAN)
            IntentKind.CONSUMED -> buildLog(raw, body, IntentKind.CONSUMED)
            IntentKind.UNKNOWN -> unknownIntent(raw)
        }

        val violations = NutritionIntentValidator.validate(intent)
        return if (violations.isEmpty()) {
            InterpretResult.Success(intent)
        } else {
            // Defensive: the parser aims to only ever emit valid intents.
            InterpretResult.Invalid(violations, rawOutput = intent.toString())
        }
    }

    // --- intent classification --------------------------------------------------

    private fun classify(body: String): IntentKind = when {
        EnglishLexicon.REMOVE_MARKERS.any { body.startsWith(it.trim()) || body.contains(it) } -> IntentKind.REMOVE
        EnglishLexicon.SUGGEST_MARKERS.any { body.contains(it) } -> IntentKind.SUGGEST
        EnglishLexicon.REPEAT_MARKERS.any { body.contains(it) } -> IntentKind.REPEAT
        isCorrection(body) -> IntentKind.CORRECT
        EnglishLexicon.PLAN_MARKERS.any { body.contains(it) } -> IntentKind.PLAN
        isQuestion(body) -> IntentKind.ASK
        hasFoodContent(body) -> IntentKind.CONSUMED
        else -> IntentKind.UNKNOWN
    }

    private fun isCorrection(body: String): Boolean =
        EnglishLexicon.CORRECTION_REGEX.containsMatchIn(body) ||
            EnglishLexicon.CORRECTION_WORDS.any { body.contains(it) }

    private fun isQuestion(body: String): Boolean =
        EnglishLexicon.QUESTION_STARTERS.any { body == it || body.startsWith("$it ") }

    private fun hasFoodContent(body: String): Boolean =
        tokensFor(stripLeadingNoise(body)).any { token ->
            token.length > 1 &&
                token.any(Char::isLetter) &&
                token !in EnglishLexicon.FOOD_STOPWORDS &&
                token !in EnglishLexicon.UNIT_TOKENS &&
                token !in EnglishLexicon.NUMBER_WORDS
        }

    // --- builders -------------------------------------------------------------

    private fun buildLog(raw: String, body: String, kind: IntentKind): NutritionIntent {
        val status = if (kind == IntentKind.PLAN) MealItemStatus.PLANNED else MealItemStatus.CONSUMED
        val meal = detectMeal(body)
        val items = extractItems(stripLeadingNoise(body), status, meal)
        return if (items.isEmpty()) {
            unknownIntent(raw)
        } else {
            NutritionIntent(
                kind = kind,
                items = items,
                overallConfidence = ConfidenceBand.LOW,
                sourceTranscript = raw,
            )
        }
    }

    private fun buildRemoveOrCorrect(raw: String, body: String, kind: IntentKind): NutritionIntent {
        val meal = detectMeal(body)
        val items = stripLeadingNoise(body)
            .split(EnglishLexicon.ITEM_SEPARATORS)
            .map { it.trim() }
            .filterNot { it.isEmpty() || it.startsWith("not ") || it == "not" || it.startsWith("instead") }
            .mapNotNull { parseFragment(it, MealItemStatus.CONSUMED, meal) }

        if (items.isEmpty()) return unknownIntent(raw)

        val verb = if (kind == IntentKind.REMOVE) "Remove" else "Update"
        return NutritionIntent(
            kind = kind,
            items = items,
            clarification = Clarification(
                question = "$verb which \"${items.first().foodName}\" entry?",
                reason = ClarificationReason.TARGET_AMBIGUOUS,
                itemIndex = 0,
            ),
            overallConfidence = ConfidenceBand.LOW,
            sourceTranscript = raw,
        )
    }

    private fun buildRepeat(raw: String): NutritionIntent =
        NutritionIntent(
            kind = IntentKind.REPEAT,
            items = emptyList(),
            clarification = Clarification(
                question = "Repeat which meal? You'll see a preview before anything is saved.",
                reason = ClarificationReason.TARGET_AMBIGUOUS,
            ),
            overallConfidence = ConfidenceBand.LOW,
            sourceTranscript = raw,
        )

    private fun mutationFreeIntent(kind: IntentKind, raw: String): NutritionIntent =
        NutritionIntent(
            kind = kind,
            items = emptyList(),
            overallConfidence = ConfidenceBand.LOW,
            sourceTranscript = raw,
        )

    private fun unknownIntent(raw: String): NutritionIntent =
        NutritionIntent(
            kind = IntentKind.UNKNOWN,
            items = emptyList(),
            clarification = Clarification(
                question = "Sorry, I didn't catch that — what did you eat, or what would you like to do?",
                reason = ClarificationReason.UTTERANCE_UNCLEAR,
            ),
            overallConfidence = ConfidenceBand.UNRESOLVED,
            sourceTranscript = raw,
        )

    // --- item extraction ---------------------------------------------------------

    private fun extractItems(body: String, status: MealItemStatus, meal: MealSlot): List<ParsedItem> =
        body.split(EnglishLexicon.ITEM_SEPARATORS)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseFragment(it, status, meal) }

    private fun parseFragment(fragment: String, status: MealItemStatus, meal: MealSlot): ParsedItem? {
        val tokens = tokensFor(fragment)
        if (tokens.isEmpty()) return null

        var amount: Double? = null
        var amountFromDigit = false
        var unit: String? = null
        val foodTokens = mutableListOf<String>()

        for (token in tokens) {
            val digit = token.toDoubleOrNull()
            when {
                amount == null && digit != null -> {
                    amount = digit
                    amountFromDigit = true
                }
                amount == null && EnglishLexicon.NUMBER_WORDS.containsKey(token) ->
                    amount = EnglishLexicon.NUMBER_WORDS.getValue(token)
                unit == null && EnglishLexicon.UNIT_TOKENS.containsKey(token) ->
                    unit = EnglishLexicon.UNIT_TOKENS.getValue(token)
                token in EnglishLexicon.FOOD_STOPWORDS -> Unit
                token in EnglishLexicon.MEAL_WORDS -> Unit
                else -> foodTokens += token
            }
        }

        val foodName = foodTokens.joinToString(" ").trim()
        if (foodName.isEmpty()) return null

        val quantity = when {
            amount != null && unit != null -> ParsedQuantity(amount, unit, approximate = isApproximate(amountFromDigit, unit))
            amount != null && unit == null -> ParsedQuantity(amount, "piece", approximate = true)
            amount == null && unit != null -> ParsedQuantity(1.0, unit, approximate = true)
            else -> null
        }

        return ParsedItem(
            rawText = fragment,
            foodName = foodName,
            quantity = quantity,
            meal = meal,
            status = status,
            confidence = ConfidenceBand.LOW,
        )
    }

    // --- helpers ---------------------------------------------------------------

    private fun tokensFor(text: String): List<String> =
        text.split(WHITESPACE).mapNotNull { token ->
            token.trim { it == '.' || it == ',' || it == '!' || it == '?' || it == '(' || it == ')' }
                .takeIf { it.isNotEmpty() }
        }

    private fun stripLeadingNoise(body: String): String {
        var current = body
        var changed = true
        while (changed) {
            changed = false
            for (phrase in LEADING_NOISE_BY_LENGTH) {
                if (current == phrase) {
                    current = ""
                    changed = true
                    break
                }
                if (current.startsWith("$phrase ")) {
                    current = current.removePrefix("$phrase ").trim()
                    changed = true
                    break
                }
            }
        }
        return current
    }

    private fun detectMeal(body: String): MealSlot =
        EnglishLexicon.MEAL_WORDS.entries
            .firstOrNull { body.contains(it.key) }
            ?.let { MealSlot.valueOf(it.value) }
            ?: MealSlot.UNSPECIFIED

    private fun isApproximate(amountFromDigit: Boolean, unit: String): Boolean =
        !amountFromDigit || unit !in EXACT_UNITS

    private companion object {
        val WHITESPACE = Regex("""\s+""")
        val EXACT_UNITS = setOf("g", "kg", "ml", "l")
        val LEADING_NOISE_BY_LENGTH = EnglishLexicon.LEADING_NOISE.sortedByDescending { it.length }
    }
}
