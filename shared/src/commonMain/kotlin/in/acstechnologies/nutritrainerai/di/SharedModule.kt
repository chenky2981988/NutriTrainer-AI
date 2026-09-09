package `in`.acstechnologies.nutritrainerai.di

import `in`.acstechnologies.nutritrainerai.ai.NutritionLanguageEngine
import `in`.acstechnologies.nutritrainerai.ai.parser.DeterministicNutritionParser
import `in`.acstechnologies.nutritrainerai.data.db.DatabaseDriverFactory
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.db.createNutriDb
import `in`.acstechnologies.nutritrainerai.data.meallog.SqlDelightMealLogRepository
import `in`.acstechnologies.nutritrainerai.data.measurement.SqlDelightMeasurementRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.MeasurementRepository
import `in`.acstechnologies.nutritrainerai.domain.usecase.GetWeightTrendUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * The shared-layer Koin graph: things every platform gets for free.
 *
 * The platform entry point must additionally register a [DatabaseDriverFactory]
 * (Android needs a `Context`, iOS does not) before resolving [NutriDb].
 */
/** DI qualifier for the always-available fallback engine (PRD §8). */
val DeterministicEngine = named("deterministic")

val sharedModule: Module = module {
    single<NutriDb> { createNutriDb(get<DatabaseDriverFactory>()) }

    single {
        Json {
            ignoreUnknownKeys = true // tolerate additive schema changes (PRD §8 versioned contracts)
            encodeDefaults = true
            explicitNulls = false
        }
    }

    // The model-free fallback engine. Platform modules add their on-device engine
    // (Gemini Nano / Apple Foundation Models) and the selection logic on top.
    single<NutritionLanguageEngine>(DeterministicEngine) { DeterministicNutritionParser() }

    // Data layer — DB work runs on the default dispatcher.
    single<MealLogRepository> { SqlDelightMealLogRepository(get(), Dispatchers.Default) }
    single<MeasurementRepository> { SqlDelightMeasurementRepository(get(), Dispatchers.Default) }

    factory { GetWeightTrendUseCase(get()) }
}
