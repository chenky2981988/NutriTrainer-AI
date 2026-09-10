package `in`.acstechnologies.nutritrainerai.data.coach

import `in`.acstechnologies.nutritrainerai.data.inMemoryNutriDb
import `in`.acstechnologies.nutritrainerai.domain.model.CoachTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightCoachTranscriptRepositoryTest {

    private fun repo() = SqlDelightCoachTranscriptRepository(inMemoryNutriDb(), Dispatchers.Unconfined)

    private fun turn(text: String, at: Long) =
        CoachTurn(userText = text, understanding = "ok", result = "+0 kcal", createdAtEpochMillis = at)

    @Test
    fun append_thenObserve_ordersByCreatedAt() = runTest {
        val r = repo()
        r.append(20_000L, turn("second", at = 20))
        r.append(20_000L, turn("first", at = 10))
        assertEquals(listOf("first", "second"), r.observeDay(20_000L).first().map { it.userText })
    }

    @Test
    fun transcript_isScopedToTheDay() = runTest {
        val r = repo()
        r.append(20_000L, turn("today", at = 1))
        r.append(20_001L, turn("tomorrow", at = 1))
        assertEquals(listOf("today"), r.observeDay(20_000L).first().map { it.userText })
    }

    @Test
    fun clearDay_emptiesOnlyThatDay() = runTest {
        val r = repo()
        r.append(20_000L, turn("a", at = 1))
        r.append(20_001L, turn("b", at = 1))
        r.clearDay(20_000L)
        assertEquals(emptyList(), r.observeDay(20_000L).first())
        assertEquals(listOf("b"), r.observeDay(20_001L).first().map { it.userText })
    }
}
