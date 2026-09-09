package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * A bundle of nutrient amounts for some quantity of food.
 *
 * `kcal` and `kJ` are stored **separately and never derived from each other**
 * (PRD §6 "Data quality"): source data may publish only one, and silently
 * computing the other would fabricate precision. Use [ofKcal] only when the
 * source genuinely gives just calories.
 *
 * All amounts are for whatever quantity this vector describes (per 100 g, a
 * serving, a whole day…). Scaling is the caller's job via [times].
 */
@Serializable
data class NutrientVector(
    val energyKcal: Double,
    val energyKj: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val fibreG: Double,
) {
    operator fun plus(other: NutrientVector): NutrientVector =
        NutrientVector(
            energyKcal = energyKcal + other.energyKcal,
            energyKj = energyKj + other.energyKj,
            proteinG = proteinG + other.proteinG,
            carbohydrateG = carbohydrateG + other.carbohydrateG,
            fatG = fatG + other.fatG,
            fibreG = fibreG + other.fibreG,
        )

    operator fun times(factor: Double): NutrientVector {
        require(factor.isFinite() && factor >= 0.0) { "factor must be finite and >= 0, was $factor" }
        return NutrientVector(
            energyKcal = energyKcal * factor,
            energyKj = energyKj * factor,
            proteinG = proteinG * factor,
            carbohydrateG = carbohydrateG * factor,
            fatG = fatG * factor,
            fibreG = fibreG * factor,
        )
    }

    companion object {
        val ZERO = NutrientVector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        /** 1 kcal = 4.184 kJ (thermochemical). */
        const val KJ_PER_KCAL: Double = 4.184

        /**
         * Build a vector when the source publishes only calories; kJ is filled
         * with the thermochemical conversion. Do not use when the label states
         * kJ directly — keep the label's own value instead.
         */
        fun ofKcal(
            energyKcal: Double,
            proteinG: Double,
            carbohydrateG: Double,
            fatG: Double,
            fibreG: Double,
        ): NutrientVector =
            NutrientVector(
                energyKcal = energyKcal,
                energyKj = energyKcal * KJ_PER_KCAL,
                proteinG = proteinG,
                carbohydrateG = carbohydrateG,
                fatG = fatG,
                fibreG = fibreG,
            )
    }
}

/** Sum of a collection of vectors; empty collections yield [NutrientVector.ZERO]. */
fun Iterable<NutrientVector>.sum(): NutrientVector =
    fold(NutrientVector.ZERO, NutrientVector::plus)
