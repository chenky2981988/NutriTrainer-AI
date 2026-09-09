package `in`.acstechnologies.nutritrainerai.data.meallog

import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightMealLogRepositoryTest {

    private fun repo(db: NutriDb = inMemoryNutriDb()) =
        SqlDelightMealLogRepository(db, Dispatchers.Unconfined)

    private fun item(
        id: String,
        day: Long = 100L,
        kcal: Double = 200.0,
        confirmed: Boolean = true,
        status: MealItemStatus = MealItemStatus.CONSUMED,
        created: Long = 1L,
    ) = MealItem(
        id = id,
        dayEpochDay = day,
        status = status,
        confirmed = confirmed,
        foodName = "food-$id",
        quantityGrams = 150.0,
        nutrients = NutrientVector.ofKcal(kcal, proteinG = 10.0, carbohydrateG = 20.0, fatG = 5.0, fibreG = 3.0),
        confidence = ConfidenceBand.MEDIUM,
        source = SourceType.AUTHORITATIVE_DATA,
        revision = 1,
        calculationVersion = 1,
        createdAtEpochMillis = created,
    )

    @Test
    fun upsert_thenGetDay_ordersByCreatedAt() = runTest {
        val r = repo()
        r.upsert(item("b", created = 20))
        r.upsert(item("a", created = 10))
        val day = r.getDay(100)
        assertEquals(listOf("a", "b"), day.map { it.id })
        assertEquals(200.0, day.first().nutrients.energyKcal)
    }

    @Test
    fun upsert_sameId_replacesNotDuplicates() = runTest {
        val r = repo()
        r.upsert(item("x", kcal = 200.0))
        r.upsert(item("x", kcal = 350.0))
        val day = r.getDay(100)
        assertEquals(1, day.size)
        assertEquals(350.0, day.single().nutrients.energyKcal)
    }

    @Test
    fun softDelete_removesFromDayViews() = runTest {
        val r = repo()
        r.upsert(item("x"))
        r.softDelete("x", atEpochMillis = 999)
        assertEquals(emptyList(), r.getDay(100))
    }

    @Test
    fun observeDay_emitsCurrentState() = runTest {
        val r = repo()
        r.upsert(item("x"))
        assertEquals(listOf("x"), r.observeDay(100).first().map { it.id })
    }

    @Test
    fun mapper_roundTripsEnumsAndNutrients() = runTest {
        val r = repo()
        val original = item("x").copy(
            status = MealItemStatus.PLANNED,
            confirmed = false,
            confidence = ConfidenceBand.UNRESOLVED,
            source = SourceType.USER_CONFIRMED,
        )
        r.upsert(original)
        assertEquals(original, r.getDay(100).single())
    }

    @Test
    fun getDay_isScopedToTheDay() = runTest {
        val r = repo()
        r.upsert(item("x", day = 100))
        r.upsert(item("y", day = 101))
        assertEquals(listOf("x"), r.getDay(100).map { it.id })
    }
}
