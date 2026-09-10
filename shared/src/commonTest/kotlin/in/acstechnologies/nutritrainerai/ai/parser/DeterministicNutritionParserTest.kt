package `in`.acstechnologies.nutritrainerai.ai.parser

import `in`.acstechnologies.nutritrainerai.ai.ClarificationReason
import `in`.acstechnologies.nutritrainerai.ai.CompactDailySummary
import `in`.acstechnologies.nutritrainerai.ai.InterpretRequest
import `in`.acstechnologies.nutritrainerai.ai.InterpretResult
import `in`.acstechnologies.nutritrainerai.ai.IntentKind
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntent
import `in`.acstechnologies.nutritrainerai.ai.NutritionIntentValidator
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.ai.MealSlot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicNutritionParserTest {

    private val parser = DeterministicNutritionParser()

    private val emptyDay = CompactDailySummary(
        consumedKcal = 0.0,
        consumedProteinG = 0.0,
        mealsLogged = 0,
    )

    private suspend fun parse(utterance: String): NutritionIntent {
        val result = parser.interpret(InterpretRequest(utterance = utterance, dailySummary = emptyDay))
        assertTrue(result is InterpretResult.Success, "expected Success for \"$utterance\", got $result")
        val intent = result.intent
        assertEquals(emptyList(), NutritionIntentValidator.validate(intent), "invalid intent for \"$utterance\"")
        return intent
    }

    @Test
    fun alwaysAvailable() = runTest {
        assertTrue(parser.isAvailable())
        assertEquals("deterministic-parser", parser.id)
    }

    // --- PRD §4.2 / Appendix A examples -------------------------------------

    @Test
    fun consumed_countAndBareFood() = runTest {
        val intent = parse("I had two fulka and dal.")
        assertEquals(IntentKind.CONSUMED, intent.kind)
        assertEquals(2, intent.items.size)
        assertEquals("fulka", intent.items[0].foodName)
        assertEquals(2.0, intent.items[0].quantity?.amount)
        assertEquals("piece", intent.items[0].quantity?.unit)
        assertEquals("dal", intent.items[1].foodName)
        assertNull(intent.items[1].quantity)
        assertTrue(intent.items.all { it.status == MealItemStatus.CONSUMED })
        assertEquals(ConfidenceBand.LOW, intent.overallConfidence)
    }

    @Test
    fun plan_massQuantityIsExact() = runTest {
        val intent = parse("I may have 200 g rice.")
        assertEquals(IntentKind.PLAN, intent.kind)
        assertEquals("rice", intent.items.single().foodName)
        assertEquals(200.0, intent.items.single().quantity?.amount)
        assertEquals("g", intent.items.single().quantity?.unit)
        assertEquals(false, intent.items.single().quantity?.approximate)
        assertEquals(MealItemStatus.PLANNED, intent.items.single().status)
    }

    @Test
    fun correct_withOldValueTail() = runTest {
        val intent = parse("Rice was 120 g, not 200.")
        assertEquals(IntentKind.CORRECT, intent.kind)
        assertEquals("rice", intent.items.single().foodName)
        assertEquals(120.0, intent.items.single().quantity?.amount)
        assertEquals("g", intent.items.single().quantity?.unit)
        assertEquals(ClarificationReason.TARGET_AMBIGUOUS, intent.clarification?.reason)
    }

    @Test
    fun correct_withFinalPrefix() = runTest {
        val intent = parse("Final rice was 120 g.")
        assertEquals(IntentKind.CORRECT, intent.kind)
        assertEquals("rice", intent.items.single().foodName)
        assertEquals(120.0, intent.items.single().quantity?.amount)
    }

    @Test
    fun remove_namesFoodAndAsksWhichEntry() = runTest {
        val intent = parse("Remove the peanuts.")
        assertEquals(IntentKind.REMOVE, intent.kind)
        assertEquals("peanuts", intent.items.single().foodName)
        val clarification = assertNotNull(intent.clarification)
        assertEquals(ClarificationReason.TARGET_AMBIGUOUS, clarification.reason)
    }

    @Test
    fun repeat_hasNoItemsButAClarification() = runTest {
        val intent = parse("Same breakfast as Tuesday.")
        assertEquals(IntentKind.REPEAT, intent.kind)
        assertTrue(intent.items.isEmpty())
        assertNotNull(intent.clarification)
    }

    @Test
    fun ask_isMutationFree() = runTest {
        val intent = parse("Are my carbs too high?")
        assertEquals(IntentKind.ASK, intent.kind)
        assertTrue(intent.items.isEmpty())
        assertNull(intent.clarification)
    }

    @Test
    fun ask_whyQuestion() = runTest {
        assertEquals(IntentKind.ASK, parse("Why did weight rise overnight?").kind)
    }

    @Test
    fun suggest_beatsAskForWhatShouldIEat() = runTest {
        val intent = parse("What should I eat next?")
        assertEquals(IntentKind.SUGGEST, intent.kind)
        assertTrue(intent.items.isEmpty())
    }

    @Test
    fun directiveOnly_isUnknownNotAFood() = runTest {
        val intent = parse("Just estimate it.")
        assertEquals(IntentKind.UNKNOWN, intent.kind)
        val clarification = assertNotNull(intent.clarification)
        assertEquals(ClarificationReason.UTTERANCE_UNCLEAR, clarification.reason)
    }

    // --- quantity / meal parsing -----------------------------------------------

    @Test
    fun householdUnitAndMealSlot() = runTest {
        val intent = parse("I had a bowl of dal for lunch")
        assertEquals(IntentKind.CONSUMED, intent.kind)
        val item = intent.items.single()
        assertEquals("dal", item.foodName)
        assertEquals(1.0, item.quantity?.amount)
        assertEquals("bowl", item.quantity?.unit)
        assertEquals(true, item.quantity?.approximate)
        assertEquals(MealSlot.LUNCH, item.meal)
    }

    @Test
    fun threeItemsSplitOnAnd() = runTest {
        val intent = parse("dal and rice and two roti")
        assertEquals(listOf("dal", "rice", "roti"), intent.items.map { it.foodName })
        assertEquals(2.0, intent.items[2].quantity?.amount)
    }

    @Test
    fun runOnDescription_splitsAtAmountBoundariesIntoSeveralItems() = runTest {
        // no "and"/"," separators — a real run-on log from device testing
        val intent = parse("almond 5 boiled egg 2 white 30 g milk 250")
        assertEquals(IntentKind.CONSUMED, intent.kind)
        assertEquals(4, intent.items.size)
        assertEquals(listOf("almond", "boiled egg", "white", "milk"), intent.items.map { it.foodName })
        assertEquals(30.0, intent.items[2].quantity?.amount)
        assertEquals("g", intent.items[2].quantity?.unit)
        assertEquals(250.0, intent.items[3].quantity?.amount)
    }

    @Test
    fun foodName_isCappedToLastFewWords() = runTest {
        val intent = parse("some cooked spicy homemade paneer butter masala 200 g")
        assertEquals("homemade paneer butter masala", intent.items.single().foodName)
    }

    // --- degenerate input -----------------------------------------------------

    @Test
    fun blankUtterance_isInvalidResult() = runTest {
        assertTrue(parser.interpret(InterpretRequest("   ", dailySummary = emptyDay)) is InterpretResult.Invalid)
    }

    @Test
    fun everyExampleProducesAValidIntent() = runTest {
        val corpus = listOf(
            "I had two fulka and dal.",
            "I may have 200 g rice.",
            "Final rice was 120 g.",
            "Rice was 120 g, not 200.",
            "Same breakfast as Tuesday.",
            "Just estimate it.",
            "Why did weight rise overnight?",
            "Remove the peanuts.",
            "What should I eat next?",
            "I had a bowl of dal for lunch",
        )
        for (line in corpus) {
            val result = parser.interpret(InterpretRequest(line, dailySummary = emptyDay))
            assertTrue(result is InterpretResult.Success, "not Success: $line -> $result")
            assertTrue(
                NutritionIntentValidator.isValid(result.intent),
                "invalid intent for: $line",
            )
        }
    }
}
