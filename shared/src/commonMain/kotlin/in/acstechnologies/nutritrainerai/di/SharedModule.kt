package `in`.acstechnologies.nutritrainerai.di

import `in`.acstechnologies.nutritrainerai.ai.NutritionLanguageEngine
import `in`.acstechnologies.nutritrainerai.ai.parser.DeterministicNutritionParser
import `in`.acstechnologies.nutritrainerai.ai.pipeline.LogParsedIntentUseCase
import `in`.acstechnologies.nutritrainerai.data.food.OpenFoodFactsSource
import `in`.acstechnologies.nutritrainerai.data.food.SqlDelightFoodRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.FoodRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.OnlineFoodSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import `in`.acstechnologies.nutritrainerai.domain.resolve.CompositeFoodResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.FoodResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.QuantityResolver
import `in`.acstechnologies.nutritrainerai.domain.resolve.SeedFoodResolver
import `in`.acstechnologies.nutritrainerai.domain.usecase.AddUserFoodUseCase
import `in`.acstechnologies.nutritrainerai.domain.usecase.ObserveDayUseCase
import `in`.acstechnologies.nutritrainerai.ui.coach.CoachViewModel
import `in`.acstechnologies.nutritrainerai.ui.today.TodayViewModel
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import `in`.acstechnologies.nutritrainerai.data.db.DatabaseDriverFactory
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.db.createNutriDb
import `in`.acstechnologies.nutritrainerai.data.meallog.SqlDelightMealLogRepository
import `in`.acstechnologies.nutritrainerai.data.measurement.SqlDelightMeasurementRepository
import `in`.acstechnologies.nutritrainerai.data.onboarding.SqlDelightOnboardingDraftRepository
import `in`.acstechnologies.nutritrainerai.data.settings.SqlDelightAppSettingsRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.AppSettingsRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.MealLogRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.MeasurementRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.OnboardingDraftRepository
import `in`.acstechnologies.nutritrainerai.domain.usecase.GetWeightTrendUseCase
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingViewModel
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

@OptIn(ExperimentalTime::class)
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
    single<OnboardingDraftRepository> {
        SqlDelightOnboardingDraftRepository(get(), get(), Dispatchers.Default)
    }
    single<AppSettingsRepository> { SqlDelightAppSettingsRepository(get(), Dispatchers.Default) }
    single<FoodRepository> { SqlDelightFoodRepository(get(), Dispatchers.Default) }

    // Online catalogue (Open Food Facts, PRD §7).
    single {
        HttpClient {
            install(ContentNegotiation) { json(get()) }
        }
    }
    single<OnlineFoodSource> { OpenFoodFactsSource(get()) }

    // Resolution + calculation pipeline (deterministic; no AI in the numbers).
    single { SeedFoodResolver() }
    // User-confirmed foods first (PRD §6 rank 1), then the seed table.
    single<FoodResolver> { CompositeFoodResolver(get(), get<SeedFoodResolver>()) }
    single { QuantityResolver() }
    single { ObserveDayUseCase(get()) }
    single {
        LogParsedIntentUseCase(
            mealLog = get(),
            foods = get(),
            quantities = get(),
            now = { Clock.System.now().toEpochMilliseconds() },
        )
    }
    factory {
        AddUserFoodUseCase(
            foods = get(),
            mealLog = get(),
            quantities = get(),
            now = { Clock.System.now().toEpochMilliseconds() },
            idFactory = { "uf-" + Clock.System.now().toEpochMilliseconds() + "-" + Random.nextInt(0, 1_000_000) },
        )
    }

    factory { GetWeightTrendUseCase(get()) }
    factory { OnboardingViewModel(get()) }

    // Screen ViewModels take the day being viewed as a runtime parameter.
    factory { (dayEpochDay: Long) -> TodayViewModel(get(), dayEpochDay) }
    factory { (dayEpochDay: Long) ->
        CoachViewModel(get(DeterministicEngine), get(), get(), get(), get(), dayEpochDay)
    }
}
