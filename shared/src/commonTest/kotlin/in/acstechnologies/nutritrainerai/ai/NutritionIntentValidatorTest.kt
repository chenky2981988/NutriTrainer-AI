package `in`.acstechnologies.nutritrainerai.ai

import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionIntentValidatorTest {

    private fun item(
        name: String = "rice",
        status: MealItemStatus = MealItemStatus.CONSUMED,
        targetRef: String? = null,
        amount: Double = 150.0,
        unit: String = "g",
    ) = ParsedItem(
        rawText = "$name phrase",
        foodName = name,
        quantity = ParsedQuantity(amount, unit),
        status = status,
        confidence = ConfidenceBand.MEDIUM,
        targetItemRef = targetRef,
    )

    private fun intent(
        kind: IntentKind,
        items: List<ParsedItem> = emptyList(),
        clarification: Clarification? = null,
        transcript: String = "i had rice",
        schemaVersion: Int = NutritionIntent.SCHEMA_VERSION,
    ) = NutritionIntent(
        schemaVersion = schemaVersion,
        kind = kind,
        items = items,
        clarification = clarification,
        overallConfidence = ConfidenceBand.MEDIUM,
        sourceTranscript = transcript,
    )

    @Test
    fun consumed_withItems_isValid() {
        assertTrue(NutritionIntentValidator.isValid(intent(IntentKind.CONSUMED, listOf(item()))))
    }

    @Test
    fun consumed_withNoItems_isRejected() {
        val errors = NutritionIntentValidator.validate(intent(IntentKind.CONSUMED))
        assertContains(errors, "CONSUMED requires at least one item")
    }

    @Test
    fun plan_itemMarkedConsumed_isRejected() {
        val errors = NutritionIntentValidator.validate(
            intent(IntentKind.PLAN, listOf(item(status = MealItemStatus.CONSUMED))),
        )
        assertTrue(errors.any { it.contains("contradicts intent PLAN") })
    }

    @Test
    fun correct_withoutTargetOrClarification_isRejected() {
        val errors = NutritionIntentValidator.validate(intent(IntentKind.CORRECT, listOf(item())))
        assertContains(errors, "CORRECT needs a targetItemRef on an item, or a clarification")
    }

    @Test
    fun correct_withTargetRef_isValid() {
        assertTrue(
            NutritionIntentValidator.isValid(
                intent(IntentKind.CORRECT, listOf(item(targetRef = "meal-item-42"))),
            ),
        )
    }

    @Test
    fun unknown_withoutClarification_isRejected() {
        val errors = NutritionIntentValidator.validate(intent(IntentKind.UNKNOWN))
        assertTrue(errors.any { it.contains("UNKNOWN must carry a clarification") })
    }

    @Test
    fun unknown_withClarification_isValid() {
        val c = Clarification("What did you eat?", ClarificationReason.UTTERANCE_UNCLEAR)
        assertTrue(NutritionIntentValidator.isValid(intent(IntentKind.UNKNOWN, clarification = c)))
    }

    @Test
    fun blankTranscript_isRejected() {
        val errors = NutritionIntentValidator.validate(
            intent(IntentKind.ASK, transcript = "   "),
        )
        assertContains(errors, "sourceTranscript must be preserved (PRD FR01)")
    }

    @Test
    fun unknownSchemaVersion_isRejected() {
        val errors = NutritionIntentValidator.validate(
            intent(IntentKind.CONSUMED, listOf(item()), schemaVersion = 99),
        )
        assertTrue(errors.any { it.startsWith("unsupported schemaVersion 99") })
    }

    @Test
    fun negativeQuantity_isRejected() {
        val errors = NutritionIntentValidator.validate(
            intent(IntentKind.CONSUMED, listOf(item(amount = -5.0))),
        )
        assertTrue(errors.any { it.contains("quantity.amount invalid") })
    }

    @Test
    fun clarificationItemIndexOutOfRange_isRejected() {
        val c = Clarification("Raw or cooked?", ClarificationReason.RAW_VS_COOKED, itemIndex = 3)
        val errors = NutritionIntentValidator.validate(
            intent(IntentKind.CONSUMED, listOf(item()), clarification = c),
        )
        assertTrue(errors.any { it.contains("clarification.itemIndex 3 out of range") })
    }

    @Test
    fun ask_withNoItems_isValid() {
        assertEquals(emptyList(), NutritionIntentValidator.validate(intent(IntentKind.ASK)))
    }
}
