package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.calc.WeightPoint
import `in`.acstechnologies.nutritrainerai.domain.model.Measurement
import kotlinx.coroutines.flow.Flow

/** Local store of body measurements (PRD §4.4). */
interface MeasurementRepository {

    fun observeAll(): Flow<List<Measurement>>

    suspend fun upsert(measurement: Measurement)

    suspend fun delete(id: String)

    /** Weight readings on or after [sinceEpochDay], oldest first — input to the weight-trend calc. */
    suspend fun weightSeries(sinceEpochDay: Long): List<WeightPoint>
}
