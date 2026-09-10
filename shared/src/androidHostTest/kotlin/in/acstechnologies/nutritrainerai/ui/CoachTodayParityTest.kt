package `in`.acstechnologies.nutritrainerai.ui

import `in`.acstechnologies.nutritrainerai.ai.parser.DeterministicNutritionParser
import `in`.acstechnologies.nutritrainerai.ai.pipeline.LogParsedIntentUseCase
import `in`.acstechnologies.nutritrainerai.data.coach.SqlDelightCoachTranscriptRepository
import `in`.acstechnologies.nutritrainerai.data.food.SqlDelightFoodRepository
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.data.meallog.SqlDelightMealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.resolve.CompositeFoodResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.QuantityResolver
import `in`.acstechnologies.nutritrainerai.domain.model.FoodSearchResult
import `in`.acstechnologies.nutritrainerai.domain.repository.OnlineFoodSource
import `in`.acstechnologies.nutritrainerai.domain.resolve.SeedFoodResolver
import `in`.acstechnologies.nutritrainerai.domain.usecase.AddUserFoodUseCase
import `in`.acstechnologies.nutritrainerai.domain.usecase.ObserveDayUseCase
import `in`.acstechnologies.nutritrainerai.ui.coach.CoachIntent
import `in`.acstechnologies.nutritrainerai.ui.coach.CoachViewModel
import `in`.acstechnologies.nutritrainerai.ui.today.TodayViewModel
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

/**
 * The meal Definition of Done: **Coach and Today totals match** (PRD §2A, §12).
 * Both ViewModels derive from one [ObserveDayUseCase] stream over one repository,
 * so any log/correction shows up identically in both.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoachTodayParityTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class World {
        val day = 20_000L
        val db = inMemoryNutriDb()
        val mealLog = SqlDelightMealLogRepository(db, Dispatchers.Unconfined)
        val foods = SqlDelightFoodRepository(db, Dispatchers.Unconfined)
        val observe = ObserveDayUseCase(mealLog)
        private var seq = 0
        val quantities = QuantityResolver()
        val resolver = CompositeFoodResolver(foods, SeedFoodResolver())
        val log = LogParsedIntentUseCase(
            mealLog = mealLog,
            foods = resolver,
            quantities = quantities,
            now = { 1_000L },
            idFactory = { "id-${seq++}" },
        )
        val addUserFood = AddUserFoodUseCase(
            foods = foods,
            mealLog = mealLog,
            quantities = quantities,
            now = { 1_000L },
            idFactory = { "uf-${seq++}" },
        )
        val noOnline = object : OnlineFoodSource {
            override val id = "none"
            override suspend fun searchByName(query: String, limit: Int, countryCode: String?): List<FoodSearchResult> = emptyList()
            override suspend fun lookupByBarcode(barcode: String): FoodSearchResult? = null
        }
        val transcript = SqlDelightCoachTranscriptRepository(db, Dispatchers.Unconfined)
        val coach = CoachViewModel(
            DeterministicNutritionParser(), log, addUserFood, noOnline, transcript, observe, day, now = { 1_000L },
        )
        val today = TodayViewModel(observe, mealLog, day, now = { 1_000L })
    }

    private fun rice(grams: Double) =
        NutritionMath.ingredientNutrients(SeedFoodResolver.RICE.nutrientsPerBase, grams)

    private fun dal(grams: Double) =
        NutritionMath.ingredientNutrients(SeedFoodResolver.DAL.nutrientsPerBase, grams)

    private fun CoachViewModel.say(text: String) {
        onIntent(CoachIntent.ComposerChanged(text))
        onIntent(CoachIntent.Submit)
    }

    @Test
    fun coachLog_appearsIdenticallyInTodayAndCoach() = runTest {
        val w = World()
        w.coach.say("I had 150 g rice and 50 g dal")
        advanceUntilIdle()

        val expected = rice(150.0) + dal(50.0)
        assertEquals(expected, w.today.state.value.totals.consumed)
        assertEquals(w.today.state.value.totals.consumed, w.coach.state.value.dayTotals.consumed)
        assertEquals(2, w.today.state.value.items.size)
    }

    @Test
    fun correction_replacesWithoutDuplicating_bothViewsStayInSync() = runTest {
        val w = World()
        w.coach.say("I had 150 g rice")
        advanceUntilIdle()
        w.coach.say("rice was 200 g")
        advanceUntilIdle()

        assertEquals(1, w.mealLog.getDay(w.day).size)
        assertEquals(rice(200.0), w.coach.state.value.dayTotals.consumed)
        assertEquals(w.coach.state.value.dayTotals.consumed, w.today.state.value.totals.consumed)
    }

    @Test
    fun plannedItemsNeverInflateConsumed_inEitherView() = runTest {
        val w = World()
        w.coach.say("I may have 100 g rice")
        advanceUntilIdle()

        assertEquals(NutrientVector.ZERO, w.today.state.value.totals.consumed)
        assertEquals(NutrientVector.ZERO, w.coach.state.value.dayTotals.consumed)
        assertEquals(1, w.today.state.value.totals.plannedItemCount)
        assertEquals(rice(100.0), w.today.state.value.totals.planned)
    }
}
