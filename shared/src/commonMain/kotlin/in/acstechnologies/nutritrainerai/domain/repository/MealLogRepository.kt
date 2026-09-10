package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import kotlinx.coroutines.flow.Flow

/**
 * The single source of truth for logged items — the local database (PRD §9
 * "SQLite remains the local source of truth"). Coach and Today both read the
 * same [observeDay] stream, which is what keeps them synchronised (PRD §2A).
 *
 * [observeDay] and [getDay] exclude soft-deleted rows.
 */
interface MealLogRepository {

    /** Live list of a day's items, newest-write last. Emits again on any change. */
    fun observeDay(dayEpochDay: Long): Flow<List<MealItem>>

    /** One-shot read of a day's items. */
    suspend fun getDay(dayEpochDay: Long): List<MealItem>

    /** Insert, or replace an existing row by id (corrections replace, never duplicate — PRD §4.2). */
    suspend fun upsert(item: MealItem)

    /** Soft-delete with undo (PRD FR07); bumps the row's revision. */
    suspend fun softDelete(id: String, atEpochMillis: Long)

    /** Undo a [softDelete] — clears the delete marker; bumps the row's revision (PRD FR07). */
    suspend fun restore(id: String)
}
