package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MeasurementSource {
    MANUAL,
    HEALTH_CONNECT,
    HEALTHKIT,
    WEARABLE,
}

/**
 * A body measurement reading (PRD §4.4). [weightKg] and [waistCm] are both
 * nullable — a reading may carry only one. [conditionsNote] records comparability
 * context (e.g. "morning, fasted") for BCA/weight trends.
 */
@Serializable
data class Measurement(
    val id: String,
    val takenEpochDay: Long,
    val weightKg: Double? = null,
    val waistCm: Double? = null,
    val conditionsNote: String? = null,
    val source: MeasurementSource = MeasurementSource.MANUAL,
    val createdAtEpochMillis: Long,
)
