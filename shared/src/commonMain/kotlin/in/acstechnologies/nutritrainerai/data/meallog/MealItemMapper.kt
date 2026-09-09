package `in`.acstechnologies.nutritrainerai.data.meallog

import `in`.acstechnologies.nutritrainerai.data.db.MealItemEntity
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.model.MealItemStatus
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType

/**
 * Row <-> domain mapping. Enums are stored as their `name`; nutrition is spread
 * across columns and reassembled into a [NutrientVector]. SQLite INTEGER maps to
 * [Long], so revision/version narrow to [Int] here.
 */
internal fun MealItemEntity.toDomain(): MealItem =
    MealItem(
        id = id,
        dayEpochDay = dayEpochDay,
        status = MealItemStatus.valueOf(status),
        confirmed = confirmed,
        foodName = foodName,
        quantityGrams = quantityGrams,
        nutrients = NutrientVector(
            energyKcal = energyKcal,
            energyKj = energyKj,
            proteinG = proteinG,
            carbohydrateG = carbohydrateG,
            fatG = fatG,
            fibreG = fibreG,
        ),
        confidence = ConfidenceBand.valueOf(confidenceBand),
        source = SourceType.valueOf(sourceType),
        revision = revision.toInt(),
        calculationVersion = calculationVersion.toInt(),
        createdAtEpochMillis = createdAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
    )
