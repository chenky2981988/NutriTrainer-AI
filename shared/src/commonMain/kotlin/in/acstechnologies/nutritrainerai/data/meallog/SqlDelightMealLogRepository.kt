package `in`.acstechnologies.nutritrainerai.data.meallog

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import `in`.acstechnologies.nutritrainerai.data.db.MealItemEntity
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * SQLDelight-backed [MealLogRepository]. The generated `selectByDay` query only
 * returns non-deleted rows, and it re-emits on any write to `mealItemEntity`,
 * which is what lets Coach and Today stay in lock-step from one stream.
 *
 * [ioContext] is where DB work runs — a background dispatcher in production, a
 * test dispatcher in tests.
 */
class SqlDelightMealLogRepository(
    db: NutriDb,
    private val ioContext: CoroutineContext,
) : MealLogRepository {

    private val queries = db.mealItemEntityQueries

    override fun observeDay(dayEpochDay: Long): Flow<List<MealItem>> =
        queries.selectByDay(dayEpochDay)
            .asFlow()
            .mapToList(ioContext)
            .map { rows -> rows.map(MealItemEntity::toDomain) }

    override suspend fun getDay(dayEpochDay: Long): List<MealItem> =
        withContext(ioContext) {
            queries.selectByDay(dayEpochDay).executeAsList().map(MealItemEntity::toDomain)
        }

    override suspend fun upsert(item: MealItem) {
        withContext(ioContext) {
            queries.upsert(
                id = item.id,
                dayEpochDay = item.dayEpochDay,
                status = item.status.name,
                confirmed = item.confirmed,
                foodName = item.foodName,
                quantityGrams = item.quantityGrams,
                energyKcal = item.nutrients.energyKcal,
                energyKj = item.nutrients.energyKj,
                proteinG = item.nutrients.proteinG,
                carbohydrateG = item.nutrients.carbohydrateG,
                fatG = item.nutrients.fatG,
                fibreG = item.nutrients.fibreG,
                confidenceBand = item.confidence.name,
                sourceType = item.source.name,
                revision = item.revision.toLong(),
                calculationVersion = item.calculationVersion.toLong(),
                createdAtEpochMillis = item.createdAtEpochMillis,
                deletedAtEpochMillis = item.deletedAtEpochMillis,
            )
        }
    }

    override suspend fun softDelete(id: String, atEpochMillis: Long) {
        withContext(ioContext) {
            queries.softDelete(deletedAtEpochMillis = atEpochMillis, id = id)
        }
    }

    override suspend fun restore(id: String) {
        withContext(ioContext) {
            queries.restore(id = id)
        }
    }
}
