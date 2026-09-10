package `in`.acstechnologies.nutritrainerai.data.food

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import `in`.acstechnologies.nutritrainerai.data.db.FoodEntity
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.Food
import `in`.acstechnologies.nutritrainerai.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SqlDelightFoodRepository(
    db: NutriDb,
    private val ioContext: CoroutineContext,
) : FoodRepository {

    private val queries = db.foodEntityQueries

    override suspend fun findByName(name: String): Food? =
        withContext(ioContext) {
            queries.selectByNameKey(name.trim().lowercase()).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun upsert(food: Food) {
        withContext(ioContext) {
            queries.upsert(
                id = food.id,
                canonicalName = food.canonicalName,
                nameKey = food.nameKey,
                aliasesCsv = food.aliasesCsv(),
                basis = food.basis.name,
                source = food.source.name,
                confidenceBand = food.confidence.name,
                brand = food.brand,
                pack = food.pack,
                servingGrams = food.servingGrams,
                energyKcal = food.nutrientsPerBase.energyKcal,
                energyKj = food.nutrientsPerBase.energyKj,
                proteinG = food.nutrientsPerBase.proteinG,
                carbohydrateG = food.nutrientsPerBase.carbohydrateG,
                fatG = food.nutrientsPerBase.fatG,
                fibreG = food.nutrientsPerBase.fibreG,
                createdAtEpochMillis = food.createdAtEpochMillis,
            )
        }
    }

    override suspend fun search(query: String): List<Food> =
        withContext(ioContext) {
            queries.search(query.trim().lowercase()).executeAsList().map(FoodEntity::toDomain)
        }

    override fun observeAll(): Flow<List<Food>> =
        queries.selectAll().asFlow().mapToList(ioContext).map { rows -> rows.map(FoodEntity::toDomain) }

    override suspend fun delete(id: String) {
        withContext(ioContext) { queries.deleteById(id) }
    }
}
