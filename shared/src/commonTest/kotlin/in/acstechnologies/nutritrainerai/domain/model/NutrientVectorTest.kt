package `in`.acstechnologies.nutritrainerai.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class NutrientVectorTest {

    private val a = NutrientVector(100.0, 418.0, 5.0, 10.0, 2.0, 1.0)
    private val b = NutrientVector(50.0, 209.0, 3.0, 7.0, 1.0, 0.5)

    @Test
    fun plus_addsComponentwise() {
        assertEquals(NutrientVector(150.0, 627.0, 8.0, 17.0, 3.0, 1.5), a + b)
    }

    @Test
    fun times_scalesComponentwise() {
        assertEquals(NutrientVector(200.0, 836.0, 10.0, 20.0, 4.0, 2.0), a * 2.0)
    }

    @Test
    fun times_byZero_isZeroVector() {
        assertEquals(NutrientVector.ZERO, a * 0.0)
    }

    @Test
    fun sum_ofEmpty_isZero() {
        assertEquals(NutrientVector.ZERO, emptyList<NutrientVector>().sum())
    }

    @Test
    fun sum_ofList_folds() {
        assertEquals(
            NutrientVector(250.0, 1045.0, 13.0, 27.0, 5.0, 2.5),
            listOf(a, b, a).sum(),
        )
    }

    @Test
    fun ofKcal_derivesKjWithThermochemicalFactor() {
        val v = NutrientVector.ofKcal(energyKcal = 100.0, proteinG = 5.0, carbohydrateG = 10.0, fatG = 2.0, fibreG = 1.0)
        assertEquals(418.4, v.energyKj, 1e-9)
    }
}
