package `in`.acstechnologies.nutritrainerai.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_back
import nutritrainerai.shared.generated.resources.action_continue
import nutritrainerai.shared.generated.resources.action_skip_for_now
import nutritrainerai.shared.generated.resources.onboarding_step_progress
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The shared onboarding chrome (PRD §4.1.3): 24 dp column, "Step n of 10" + a
 * thin determinate bar and a non-destructive Back at the top, scrollable content,
 * and Continue pinned above the bottom safe area with a "Skip for now" secondary.
 */
@Composable
fun OnboardingScaffold(
    stepNumber: Int,
    totalSteps: Int,
    title: StringResource,
    helperText: StringResource?,
    continueEnabled: Boolean,
    continueLabel: StringResource,
    onBack: (() -> Unit)?,
    onContinue: () -> Unit,
    onSkip: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                TextButton(onClick = onBack) { Text(stringResource(Res.string.action_back)) }
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(Res.string.onboarding_step_progress, stepNumber, totalSteps),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LinearProgressIndicator(
            progress = { stepNumber.toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        if (helperText != null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(helperText), style = MaterialTheme.typography.bodyMedium)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }

        Button(
            onClick = onContinue,
            enabled = continueEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(continueLabel))
        }
        if (onSkip != null) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            ) {
                Text(stringResource(Res.string.action_skip_for_now))
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** The default Continue-button label; Review overrides it. */
internal val ContinueLabel: StringResource = Res.string.action_continue
