package `in`.acstechnologies.nutritrainerai.domain.usecase

import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.data.measurement.SqlDelightMeasurementRepository
import `in`.acstechnologies.nutritrainerai.domain.model.Measurement
import `in`.acstechnologies.nutritrainerai.domain.repository.MeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Exercises the use case over the real SQLDelight repository (in-memory). */
class GetWeightTrendUseCaseTest {

    private suspend fun repoWith(vararg dayToKg: Pair<Int, Double>): MeasurementRepository {
        val repo = SqlDelightMeasurementRepository(inMemoryNutriDb(), Dispatchers.Unconfined)
        dayToKg.forEach { (day, kg) ->
            repo.upsert(
                Measurement(
                    id = "m$day",
                    takenEpochDay = day.toLong(),
                    weightKg = kg,
                    createdAtEpochMillis = day.toLong(),
                ),
            )
        }
        return repo
    }

    @Test
    fun sevenDayAverage_matchesHandCalculation() = runTest {
        val repo = repoWith(5 to 82.0, 10 to 80.0, 11 to 79.5, 12 to 79.8)
        val trend = GetWeightTrendUseCase(repo).invoke(asOfEpochDay = 12, windowDays = 7)
        // window = days 6..12 -> 10, 11, 12 ; day 5 excluded
        assertEquals((80.0 + 79.5 + 79.8) / 3.0, trend!!, 1e-9)
    }

    @Test
    fun noReadingsInWindow_returnsNull() = runTest {
        val repo = repoWith(1 to 90.0)
        assertNull(GetWeightTrendUseCase(repo).invoke(asOfEpochDay = 100))
    }
}
