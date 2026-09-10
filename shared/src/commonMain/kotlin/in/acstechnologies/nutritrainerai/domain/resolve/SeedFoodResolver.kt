package `in`.acstechnologies.nutritrainerai.domain.resolve

import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType

/**
 * A tiny hard-coded food table so the Coach→Today loop works before the real
 * catalogue / content pipeline exists (PRD §7). Every entry is
 * [SourceType.CURATED_REGIONAL] + [ConfidenceBand.LOW] — it is a seed, not
 * authoritative data. Values are per 100 g as eaten (per 100 ml for milk).
 */
class SeedFoodResolver : FoodResolver {

    override fun resolve(foodName: String): ResolvedFood? {
        val key = foodName.trim().lowercase()
        return TABLE[key] ?: TABLE[ALIASES[key]]
    }

    companion object {
        private fun perG(
            name: String,
            kcal: Double,
            protein: Double,
            carb: Double,
            fat: Double,
            fibre: Double,
            pieceGrams: Double? = null,
            basis: MeasurementBasis = MeasurementBasis.PER_100_G,
            household: Map<String, Double> = emptyMap(),
        ) = ResolvedFood(
            canonicalName = name,
            nutrientsPerBase = NutrientVector.ofKcal(kcal, protein, carb, fat, fibre),
            basis = basis,
            source = SourceType.CURATED_REGIONAL,
            confidence = ConfidenceBand.LOW,
            pieceGrams = pieceGrams,
            householdUnitGrams = household,
        )

        // Reference nutrients per 100 g / 100 ml, as eaten. Rough seed values.
        val RICE = perG("rice", 130.0, 2.7, 28.0, 0.3, 0.4, household = mapOf("katori" to 150.0, "bowl" to 200.0))
        val DAL = perG("dal", 116.0, 7.0, 20.0, 0.4, 3.0, household = mapOf("katori" to 150.0, "bowl" to 200.0))
        val ROTI = perG("roti", 297.0, 8.0, 46.0, 7.0, 5.0, pieceGrams = 35.0)
        val EGG = perG("egg", 155.0, 13.0, 1.1, 11.0, 0.0, pieceGrams = 50.0)
        val MILK = perG("milk", 62.0, 3.2, 4.8, 3.3, 0.0, basis = MeasurementBasis.PER_100_ML, household = mapOf("glass" to 250.0))
        val CURD = perG("curd", 60.0, 3.5, 4.7, 3.3, 0.0, household = mapOf("katori" to 150.0))
        val PANEER = perG("paneer", 265.0, 18.0, 3.0, 20.0, 0.0)
        val CHICKEN = perG("chicken", 165.0, 31.0, 0.0, 3.6, 0.0)
        val BANANA = perG("banana", 89.0, 1.1, 23.0, 0.3, 2.6, pieceGrams = 120.0)
        val IDLI = perG("idli", 130.0, 3.5, 27.0, 0.5, 1.0, pieceGrams = 40.0)
        val DOSA = perG("dosa", 168.0, 3.9, 29.0, 3.7, 1.2, pieceGrams = 80.0)

        private val TABLE: Map<String, ResolvedFood> = listOf(
            RICE, DAL, ROTI, EGG, MILK, CURD, PANEER, CHICKEN, BANANA, IDLI, DOSA,
        ).associateBy { it.canonicalName }

        private val ALIASES: Map<String, String> = mapOf(
            "rice" to "rice", "cooked rice" to "rice", "boiled rice" to "rice", "steamed rice" to "rice",
            "daal" to "dal", "dahl" to "dal", "dal fry" to "dal", "toor dal" to "dal", "lentils" to "dal",
            "fulka" to "roti", "phulka" to "roti", "chapati" to "roti", "chapathi" to "roti", "rotli" to "roti",
            "eggs" to "egg", "boiled egg" to "egg", "boiled eggs" to "egg",
            "dahi" to "curd", "yogurt" to "curd", "yoghurt" to "curd",
            "chicken curry" to "chicken", "chicken breast" to "chicken",
            "idlis" to "idli", "dosai" to "dosa", "plain dosa" to "dosa",
        )
    }
}
