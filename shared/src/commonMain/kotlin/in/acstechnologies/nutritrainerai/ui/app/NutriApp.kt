package `in`.acstechnologies.nutritrainerai.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState
import `in`.acstechnologies.nutritrainerai.domain.model.ThemePreference
import `in`.acstechnologies.nutritrainerai.domain.repository.AppSettingsRepository
import `in`.acstechnologies.nutritrainerai.domain.repository.OnboardingDraftRepository
import `in`.acstechnologies.nutritrainerai.ui.coach.CoachScreen
import `in`.acstechnologies.nutritrainerai.ui.coach.CoachViewModel
import `in`.acstechnologies.nutritrainerai.ui.library.LibraryScreen
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingRoute
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingViewModel
import `in`.acstechnologies.nutritrainerai.ui.profile.ProfileScreen
import `in`.acstechnologies.nutritrainerai.ui.progress.ProgressScreen
import `in`.acstechnologies.nutritrainerai.ui.settings.SettingsScreen
import `in`.acstechnologies.nutritrainerai.ui.theme.NutriTheme
import `in`.acstechnologies.nutritrainerai.ui.theme.isDark
import `in`.acstechnologies.nutritrainerai.ui.today.TodayScreen
import `in`.acstechnologies.nutritrainerai.ui.today.TodayViewModel
import kotlinx.coroutines.launch
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.tab_coach
import nutritrainerai.shared.generated.resources.tab_library
import nutritrainerai.shared.generated.resources.tab_profile
import nutritrainerai.shared.generated.resources.tab_progress
import nutritrainerai.shared.generated.resources.tab_today
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Whole-app root: theme, the onboarding gate, then the tabbed home. */
@Composable
fun NutriApp() {
    KoinContext {
        val settings = koinInject<AppSettingsRepository>()
        val themePref by settings.themePreference().collectAsState(ThemePreference.DEFAULT)

        NutriTheme(darkTheme = themePref.isDark()) {
            Surface(Modifier.fillMaxSize()) {
                val draftRepo = koinInject<OnboardingDraftRepository>()
                var onboarded by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    onboarded = draftRepo.load()?.isComplete == true
                }

                when (onboarded) {
                    null -> LoadingScreen()
                    false -> {
                        val vm = koinInject<OnboardingViewModel>()
                        OnboardingRoute(vm) { onboarded = true }
                    }
                    true -> HomeScaffold(onRestartOnboarding = { onboarded = false })
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private enum class HomeTab(val labelRes: StringResource, val glyph: String) {
    COACH(Res.string.tab_coach, "💬"),
    TODAY(Res.string.tab_today, "☑"),
    PROGRESS(Res.string.tab_progress, "📈"),
    LIBRARY(Res.string.tab_library, "📖"),
    PROFILE(Res.string.tab_profile, "👤"),
}

@OptIn(ExperimentalTime::class)
@Composable
private fun HomeScaffold(onRestartOnboarding: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.COACH) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val day = remember { Clock.System.now().toEpochMilliseconds() / 86_400_000L }
    val scope = rememberCoroutineScope()
    val draftRepo = koinInject<OnboardingDraftRepository>()

    if (settingsOpen) {
        SettingsScreen(onBack = { settingsOpen = false })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.glyph) },
                        label = { Text(stringResource(t.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                // koinViewModel keeps these in the host ViewModelStore, so the Coach
                // transcript and Today state survive tab switches (recreated only on
                // process death — chat history persistence is a separate follow-up).
                HomeTab.COACH -> CoachScreen(koinViewModel<CoachViewModel> { parametersOf(day) })
                HomeTab.TODAY -> TodayScreen(koinViewModel<TodayViewModel> { parametersOf(day) })
                HomeTab.PROGRESS -> ProgressScreen(day = day)
                HomeTab.LIBRARY -> LibraryScreen()
                HomeTab.PROFILE -> ProfileScreen(
                    onOpenSettings = { settingsOpen = true },
                    onRestartOnboarding = {
                        scope.launch {
                            draftRepo.save(OnboardingState())
                            onRestartOnboarding()
                        }
                    },
                )
            }
        }
    }
}
