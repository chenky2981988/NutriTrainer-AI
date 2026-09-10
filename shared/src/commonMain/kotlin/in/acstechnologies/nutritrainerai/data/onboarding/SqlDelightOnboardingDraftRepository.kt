package `in`.acstechnologies.nutritrainerai.data.onboarding

import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState
import `in`.acstechnologies.nutritrainerai.domain.repository.OnboardingDraftRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Stores [OnboardingState] as a JSON blob in the single draft row. A blob keeps
 * the draft trivially forward-compatible: unknown fields from a newer app
 * version survive a downgrade, and `ignoreUnknownKeys` handles the reverse.
 */
@OptIn(ExperimentalTime::class)
class SqlDelightOnboardingDraftRepository(
    db: NutriDb,
    private val json: Json,
    private val ioContext: CoroutineContext,
) : OnboardingDraftRepository {

    private val queries = db.onboardingDraftEntityQueries
    private val serializer = serializer<OnboardingState>()

    override suspend fun load(): OnboardingState? =
        withContext(ioContext) {
            queries.selectCurrent().executeAsOneOrNull()
                ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
        }

    override suspend fun save(state: OnboardingState) {
        withContext(ioContext) {
            queries.upsertCurrent(
                json = json.encodeToString(serializer, state),
                updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    override suspend fun clear() {
        withContext(ioContext) { queries.clearCurrent() }
    }
}
