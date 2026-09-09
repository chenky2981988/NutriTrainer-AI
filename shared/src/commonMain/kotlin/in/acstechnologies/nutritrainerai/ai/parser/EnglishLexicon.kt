package `in`.acstechnologies.nutritrainerai.ai.parser

/**
 * The word lists the [DeterministicNutritionParser] matches against. Kept
 * separate from the parsing logic so regional lexicons (Hindi, Tamil,
 * transliteration…) can be added later as sibling objects without touching the
 * pipeline (PRD §4.1 "Search matches English, local script and transliteration").
 *
 * English only, deliberately small. This is the fallback engine, not the
 * primary one (PRD §8 engine strategy).
 */
internal object EnglishLexicon {

    /** Phrases removed from the front of an utterance before item extraction. */
    val LEADING_NOISE: List<String> = listOf(
        "i just had", "i have just had", "i had", "i ate", "i've had", "ive had",
        "i just ate", "i am having", "i'm having", "im having", "i had some",
        "i may have", "i might have", "i may eat", "i might eat", "i plan to eat",
        "i plan to have", "i'm planning to eat", "im planning to eat",
        "i am going to eat", "i'm going to eat", "im going to eat", "going to eat",
        "i will have", "i'll have", "ill have", "planning to eat",
        "final", "actually", "correction", "i meant", "just log",
        "just estimate it", "just estimate", "estimate it", "just guess it",
        "just guess", "best guess",
        "remove the", "remove", "delete the", "delete", "take out the", "take out",
        "please",
    )

    val PLAN_MARKERS: List<String> = listOf(
        "i may ", "i might ", "may have", "might have", "i plan to", "planning to",
        "going to eat", "i will have", "i'll have", "ill have", "thinking of eating",
        "later i", "for dinner i might", "i intend to",
    )

    val REMOVE_MARKERS: List<String> = listOf(
        "remove ", "delete ", "take out ", "get rid of ", "undo the",
    )

    val REPEAT_MARKERS: List<String> = listOf(
        "same as", "same breakfast", "same lunch", "same dinner", "same snack",
        "same meal", "repeat ", "again like", "like yesterday", "as yesterday",
    )

    val SUGGEST_MARKERS: List<String> = listOf(
        "what should i eat", "what can i eat", "what to eat", "suggest ",
        "recommend ", "give me a meal", "what's for", "whats for", "next meal",
        "what should i have",
    )

    /** Detects a "<food> was <number>" style correction. */
    val CORRECTION_REGEX = Regex("""\b[a-z]+\s+(was|wasn't|weren't|should be|is actually)\s+\d""")
    val CORRECTION_WORDS: List<String> = listOf(
        "actually", "correction", "i meant", " not ", "change it to", "make it",
        "should be", "instead of",
    )

    val QUESTION_STARTERS: List<String> = listOf(
        "why", "how", "what", "is", "are", "am", "do", "does", "did", "can",
        "could", "should", "when", "which", "was",
    )

    /** number word -> value; "a"/"an" count as one, hedges map to typical counts. */
    val NUMBER_WORDS: Map<String, Double> = mapOf(
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0,
        "four" to 4.0, "five" to 5.0, "six" to 6.0, "seven" to 7.0, "eight" to 8.0,
        "nine" to 9.0, "ten" to 10.0, "eleven" to 11.0, "twelve" to 12.0,
        "half" to 0.5, "quarter" to 0.25, "couple" to 2.0, "few" to 3.0,
        "dozen" to 12.0,
    )

    /** unit token (singular, lower-case) -> canonical unit string for [ParsedQuantity]. */
    val UNIT_TOKENS: Map<String, String> = buildMap {
        listOf("g", "gram", "grams", "gm", "gms").forEach { put(it, "g") }
        listOf("kg", "kilogram", "kilograms", "kilo", "kilos").forEach { put(it, "kg") }
        listOf("ml", "millilitre", "millilitres", "milliliter", "milliliters").forEach { put(it, "ml") }
        listOf("l", "litre", "litres", "liter", "liters").forEach { put(it, "l") }
        listOf("katori", "katoris").forEach { put(it, "katori") }
        listOf("bowl", "bowls").forEach { put(it, "bowl") }
        listOf("cup", "cups").forEach { put(it, "cup") }
        listOf("glass", "glasses").forEach { put(it, "glass") }
        listOf("plate", "plates").forEach { put(it, "plate") }
        listOf("tbsp", "tablespoon", "tablespoons").forEach { put(it, "tbsp") }
        listOf("tsp", "teaspoon", "teaspoons").forEach { put(it, "tsp") }
        listOf("piece", "pieces", "pc", "pcs").forEach { put(it, "piece") }
        listOf("slice", "slices").forEach { put(it, "slice") }
        listOf("handful", "handfuls").forEach { put(it, "handful") }
        listOf("scoop", "scoops").forEach { put(it, "scoop") }
        listOf("serving", "servings", "portion", "portions").forEach { put(it, "serving") }
    }

    /** Dropped from a food name after quantity/unit are pulled out. */
    val FOOD_STOPWORDS: Set<String> = setOf(
        "of", "some", "the", "a", "an", "and", "with", "plus", "my", "then",
        "also", "for", "to", "had", "ate", "eat", "eaten", "was", "were",
        "it", "just", "only", "about", "around", "approximately",
    )

    /** Fragment separators inside a single utterance. */
    val ITEM_SEPARATORS: Regex = Regex("""\s*(?:,|;|\band\b|\bwith\b|\bplus\b|\+)\s*""")

    val MEAL_WORDS: Map<String, String> = mapOf(
        "breakfast" to "BREAKFAST", "lunch" to "LUNCH", "dinner" to "DINNER",
        "snack" to "SNACK", "brunch" to "LUNCH", "supper" to "DINNER",
    )
}
