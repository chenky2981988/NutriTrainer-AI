package `in`.acstechnologies.nutritrainerai.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal
import `in`.acstechnologies.nutritrainerai.domain.region.IndiaRegionCatalog
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Renders the current onboarding step inside [OnboardingScaffold] and forwards
 * user actions as [OnboardingIntent]s. Collects [effects] to signal completion.
 *
 * OB-02 (food pattern) and OB-05 (state/UT) are built out; the remaining steps
 * render an informational placeholder for now — their content lands next.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun OnboardingHost(
    state: OnboardingUiState,
    effects: Flow<OnboardingEffect>,
    onIntent: (OnboardingIntent) -> Unit,
    onCompleted: () -> Unit,
) {
    LaunchedEffect(effects) {
        effects.collect { if (it is OnboardingEffect.Completed) onCompleted() }
    }

    val step = state.step
    val isReview = step == OnboardingStep.REVIEW
    val copy = OnboardingCopy.forStep(step)

    OnboardingScaffold(
        stepNumber = state.stepNumber,
        totalSteps = state.totalSteps,
        title = copy.title,
        helperText = copy.helper,
        continueEnabled = if (isReview) state.canFinish else state.canContinue,
        continueLabel = if (isReview) "Start tracking" else "Continue",
        onBack = step.previous?.let { { onIntent(OnboardingIntent.Back) } },
        onContinue = {
            if (isReview) onIntent(OnboardingIntent.Finish(Clock.System.now().toEpochMilliseconds()))
            else onIntent(OnboardingIntent.Continue)
        },
        onSkip = if (step.required || isReview) null else ({ onIntent(OnboardingIntent.Skip) }),
    ) {
        when (step) {
            OnboardingStep.FOOD_PATTERN -> FoodPatternContent(state, onIntent)
            OnboardingStep.STATE_UT -> RegionContent(state, onIntent)
            OnboardingStep.GOAL_BODY -> GoalContent(state, onIntent)
            OnboardingStep.REVIEW -> ReviewContent(state, onIntent)
            else -> Text(
                copy.placeholderBody,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun FoodPatternContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    FoodPattern.entries.forEach { pattern ->
        SelectableCard(
            selected = state.draft.foodPattern == pattern,
            onClick = { onIntent(OnboardingIntent.SelectFoodPattern(pattern)) },
        ) {
            Text(OnboardingCopy.foodPatternLabel(pattern), fontWeight = FontWeight.SemiBold)
            if (pattern == FoodPattern.VEG_PLUS_NON_VEG) {
                Text(
                    "Includes every vegetarian food. It does not mean every meal is non-veg.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RegionContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    OutlinedTextField(
        value = state.regionQuery,
        onValueChange = { onIntent(OnboardingIntent.SetRegionQuery(it)) },
        label = { Text("Search state or union territory") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    IndiaRegionCatalog.grouped().forEach { (zone, all) ->
        val visible = all.filter { it in state.regionResults }
        if (visible.isNotEmpty()) {
            Text(
                zone.label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            visible.forEach { entry ->
                SelectableCard(
                    selected = state.draft.stateUtCode == entry.code,
                    onClick = { onIntent(OnboardingIntent.SelectRegion(entry.code)) },
                ) { Text(entry.name) }
            }
        }
    }
}

@Composable
private fun GoalContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    PrimaryGoal.entries.forEach { goal ->
        SelectableCard(
            selected = state.draft.primaryGoal == goal,
            onClick = { onIntent(OnboardingIntent.SetGoal(goal)) },
        ) { Text(OnboardingCopy.goalLabel(goal)) }
    }
}

@Composable
private fun ReviewContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val d = state.draft
    ReviewRow("Food category", d.foodPattern?.let(OnboardingCopy::foodPatternLabel) ?: "—") {
        onIntent(OnboardingIntent.GoToStep(OnboardingStep.FOOD_PATTERN))
    }
    ReviewRow("Region", d.stateUtCode?.let { IndiaRegionCatalog.byCode(it)?.name } ?: "Skipped") {
        onIntent(OnboardingIntent.GoToStep(OnboardingStep.STATE_UT))
    }
    ReviewRow("Goal", d.primaryGoal?.let(OnboardingCopy::goalLabel) ?: "—") {
        onIntent(OnboardingIntent.GoToStep(OnboardingStep.GOAL_BODY))
    }
    Text(
        "Your food category always takes priority over regional suggestions.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ReviewRow(label: String, value: String, onEdit: () -> Unit) {
    SelectableCard(selected = false, onClick = onEdit) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = if (selected) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.elevatedCardColors()
        },
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

/** A convenience wrapper: collects [OnboardingViewModel.state] and drives [OnboardingHost]. */
@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel, onCompleted: () -> Unit) {
    val state by viewModel.state.collectAsState()
    OnboardingHost(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onCompleted = onCompleted,
    )
}
