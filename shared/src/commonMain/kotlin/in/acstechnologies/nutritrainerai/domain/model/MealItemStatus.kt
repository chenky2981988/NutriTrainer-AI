package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * Whether a logged item was actually eaten or is only projected.
 *
 * Planned items are shown separately and **never** inflate consumed totals
 * (PRD §4.3, §5 "Daily total: sum confirmed consumed items only").
 */
@Serializable
enum class MealItemStatus {
    CONSUMED,
    PLANNED,
}
