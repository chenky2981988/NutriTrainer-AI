package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * Confidence in a nutrition figure (PRD §6 "Confidence bands"). Drives
 * presentation: value+source, value+tolerance, silent range, wide range, or
 * "must choose before saving". **Never** communicated by colour alone (PRD §10).
 */
@Serializable
enum class ConfidenceBand {
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,

    /** The product/food cannot be distinguished; the user must pick before saving. */
    UNRESOLVED,
}

/**
 * A value the app is honest about approximating (PRD §3 "Approximate honestly —
 * use ranges when inputs are ambiguous"). Invariant: [low] ≤ [estimate] ≤ [high].
 */
data class Ranged<T : Comparable<T>>(
    val low: T,
    val estimate: T,
    val high: T,
) {
    init {
        require(estimate in low..high) {
            "Ranged bounds out of order: $low <= $estimate <= $high"
        }
    }

    val isExact: Boolean get() = low == high

    companion object {
        fun <T : Comparable<T>> exact(value: T): Ranged<T> = Ranged(value, value, value)
    }
}
