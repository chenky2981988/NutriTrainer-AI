package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.Food
import kotlinx.coroutines.flow.Flow

/** Store of foods the app knows — currently just the ones the user teaches it. */
interface FoodRepository {

    /** Exact match on canonical name or an alias (case-insensitive). */
    suspend fun findByName(name: String): Food?

    suspend fun upsert(food: Food)

    suspend fun search(query: String): List<Food>

    fun observeAll(): Flow<List<Food>>

    suspend fun delete(id: String)
}
