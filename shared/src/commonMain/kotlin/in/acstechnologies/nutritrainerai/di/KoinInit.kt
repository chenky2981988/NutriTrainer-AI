package `in`.acstechnologies.nutritrainerai.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

/**
 * Boots the Koin container once. Safe to call repeatedly — a second call is a
 * no-op. Android passes `androidContext(...)` via [appDeclaration]; iOS calls it
 * with no arguments from the app entry point.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    if (runCatching { KoinPlatform.getKoin() }.isSuccess) return
    startKoin {
        appDeclaration()
        modules(sharedModule, platformModule())
    }
}
