package `in`.acstechnologies.nutritrainerai.domain.region

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndiaRegionCatalogTest {

    @Test
    fun covers28StatesAnd8UnionTerritories() {
        assertEquals(36, IndiaRegionCatalog.entries.size)
        assertEquals(28, IndiaRegionCatalog.states.size)
        assertEquals(8, IndiaRegionCatalog.unionTerritories.size)
    }

    @Test
    fun codesAreUnique() {
        val codes = IndiaRegionCatalog.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun everyZoneIsPresentAndNonEmpty_inPrdOrder() {
        val grouped = IndiaRegionCatalog.grouped()
        assertEquals(IndiaZone.entries, grouped.keys.toList())
        assertTrue(grouped.values.all { it.isNotEmpty() })
        assertEquals(IndiaZone.NORTH, grouped.keys.first())
    }

    @Test
    fun search_matchesNameCaseInsensitively() {
        assertTrue(IndiaRegionCatalog.search("tamil").any { it.code == "IN-TN" })
        assertTrue(IndiaRegionCatalog.search("KERALA").any { it.code == "IN-KL" })
    }

    @Test
    fun search_matchesAliasesAndCodes() {
        assertTrue(IndiaRegionCatalog.search("bengal").any { it.code == "IN-WB" })
        assertTrue(IndiaRegionCatalog.search("NCT").any { it.code == "IN-DL" })
        assertTrue(IndiaRegionCatalog.search("IN-KA").any { it.code == "IN-KA" })
    }

    @Test
    fun blankQueryReturnsEverything_unknownReturnsNothing() {
        assertEquals(36, IndiaRegionCatalog.search("   ").size)
        assertEquals(emptyList(), IndiaRegionCatalog.search("atlantis"))
    }

    @Test
    fun byCode_resolves() {
        assertEquals("Kerala", IndiaRegionCatalog.byCode("IN-KL")?.name)
        assertEquals(null, IndiaRegionCatalog.byCode("IN-ZZ"))
    }
}
