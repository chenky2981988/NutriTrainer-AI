package `in`.acstechnologies.nutritrainerai.domain.model

import kotlinx.serialization.Serializable

/**
 * A candidate food from an online source (Open Food Facts today). Shown to the
 * user to confirm *before* it is saved (PRD §7 unknown-product workflow); on
 * confirm it becomes a [Food] in the local library.
 */
@Serializable
data class FoodSearchResult(
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val nutrientsPerBase: NutrientVector,
    val basis: MeasurementBasis,
    val servingGrams: Double? = null,
    val source: SourceType,
    /** Where it came from — kept for provenance / attribution (PRD §7). */
    val provenanceUrl: String? = null,
    /** Small front-of-pack image for the result row, if the source has one. */
    val thumbnailUrl: String? = null,
    /** Full-size photos to browse on the detail view: front, ingredients, nutrition panel. */
    val imageUrls: List<ProductImage> = emptyList(),
)

/** One product photo from an online source, tagged by what it shows. */
@Serializable
data class ProductImage(
    val url: String,
    val kind: ProductImageKind,
)

enum class ProductImageKind { FRONT, INGREDIENTS, NUTRITION }
