package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * What a [NutrientVector] on a food/product is measured against
 * (PRD §6 "Store basis as per 100 g, per 100 ml, per serving or prepared").
 * Stored explicitly so a serving is never silently reinterpreted.
 */
@Serializable
enum class MeasurementBasis {
    PER_100_G,
    PER_100_ML,
    PER_SERVING,
    PREPARED,
}
