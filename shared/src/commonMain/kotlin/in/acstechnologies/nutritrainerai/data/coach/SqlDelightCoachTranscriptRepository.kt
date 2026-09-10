package `in`.acstechnologies.nutritrainerai.data.coach

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import `in`.acstechnologies.nutritrainerai.data.db.CoachTurnEntity
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.CoachTurn
import `in`.acstechnologies.nutritrainerai.domain.repository.CoachTranscriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SqlDelightCoachTranscriptRepository(
    db: NutriDb,
    private val ioContext: CoroutineContext,
) : CoachTranscriptRepository {

    private val queries = db.coachTurnEntityQueries

    override fun observeDay(dayEpochDay: Long): Flow<List<CoachTurn>> =
        queries.selectByDay(dayEpochDay)
            .asFlow()
            .mapToList(ioContext)
            .map { rows -> rows.map(CoachTurnEntity::toDomain) }

    override suspend fun append(dayEpochDay: Long, turn: CoachTurn) {
        withContext(ioContext) {
            queries.insert(
                dayEpochDay = dayEpochDay,
                userText = turn.userText,
                understanding = turn.understanding,
                result = turn.result,
                observation = turn.observation,
                needsConfirmation = turn.needsConfirmation,
                createdAtEpochMillis = turn.createdAtEpochMillis,
            )
        }
    }

    override suspend fun clearDay(dayEpochDay: Long) {
        withContext(ioContext) { queries.deleteDay(dayEpochDay) }
    }
}

private fun CoachTurnEntity.toDomain() = CoachTurn(
    userText = userText,
    understanding = understanding,
    result = result,
    observation = observation,
    needsConfirmation = needsConfirmation,
    createdAtEpochMillis = createdAtEpochMillis,
)
