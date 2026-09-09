package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * Where a nutrition figure came from, in the resolution order of PRD §6
 * ("Source precedence"). Declaration order **is** the precedence order — keep it.
 */
@Serializable
enum class SourceType {
    /** 1 — user-confirmed label or recipe. Highest priority for that user+version. */
    USER_CONFIRMED,

    /** 2 — exact barcode / manufacturer label, after product + pack confirmation. */
    BARCODE_LABEL,

    /** 3 — licensed authoritative composition data (generic ingredients and foods). */
    AUTHORITATIVE_DATA,

    /** 4 — curated regional recipe (community default with assumptions). */
    CURATED_REGIONAL,

    /** 5 — open community database (validate and retain provenance). */
    OPEN_COMMUNITY,

    /** 6 — heuristic or LLM estimate. Last resort, ranged, low confidence. */
    HEURISTIC_ESTIMATE,
    ;

    /** 1 = highest precedence, matching PRD §6 numbering. Lower wins. */
    val precedenceRank: Int get() = ordinal + 1
}

/**
 * Provenance for an imported or entered nutrition record
 * (PRD §6 "Store label version, capture date and URL"; §7 "Every imported
 * record needs provenance, retrieval date, licence metadata").
 *
 * The timestamp is a raw epoch-millis so the domain stays free of date-time
 * API choices; the data layer converts to/from real time types.
 */
@Serializable
data class SourceRef(
    val type: SourceType,
    val labelVersion: String? = null,
    val capturedAtEpochMillis: Long? = null,
    val url: String? = null,
    val licence: String? = null,
    val note: String? = null,
)
