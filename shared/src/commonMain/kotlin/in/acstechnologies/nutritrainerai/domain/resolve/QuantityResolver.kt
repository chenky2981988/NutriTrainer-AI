package `in`.acstechnologies.nutritrainerai.domain.resolve

/**
 * Turns a spoken amount + unit into grams, using (in order) per-food household
 * overrides, the resolved food's own unit weights, then a generic fallback table
 * (PRD §5 "Household calibration"). Returns `null` when the unit is unrecognised.
 *
 * Takes primitives, not the AI's `ParsedQuantity`, so the domain stays free of
 * the `ai` layer. Volume in millilitres is treated as grams for the seed foods;
 * real density handling arrives with the catalogue.
 */
class QuantityResolver {

    fun toGrams(
        amount: Double?,
        unit: String?,
        resolved: ResolvedFood?,
        householdOverrides: Map<String, Double> = emptyMap(),
    ): Double? {
        if (amount == null) {
            // No amount spoken: assume one typical serving (PRD "medium regional range").
            return resolved?.pieceGrams ?: DEFAULT_SERVING_GRAMS
        }
        if (!amount.isFinite() || amount < 0.0) return null

        return when (val u = unit?.trim()?.lowercase().orEmpty()) {
            "", "serving", "portion" -> amount * (resolved?.pieceGrams ?: DEFAULT_SERVING_GRAMS)
            "g", "gram", "grams", "gm" -> amount
            "kg", "kilogram", "kilograms" -> amount * 1_000.0
            "ml", "millilitre", "millilitres" -> amount
            "l", "litre", "litres" -> amount * 1_000.0
            "piece", "pieces", "slice", "slices", "pc" -> amount * (resolved?.pieceGrams ?: DEFAULT_PIECE_GRAMS)
            else -> {
                val perUnit = householdOverrides[u]
                    ?: resolved?.householdUnitGrams?.get(u)
                    ?: HOUSEHOLD_UNIT_GRAMS[u]
                perUnit?.let { amount * it }
            }
        }
    }

    private companion object {
        const val DEFAULT_SERVING_GRAMS = 150.0
        const val DEFAULT_PIECE_GRAMS = 40.0

        val HOUSEHOLD_UNIT_GRAMS: Map<String, Double> = mapOf(
            "katori" to 150.0,
            "bowl" to 200.0,
            "cup" to 240.0,
            "glass" to 250.0,
            "plate" to 300.0,
            "tbsp" to 15.0,
            "tsp" to 5.0,
            "handful" to 30.0,
            "scoop" to 30.0,
        )
    }
}
