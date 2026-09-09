package `in`.acstechnologies.nutritrainerai.domain.model

/** Mass units the app accepts as input (PRD §10 "grams … kilograms and pounds"). */
enum class MassUnit(val grams: Double) {
    GRAM(1.0),
    KILOGRAM(1_000.0),
    OUNCE(28.349_523_125),
    POUND(453.592_37),
}

/** Volume units the app accepts as input (PRD §10 "millilitres"). */
enum class VolumeUnit(val millilitres: Double) {
    MILLILITRE(1.0),
    LITRE(1_000.0),
}

/**
 * A quantity as the user expressed it, before resolution to grams.
 *
 * [HouseholdCount] is a count of a named household unit (katori, roti, cup…)
 * whose gram weight is *not* fixed here — it is resolved later from household
 * calibration / regional defaults (PRD §5 "Household calibration").
 */
sealed interface Quantity {
    data class Mass(val value: Double, val unit: MassUnit) : Quantity

    data class Volume(val value: Double, val unit: VolumeUnit) : Quantity

    data class HouseholdCount(val count: Double, val unitId: String) : Quantity
}

/** Grams for a mass quantity. Household and volume quantities resolve elsewhere. */
fun Quantity.Mass.toGrams(): Double = value * unit.grams

/** Millilitres for a volume quantity. */
fun Quantity.Volume.toMillilitres(): Double = value * unit.millilitres
