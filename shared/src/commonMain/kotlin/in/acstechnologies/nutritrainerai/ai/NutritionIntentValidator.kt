package `in`.acstechnologies.nutritrainerai.ai

import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus

/**
 * Gatekeeper for engine output (PRD §8 "Reject invalid output and request
 * confirmation"; "Invalid output is never written directly"). Pure and
 * exhaustively testable — part of the §8 quality-gate corpus (schema validity
 * ≥ 99.5% after validation).
 */
object NutritionIntentValidator {

    /** Empty list ⇒ acceptable. Otherwise, every reason it is not. */
    fun validate(intent: NutritionIntent): List<String> {
        val errors = mutableListOf<String>()

        if (intent.schemaVersion != NutritionIntent.SCHEMA_VERSION) {
            errors += "unsupported schemaVersion ${intent.schemaVersion} " +
                "(expected ${NutritionIntent.SCHEMA_VERSION})"
        }
        if (intent.sourceTranscript.isBlank()) {
            errors += "sourceTranscript must be preserved (PRD FR01)"
        }

        when (intent.kind) {
            IntentKind.CONSUMED, IntentKind.PLAN ->
                if (intent.items.isEmpty()) errors += "${intent.kind} requires at least one item"

            IntentKind.CORRECT, IntentKind.REMOVE ->
                if (intent.items.none { it.targetItemRef != null } && intent.clarification == null) {
                    errors += "${intent.kind} needs a targetItemRef on an item, or a clarification"
                }

            IntentKind.REPEAT ->
                if (intent.items.isEmpty() && intent.clarification == null) {
                    errors += "REPEAT needs items to clone, or a clarification"
                }

            IntentKind.ASK, IntentKind.SUGGEST -> Unit // no mutation; items optional

            IntentKind.UNKNOWN ->
                if (intent.clarification == null) {
                    errors += "UNKNOWN must carry a clarification, never a silent guess (PRD §8)"
                }
        }

        intent.items.forEachIndexed { i, item ->
            if (item.foodName.isBlank()) errors += "items[$i].foodName is blank"
            if (item.rawText.isBlank()) errors += "items[$i].rawText is blank"
            item.quantity?.let { q ->
                if (!q.amount.isFinite() || q.amount < 0.0) {
                    errors += "items[$i].quantity.amount invalid: ${q.amount}"
                }
                if (q.unit.isBlank()) errors += "items[$i].quantity.unit is blank"
            }
            val required = when (intent.kind) {
                IntentKind.PLAN -> MealItemStatus.PLANNED
                IntentKind.CONSUMED -> MealItemStatus.CONSUMED
                else -> null
            }
            if (required != null && item.status != required) {
                errors += "items[$i].status ${item.status} contradicts intent ${intent.kind} " +
                    "(plan vs consumed must not blur — PRD §4.3)"
            }
        }

        intent.clarification?.let { c ->
            if (c.question.isBlank()) errors += "clarification.question is blank"
            c.itemIndex?.let { idx ->
                if (idx !in intent.items.indices) {
                    errors += "clarification.itemIndex $idx out of range (items=${intent.items.size})"
                }
            }
        }

        return errors
    }

    fun isValid(intent: NutritionIntent): Boolean = validate(intent).isEmpty()
}
