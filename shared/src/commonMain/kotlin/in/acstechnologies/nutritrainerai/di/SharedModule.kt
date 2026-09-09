package `in`.acstechnologies.nutritrainerai.di

import `in`.acstechnologies.nutritrainerai.data.db.DatabaseDriverFactory
import `in`.acstechnologies.nutritrainerai.data.db.NutriDb
import `in`.acstechnologies.nutritrainerai.data.db.createNutriDb
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The shared-layer Koin graph: things every platform gets for free.
 *
 * The platform entry point must additionally register a [DatabaseDriverFactory]
 * (Android needs a `Context`, iOS does not) before resolving [NutriDb].
 */
val sharedModule: Module = module {
    single<NutriDb> { createNutriDb(get<DatabaseDriverFactory>()) }

    single {
        Json {
            ignoreUnknownKeys = true // tolerate additive schema changes (PRD §8 versioned contracts)
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
