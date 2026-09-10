package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.FoodSearchResult

/**
 * An online catalogue we can query when a food isn't known locally
 * (PRD §7). Never writes anything — results are proposed to the user, who
 * confirms before they enter the local library.
 */
interface OnlineFoodSource {
    val id: String

    /**
     * Best-effort free-text search. Returns an empty list on any failure.
     *
     * [countryCode] is an ISO-3166 alpha-2 code (e.g. `"in"`) used to bias
     * results toward products sold in the user's region — an Indian user
     * searching "cow milk" should see Amul / Chitale / Gokul first. `null`
     * lets the implementation fall back to its own default.
     */
    suspend fun searchByName(
        query: String,
        limit: Int = 8,
        countryCode: String? = null,
    ): List<FoodSearchResult>

    /** Exact product by GTIN/EAN barcode, or null. */
    suspend fun lookupByBarcode(barcode: String): FoodSearchResult?
}
