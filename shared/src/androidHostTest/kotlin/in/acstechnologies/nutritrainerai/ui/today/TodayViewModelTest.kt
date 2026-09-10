package `in`.acstechnologies.nutritrainerai.ui.today

import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.data.meallog.SqlDelightMealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import `in`.acstechnologies.nutritrainerai.domain.usecase.ObserveDayUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val day = 20_000L

    private fun item(id: String) = MealItem(
        id = id,
        dayEpochDay = day,
        status = MealItemStatus.CONSUMED,
        confirmed = true,
        foodName = "food-$id",
        quantityGrams = 100.0,
        nutrients = NutrientVector.ofKcal(200.0, proteinG = 10.0, carbohydrateG = 20.0, fatG = 5.0, fibreG = 2.0),
        confidence = ConfidenceBand.MEDIUM,
        source = SourceType.AUTHORITATIVE_DATA,
        revision = 1,
        calculationVersion = 1,
        createdAtEpochMillis = 1L,
    )

    private fun vm(): Pair<TodayViewModel, SqlDelightMealLogRepository> {
        val repo = SqlDelightMealLogRepository(inMemoryNutriDb(), Dispatchers.Unconfined)
        return TodayViewModel(ObserveDayUseCase(repo), repo, day, now = { 999L }) to repo
    }

    @Test
    fun delete_removesItemAndDropsConsumedTotal() = runTest {
        val (today, repo) = vm()
        repo.upsert(item("a"))
        repo.upsert(item("b"))
        advanceUntilIdle()
        assertEquals(2, today.state.value.items.size)
        assertEquals(400.0, today.state.value.totals.consumed.energyKcal)

        today.onIntent(TodayIntent.Delete("a"))
        advanceUntilIdle()

        assertEquals(listOf("b"), today.state.value.items.map { it.id })
        assertEquals(200.0, today.state.value.totals.consumed.energyKcal)
    }

    @Test
    fun undoDelete_bringsTheItemBack() = runTest {
        val (today, repo) = vm()
        repo.upsert(item("a"))
        advanceUntilIdle()

        today.onIntent(TodayIntent.Delete("a"))
        advanceUntilIdle()
        assertEquals(emptyList(), today.state.value.items)

        today.onIntent(TodayIntent.UndoDelete("a"))
        advanceUntilIdle()
        assertEquals(listOf("a"), today.state.value.items.map { it.id })
    }
}
