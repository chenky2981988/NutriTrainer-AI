package `in`.acstechnologies.nutritrainerai.data.measurement

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import `in`.acstechnologies.nutritrainerai.data.db.MeasurementEntity
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.domain.calc.WeightPoint
import `in`.acstechnologies.nutritrainerai.domain.model.Measurement
import `in`.acstechnologies.nutritrainerai.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SqlDelightMeasurementRepository(
    db: NutriDb,
    private val ioContext: CoroutineContext,
) : MeasurementRepository {

    private val queries = db.measurementEntityQueries

    override fun observeAll(): Flow<List<Measurement>> =
        queries.selectAll()
            .asFlow()
            .mapToList(ioContext)
            .map { rows -> rows.map(MeasurementEntity::toDomain) }

    override suspend fun upsert(measurement: Measurement) {
        withContext(ioContext) {
            queries.upsert(
                id = measurement.id,
                takenEpochDay = measurement.takenEpochDay,
                weightKg = measurement.weightKg,
                waistCm = measurement.waistCm,
                conditionsNote = measurement.conditionsNote,
                source = measurement.source.name,
                createdAtEpochMillis = measurement.createdAtEpochMillis,
            )
        }
    }

    override suspend fun delete(id: String) {
        withContext(ioContext) { queries.deleteById(id) }
    }

    override suspend fun weightSeries(sinceEpochDay: Long): List<WeightPoint> =
        withContext(ioContext) {
            queries.selectWeightsSince(sinceEpochDay)
                .executeAsList()
                .map { WeightPoint(epochDay = it.takenEpochDay, kg = it.weightKg) }
        }
}
