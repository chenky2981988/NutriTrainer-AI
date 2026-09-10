package `in`.acstechnologies.nutritrainerai.data.food

import `in`.acstechnologies.nutritrainerai.domain.model.FoodSearchResult
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import `in`.acstechnologies.nutritrainerai.domain.repository.OnlineFoodSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Open Food Facts client (PRD §7). Read-only, ODbL-licensed data — the UI shows
 * a "via Open Food Facts" attribution and every result carries its product URL.
 * Requests identify the app and ask only for the fields we use.
 */
class OpenFoodFactsSource(
    private val client: HttpClient,
    /** Region used when a call doesn't pass one. The app is India-first (PRD §1). */
    private val defaultCountryCode: String? = "in",
) : OnlineFoodSource {

    override val id: String = "open-food-facts"

    override suspend fun searchByName(query: String, limit: Int, countryCode: String?): List<FoodSearchResult> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        // OFF ranks by the country subdomain: in.openfoodfacts.org surfaces
        // products actually sold in India ahead of global ones.
        val cc = (countryCode ?: defaultCountryCode)?.trim()?.lowercase()?.takeIf { it.length == 2 }
        val host = cc?.let { "https://$it.openfoodfacts.org" } ?: WORLD
        return runCatching {
            val resp: OffSearchResponse = client.get("$host/cgi/search.pl") {
                parameter("search_terms", q)
                parameter("search_simple", 1)
                parameter("action", "process")
                parameter("json", 1)
                parameter("page_size", limit)
                parameter("fields", FIELDS)
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.body()
            resp.products.mapNotNull { it.toResult() }
        }.getOrDefault(emptyList())
    }

    override suspend fun lookupByBarcode(barcode: String): FoodSearchResult? {
        val code = barcode.trim()
        if (code.isBlank()) return null
        return runCatching {
            val resp: OffProductResponse = client.get("$WORLD/api/v2/product/$code.json") {
                parameter("fields", FIELDS)
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.body()
            resp.product?.toResult()
        }.getOrNull()
    }

    private companion object {
        const val WORLD = "https://world.openfoodfacts.org"
        const val USER_AGENT = "NutriTrainerAI/0.1 (offline-first nutrition app)"
        const val FIELDS = "code,product_name,brands,quantity,serving_quantity,nutriments"
    }
}

// --- wire format ---------------------------------------------------------------

@Serializable
internal data class OffSearchResponse(val products: List<OffProduct> = emptyList())

@Serializable
internal data class OffProductResponse(val product: OffProduct? = null, val status: Int = 0)

@Serializable
internal data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("serving_quantity") val servingQuantity: JsonPrimitive? = null,
    val nutriments: OffNutriments? = null,
)

@Serializable
internal data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    @SerialName("fiber_100g") val fiber100g: Double? = null,
)

internal fun OffProduct.toResult(): FoodSearchResult? {
    val cleanName = productName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val n = nutriments ?: return null
    val kcal = n.energyKcal100g ?: return null

    return FoodSearchResult(
        name = cleanName,
        brand = brands?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
        barcode = code,
        nutrientsPerBase = NutrientVector.ofKcal(
            energyKcal = kcal,
            proteinG = n.proteins100g ?: 0.0,
            carbohydrateG = n.carbohydrates100g ?: 0.0,
            fatG = n.fat100g ?: 0.0,
            fibreG = n.fiber100g ?: 0.0,
        ),
        basis = MeasurementBasis.PER_100_G,
        servingGrams = servingQuantity?.doubleOrNull,
        source = SourceType.OPEN_COMMUNITY,
        provenanceUrl = code?.let { "https://world.openfoodfacts.org/product/$it" },
    )
}
