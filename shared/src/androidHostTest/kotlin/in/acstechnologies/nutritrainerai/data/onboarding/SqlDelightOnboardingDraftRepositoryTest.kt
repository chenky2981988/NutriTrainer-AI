package `in`.acstechnologies.nutritrainerai.data.onboarding

import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightOnboardingDraftRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun repo(db: NutriDb = inMemoryNutriDb()) =
        SqlDelightOnboardingDraftRepository(db, json, Dispatchers.Unconfined)

    @Test
    fun load_onEmpty_isNull() = runTest {
        assertNull(repo().load())
    }

    @Test
    fun save_thenLoad_roundTrips() = runTest {
        val r = repo()
        val draft = OnboardingState(
            currentStepId = "OB-05",
            completedStepIds = setOf("OB-01", "OB-02"),
            foodPattern = FoodPattern.VEG_PLUS_NON_VEG,
            allowedAnimalFoods = emptySet(),
            primaryGoal = PrimaryGoal.LOSE_FAT,
            stateUtCode = "IN-MH",
            loggingLanguageCodes = listOf("en", "mr"),
        )
        r.save(draft)
        assertEquals(draft, r.load())
    }

    @Test
    fun save_replacesTheSingleRow() = runTest {
        val r = repo()
        r.save(OnboardingState(currentStepId = "OB-02"))
        r.save(OnboardingState(currentStepId = "OB-07"))
        assertEquals("OB-07", r.load()?.currentStepId)
    }

    @Test
    fun clear_removesTheDraft() = runTest {
        val r = repo()
        r.save(OnboardingState(currentStepId = "OB-03"))
        r.clear()
        assertNull(r.load())
    }

    @Test
    fun load_onCorruptJson_isNull() = runTest {
        val db = inMemoryNutriDb()
        db.onboardingDraftEntityQueries.upsertCurrent(json = "{ not valid", updatedAtEpochMillis = 0)
        assertNull(repo(db).load())
    }
}
