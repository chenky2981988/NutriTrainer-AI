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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The shared onboarding chrome (PRD §4.1.3 "Onboarding visual and interaction
 * specification"): 24 dp column, "Step n of 10" + a thin determinate bar and a
 * non-destructive Back at the top, scrollable content, and Continue pinned above
 * the bottom safe area with a "Skip for now" secondary.
 */
@Composable
fun OnboardingScaffold(
    stepNumber: Int,
    totalSteps: Int,
    title: String,
    helperText: String?,
    continueEnabled: Boolean,
    continueLabel: String,
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
                TextButton(onClick = onBack) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            Text("Step $stepNumber of $totalSteps", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { stepNumber.toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (helperText != null) {
            Spacer(Modifier.height(8.dp))
            Text(helperText, style = MaterialTheme.typography.bodyMedium)
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
            Text(continueLabel)
        }
        if (onSkip != null) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
            ) {
                Text("Skip for now")
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }
}
