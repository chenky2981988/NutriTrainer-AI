package `in`.acstechnologies.nutritrainerai.domain.repository

import `in`.acstechnologies.nutritrainerai.domain.model.CoachTurn
import kotlinx.coroutines.flow.Flow

/**
 * The persisted Coach conversation, one list per day (PRD §4.2). Kept separate
 * from the meal log — this is the dialogue, not the health record.
 */
interface CoachTranscriptRepository {

    /** Live transcript for a day, oldest turn first. */
    fun observeDay(dayEpochDay: Long): Flow<List<CoachTurn>>

    /** Append one turn. [CoachTurn.createdAtEpochMillis] orders it. */
    suspend fun append(dayEpochDay: Long, turn: CoachTurn)

    /** Wipe a day's transcript. */
    suspend fun clearDay(dayEpochDay: Long)
}
