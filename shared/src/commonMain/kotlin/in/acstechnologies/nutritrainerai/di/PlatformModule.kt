package `in`.acstechnologies.nutritrainerai.di

import org.koin.core.module.Module

/**
 * Platform-provided bindings the shared graph needs — chiefly the
 * `DatabaseDriverFactory` (Android needs a `Context`, iOS does not).
 */
expect fun platformModule(): Module
