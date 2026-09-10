package `in`.acstechnologies.nutritrainerai.domain.resolve

import `in`.acstechnologies.nutritrainerai.domain.model.Food
import `in`.acstechnologies.nutritrainerai.domain.repository.FoodRepository

/**
 * Resolves in PRD §6 precedence order: **user-confirmed foods first**
 * ([FoodRepository]), then the bundled seed table. As the content pipeline lands
 * (Open Food Facts / IFCT / USDA) more tiers slot in between.
 */
class CompositeFoodResolver(
    private val userFoods: FoodRepository,
    private val seed: SeedFoodResolver,
) : FoodResolver {

    override suspend fun resolve(foodName: String): ResolvedFood? =
        userFoods.findByName(foodName)?.toResolvedFood() ?: seed.resolve(foodName)
}

/** A persisted [Food] as the resolver's DTO. */
fun Food.toResolvedFood(): ResolvedFood =
    ResolvedFood(
        canonicalName = canonicalName,
        nutrientsPerBase = nutrientsPerBase,
        basis = basis,
        source = source,
        confidence = confidence,
        pieceGrams = servingGrams,
    )
