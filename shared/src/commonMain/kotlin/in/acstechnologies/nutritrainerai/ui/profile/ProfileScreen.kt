package `in`.acstechnologies.nutritrainerai.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingState
import `in`.acstechnologies.nutritrainerai.domain.region.IndiaRegionCatalog
import `in`.acstechnologies.nutritrainerai.domain.repository.OnboardingDraftRepository
import `in`.acstechnologies.nutritrainerai.ui.onboarding.OnboardingCopy
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_redo_onboarding
import nutritrainerai.shared.generated.resources.action_settings
import nutritrainerai.shared.generated.resources.profile_default_language
import nutritrainerai.shared.generated.resources.profile_food_category
import nutritrainerai.shared.generated.resources.profile_goal
import nutritrainerai.shared.generated.resources.profile_logging_languages
import nutritrainerai.shared.generated.resources.profile_not_set
import nutritrainerai.shared.generated.resources.profile_region
import nutritrainerai.shared.generated.resources.profile_title
import nutritrainerai.shared.generated.resources.value_dash
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Profile — goals and controls (PRD §4.5). A read-back of the onboarding answers. */
@Composable
fun ProfileScreen(onOpenSettings: () -> Unit, onRestartOnboarding: () -> Unit) {
    val draftRepo = koinInject<OnboardingDraftRepository>()
    val profile by produceState<OnboardingState?>(initialValue = null) {
        value = draftRepo.load()
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.profile_title), style = MaterialTheme.typography.headlineSmall)

        val p = profile
        val dash = stringResource(Res.string.value_dash)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(
                    stringResource(Res.string.profile_food_category),
                    p?.foodPattern?.let { stringResource(OnboardingCopy.foodPatternLabel(it)) } ?: dash,
                )
                Field(
                    stringResource(Res.string.profile_region),
                    p?.stateUtCode?.let { IndiaRegionCatalog.byCode(it)?.name }
                        ?: stringResource(Res.string.profile_not_set),
                )
                Field(
                    stringResource(Res.string.profile_goal),
                    p?.primaryGoal?.let { stringResource(OnboardingCopy.goalLabel(it)) } ?: dash,
                )
                Field(
                    stringResource(Res.string.profile_logging_languages),
                    p?.loggingLanguageCodes?.takeIf { it.isNotEmpty() }?.joinToString()
                        ?: stringResource(Res.string.profile_default_language),
                )
            }
        }

        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_settings))
        }
        OutlinedButton(onClick = onRestartOnboarding, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.action_redo_onboarding))
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
