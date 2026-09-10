package `in`.acstechnologies.nutritrainerai.di

import android.content.Context
import org.koin.android.ext.koin.androidContext

/**
 * Android entry point for Koin startup — keeps the koin dependency inside
 * `:shared` so `:androidApp` only needs to hand over its `Context`.
 */
fun initKoin(context: Context) = initKoin {
    androidContext(context.applicationContext)
}
