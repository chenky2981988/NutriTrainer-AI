package `in`.acstechnologies.nutritrainerai

import android.app.Application
import `in`.acstechnologies.nutritrainerai.di.initKoin

class NutriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}
