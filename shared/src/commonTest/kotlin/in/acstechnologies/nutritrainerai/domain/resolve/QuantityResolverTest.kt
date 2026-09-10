package `in`.acstechnologies.nutritrainerai.domain.resolve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuantityResolverTest {

    private val resolver = QuantityResolver()

    @Test
    fun massUnitsConvertToGrams() {
        assertEquals(150.0, resolver.toGrams(150.0, "g", null))
        assertEquals(2_000.0, resolver.toGrams(2.0, "kg", null))
    }

    @Test
    fun volumeUnitsTreatedAsGrams() {
        assertEquals(250.0, resolver.toGrams(250.0, "ml", null))
        assertEquals(1_000.0, resolver.toGrams(1.0, "l", null))
    }

    @Test
    fun piecesUseResolvedPieceGrams() {
        assertEquals(70.0, resolver.toGrams(2.0, "piece", SeedFoodResolver.ROTI)) // 2 x 35 g
    }

    @Test
    fun householdUnitPrefersFoodSpecificThenFallback() {
        assertEquals(150.0, resolver.toGrams(1.0, "katori", SeedFoodResolver.RICE)) // food-specific
        assertEquals(200.0, resolver.toGrams(1.0, "bowl", null)) // generic fallback table
    }

    @Test
    fun perFoodOverrideBeatsEverything() {
        assertEquals(
            180.0,
            resolver.toGrams(1.0, "katori", SeedFoodResolver.RICE, householdOverrides = mapOf("katori" to 180.0)),
        )
    }

    @Test
    fun noAmountAssumesOneServing() {
        assertEquals(35.0, resolver.toGrams(null, null, SeedFoodResolver.ROTI)) // pieceGrams
        assertEquals(150.0, resolver.toGrams(null, null, null)) // default serving
    }

    @Test
    fun unknownUnitOrNegativeAmountIsNull() {
        assertNull(resolver.toGrams(1.0, "furlong", null))
        assertNull(resolver.toGrams(-5.0, "g", null))
    }
}
