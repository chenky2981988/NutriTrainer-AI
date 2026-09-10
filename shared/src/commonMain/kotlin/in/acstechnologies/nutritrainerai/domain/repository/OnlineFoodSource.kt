package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.FoodSearchResult

/**
 * An online catalogue we can query when a food isn't known locally
 * (PRD §7). Never writes anything — results are proposed to the user, who
 * confirms before they enter the local library.
 */
interface OnlineFoodSource {
    val id: String

    /** Best-effort free-text search. Returns an empty list on any failure. */
    suspend fun searchByName(query: String, limit: Int = 8): List<FoodSearchResult>

    /** Exact product by GTIN/EAN barcode, or null. */
    suspend fun lookupByBarcode(barcode: String): FoodSearchResult?
}
