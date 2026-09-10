package `in`.acstechnologies.nutritrainerai.data.food

import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenFoodFactsSourceTest {

    private fun source(body: String, status: Int = 200): OpenFoodFactsSource {
        val engine = MockEngine {
            respond(
                content = body,
                status = io.ktor.http.HttpStatusCode.fromValue(status),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return OpenFoodFactsSource(client)
    }

    @Test
    fun search_mapsProductsToResults() = runTest {
        val json = """
        {"products":[
          {"code":"8901234","product_name":"Provilac Full Cream Milk","brands":"Provilac, Dairy",
           "serving_quantity":200,
           "nutriments":{"energy-kcal_100g":63.0,"proteins_100g":3.3,"carbohydrates_100g":4.7,"fat_100g":3.5,"fiber_100g":0.0}},
          {"product_name":"","nutriments":{"energy-kcal_100g":10.0}},
          {"product_name":"No nutriments here"}
        ]}
        """.trimIndent()

        val results = source(json).searchByName("provilac milk")
        assertEquals(1, results.size)
        val r = results.single()
        assertEquals("Provilac Full Cream Milk", r.name)
        assertEquals("Provilac", r.brand) // first brand only
        assertEquals("8901234", r.barcode)
        assertEquals(63.0, r.nutrientsPerBase.energyKcal)
        assertEquals(3.3, r.nutrientsPerBase.proteinG)
        assertEquals(MeasurementBasis.PER_100_G, r.basis)
        assertEquals(200.0, r.servingGrams)
        assertEquals(SourceType.OPEN_COMMUNITY, r.source)
        assertTrue(r.provenanceUrl!!.endsWith("/product/8901234"))
    }

    @Test
    fun lookupByBarcode_returnsProduct() = runTest {
        val json = """
        {"status":1,"product":{"code":"111","product_name":"Amul Paneer","brands":"Amul",
          "nutriments":{"energy-kcal_100g":296.0,"proteins_100g":20.0,"fat_100g":22.0}}}
        """.trimIndent()
        val r = source(json).lookupByBarcode("111")!!
        assertEquals("Amul Paneer", r.name)
        assertEquals(296.0, r.nutrientsPerBase.energyKcal)
        assertEquals(0.0, r.nutrientsPerBase.carbohydrateG) // missing -> 0
    }

    @Test
    fun blankOrErrorInputs_areSafe() = runTest {
        assertEquals(emptyList(), source("{}").searchByName("   "))
        assertNull(source("nonsense", status = 500).lookupByBarcode("999"))
        assertEquals(emptyList(), source("<html>oops</html>", status = 502).searchByName("milk"))
    }
}
