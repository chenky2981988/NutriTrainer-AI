package `in`.acstechnologies.nutritrainerai.data.food

import `in`.acstechnologies.nutritrainerai.data.db.FoodEntity
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.Food
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType

internal fun FoodEntity.toDomain(): Food =
    Food(
        id = id,
        canonicalName = canonicalName,
        aliases = aliasesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        nutrientsPerBase = NutrientVector(energyKcal, energyKj, proteinG, carbohydrateG, fatG, fibreG),
        basis = MeasurementBasis.valueOf(basis),
        source = SourceType.valueOf(source),
        confidence = ConfidenceBand.valueOf(confidenceBand),
        brand = brand,
        pack = pack,
        servingGrams = servingGrams,
        createdAtEpochMillis = createdAtEpochMillis,
    )

internal fun Food.aliasesCsv(): String = aliases.joinToString(",") { it.trim().lowercase() }
