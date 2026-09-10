package `in`.acstechnologies.nutritrainerai.domain.usecase

import `in`.acstechnologies.nutritrainerai.domain.calc.NutritionMath
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.Food
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import `in`.acstechnologies.nutritrainerai.domain.repository.FoodRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.resolve.QuantityResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.toResolvedFood

/**
 * What the "add a food" form collects. [entryAmount]/[entryUnit] are only used
 * when re-resolving a pending log entry; a plain Library add ignores them.
 */
data class NewFoodDetails(
    val name: String,
    val brand: String? = null,
    val pack: String? = null,
    val basis: MeasurementBasis,
    val nutrientsPerBase: NutrientVector,
    /** Grams for one serving, when [basis] is [MeasurementBasis.PER_SERVING]. */
    val servingGrams: Double? = null,
    val entryAmount: Double = 0.0,
    val entryUnit: String = "",
)

/**
 * "Teach the app a food." Persists [NewFoodDetails] as a user-confirmed [Food]
 * (PRD §6 rank 1) and re-resolves the pending meal item against it, so the entry
 * flips from `UNRESOLVED` (0 kcal, uncounted) to a real, counted value — and
 * every future log of that name resolves instantly.
 */
class AddUserFoodUseCase(
    private val foods: FoodRepository,
    private val mealLog: MealLogRepository,
    private val quantities: QuantityResolver,
    private val now: () -> Long,
    private val idFactory: () -> String,
    private val calculationVersion: Int = 1,
) {
    /** Persist [details] as a user-confirmed food. */
    suspend fun add(details: NewFoodDetails): Food {
        val food = Food(
            id = idFactory(),
            canonicalName = details.name.trim(),
            aliases = emptyList(),
            nutrientsPerBase = details.nutrientsPerBase,
            basis = details.basis,
            source = SourceType.USER_CONFIRMED,
            confidence = ConfidenceBand.VERY_HIGH,
            brand = details.brand?.trim()?.ifBlank { null },
            pack = details.pack?.trim()?.ifBlank { null },
            servingGrams = details.servingGrams,
            createdAtEpochMillis = now(),
        )
        foods.upsert(food)
        return food
    }

    suspend fun addAndResolve(
        dayEpochDay: Long,
        mealItemId: String,
        details: NewFoodDetails,
    ): MealItem? {
        val food = add(details)

        val target = mealLog.getDay(dayEpochDay).firstOrNull { it.id == mealItemId } ?: return null
        val resolved = food.toResolvedFood()
        val grams = quantities.toGrams(details.entryAmount, details.entryUnit, resolved)
            ?: return null

        val updated = target.copy(
            foodName = food.canonicalName,
            quantityGrams = grams,
            nutrients = NutritionMath.ingredientNutrients(resolved.nutrientsPerBase, grams),
            confidence = ConfidenceBand.VERY_HIGH,
            source = SourceType.USER_CONFIRMED,
            confirmed = true,
            revision = target.revision + 1,
            calculationVersion = calculationVersion,
        )
        mealLog.upsert(updated)
        return updated
    }
}
