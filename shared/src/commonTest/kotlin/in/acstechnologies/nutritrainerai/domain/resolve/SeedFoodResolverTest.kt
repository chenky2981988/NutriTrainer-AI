package `in`.acstechnologies.nutritrainerai.domain.resolve

import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SeedFoodResolverTest {

    private val resolver = SeedFoodResolver()

    @Test
    fun resolvesCanonicalNames() = runTest {
        assertEquals("rice", resolver.resolve("rice")?.canonicalName)
        assertEquals("dal", resolver.resolve("dal")?.canonicalName)
    }

    @Test
    fun resolvesAliasesCaseAndWhitespaceInsensitive() = runTest {
        assertEquals("roti", resolver.resolve("Fulka")?.canonicalName)
        assertEquals("roti", resolver.resolve("  CHAPATI ")?.canonicalName)
        assertEquals("dal", resolver.resolve("daal")?.canonicalName)
        assertEquals("curd", resolver.resolve("yoghurt")?.canonicalName)
    }

    @Test
    fun unknownFoodIsNull() = runTest {
        assertNull(resolver.resolve("unicorn steak"))
    }

    @Test
    fun seedEntriesAreLowConfidenceCuratedRegional() = runTest {
        val rice = resolver.resolve("rice")!!
        assertEquals(SourceType.CURATED_REGIONAL, rice.source)
        assertEquals(ConfidenceBand.LOW, rice.confidence)
        assertEquals(130.0, rice.nutrientsPerBase.energyKcal)
        assertEquals(130.0 * 4.184, rice.nutrientsPerBase.energyKj, 1e-9)
    }
}
