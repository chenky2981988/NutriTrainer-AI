package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * One exchange in the Coach transcript (PRD §4.2's four short beats). Persisted
 * per day so the conversation survives an app restart.
 */
@Serializable
data class CoachTurn(
    val userText: String,
    val understanding: String,
    val result: String,
    val observation: String? = null,
    val needsConfirmation: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
)
