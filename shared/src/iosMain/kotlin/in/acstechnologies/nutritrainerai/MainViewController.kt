package `in`.acstechnologies.nutritrainerai

import androidx.compose.ui.window.ComposeUIViewController
import `in`.acstechnologies.nutritrainerai.di.initKoin

/** Call once from Swift (`iOSApp.init`) before the first view is created. */
fun startKoin() = initKoin()

fun MainViewController() = ComposeUIViewController {
    initKoin() // idempotent; safety net if startKoin() was not called
    App()
}
