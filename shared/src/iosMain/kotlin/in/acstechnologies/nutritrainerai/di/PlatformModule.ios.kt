package `in`.acstechnologies.nutritrainerai.di

import `in`.acstechnologies.nutritrainerai.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
}
