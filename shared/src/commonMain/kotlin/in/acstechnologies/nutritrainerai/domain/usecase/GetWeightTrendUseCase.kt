package `in`.acstechnologies.nutritrainerai.domain.usecase

import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.repository.MeasurementRepository

/**
 * The Progress tab's default: a rolling weight average over [windowDays] ending
 * at [asOfEpochDay] (PRD §4.4 "Default to seven-day weight average"; §5).
 * `null` when there isn't a reading in the window.
 *
 * A use case: one job, constructor-injected dependency, no framework. Pure to
 * test — pair it with a fake [MeasurementRepository].
 */
class GetWeightTrendUseCase(
    private val measurements: MeasurementRepository,
) {
    suspend operator fun invoke(asOfEpochDay: Long, windowDays: Int = 7): Double? {
        require(windowDays > 0) { "windowDays must be > 0, was $windowDays" }
        val since = asOfEpochDay - (windowDays - 1)
        val points = measurements.weightSeries(since)
        return NutritionMath.rollingWeightAverageKg(points, windowDays, asOfEpochDay)
    }
}
