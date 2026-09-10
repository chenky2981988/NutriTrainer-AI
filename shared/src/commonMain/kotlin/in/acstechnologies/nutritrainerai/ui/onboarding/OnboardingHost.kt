package `in`.acstechnologies.nutritrainerai.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
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
import `in`.acstechnologies.nutritrainerai.domain.model.AnimalFood
import `in`.acstechnologies.nutritrainerai.domain.model.FoodPattern
import `in`.acstechnologies.nutritrainerai.domain.model.LanguageMode
import `in`.acstechnologies.nutritrainerai.domain.model.OnboardingStep
import `in`.acstechnologies.nutritrainerai.domain.model.PrimaryGoal
import `in`.acstechnologies.nutritrainerai.domain.region.IndiaRegionCatalog
import `in`.acstechnologies.nutritrainerai.domain.region.IndiaZone
import kotlinx.coroutines.flow.Flow
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_continue
import nutritrainerai.shared.generated.resources.ob_food_details_have_dairy
import nutritrainerai.shared.generated.resources.ob_food_details_which
import nutritrainerai.shared.generated.resources.ob_food_pattern_nonveg_note
import nutritrainerai.shared.generated.resources.ob_home_food_grains_label
import nutritrainerai.shared.generated.resources.ob_home_food_optional_note
import nutritrainerai.shared.generated.resources.ob_region_search
import nutritrainerai.shared.generated.resources.ob_review_region_skipped
import nutritrainerai.shared.generated.resources.ob_review_priority_note
import nutritrainerai.shared.generated.resources.ob_safety_allergies_label
import nutritrainerai.shared.generated.resources.ob_safety_avoid_field
import nutritrainerai.shared.generated.resources.ob_traditions_field
import nutritrainerai.shared.generated.resources.ob_traditions_note
import nutritrainerai.shared.generated.resources.ob_voice_logging_label
import nutritrainerai.shared.generated.resources.ob_welcome_benefit_1
import nutritrainerai.shared.generated.resources.ob_welcome_benefit_2
import nutritrainerai.shared.generated.resources.ob_welcome_benefit_3
import nutritrainerai.shared.generated.resources.onboarding_start_tracking
import nutritrainerai.shared.generated.resources.profile_food_category
import nutritrainerai.shared.generated.resources.profile_goal
import nutritrainerai.shared.generated.resources.profile_region
import nutritrainerai.shared.generated.resources.value_dash
import nutritrainerai.shared.generated.resources.zone_east
import nutritrainerai.shared.generated.resources.zone_north
import nutritrainerai.shared.generated.resources.zone_northeast
import nutritrainerai.shared.generated.resources.zone_south
import nutritrainerai.shared.generated.resources.zone_west_central
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Renders the current onboarding step inside [OnboardingScaffold] and forwards
 * user actions as [OnboardingIntent]s. Collects [effects] to signal completion.
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
        continueLabel = if (isReview) Res.string.onboarding_start_tracking else Res.string.action_continue,
        onBack = step.previous?.let { { onIntent(OnboardingIntent.Back) } },
        onContinue = {
            if (isReview) onIntent(OnboardingIntent.Finish(Clock.System.now().toEpochMilliseconds()))
            else onIntent(OnboardingIntent.Continue)
        },
        onSkip = if (step.required || isReview) null else ({ onIntent(OnboardingIntent.Skip) }),
    ) {
        when (step) {
            OnboardingStep.WELCOME -> WelcomeContent()
            OnboardingStep.FOOD_PATTERN -> FoodPatternContent(state, onIntent)
            OnboardingStep.FOOD_DETAILS -> FoodDetailsContent(state, onIntent)
            OnboardingStep.SAFETY_EXCLUSIONS -> SafetyContent(state, onIntent)
            OnboardingStep.STATE_UT -> RegionContent(state, onIntent)
            OnboardingStep.FOOD_TRADITIONS -> TraditionsContent(state, onIntent)
            OnboardingStep.HOME_FOOD_PROFILE -> HomeFoodContent(state, onIntent)
            OnboardingStep.GOAL_BODY -> GoalContent(state, onIntent)
            OnboardingStep.VOICE_LANGUAGE -> VoiceLanguageContent(state, onIntent)
            OnboardingStep.REVIEW -> ReviewContent(state, onIntent)
        }
    }
}

@Composable
private fun WelcomeContent() {
    listOf(
        Res.string.ob_welcome_benefit_1,
        Res.string.ob_welcome_benefit_2,
        Res.string.ob_welcome_benefit_3,
    ).forEach { line ->
        Text("•  ${stringResource(line)}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FoodDetailsContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    if (state.draft.foodPattern?.allowsAnimalFoodSelection == true) {
        Text(stringResource(Res.string.ob_food_details_which), style = MaterialTheme.typography.bodyMedium)
        ChipFlow {
            AnimalFood.entries.forEach { food ->
                ToggleChip(
                    labelRes = OnboardingCopy.animalFoodLabel(food),
                    selected = food in state.draft.allowedAnimalFoods,
                    onToggle = { onIntent(OnboardingIntent.ToggleAnimalFood(food)) },
                )
            }
        }
    } else {
        ToggleChip(
            labelRes = Res.string.ob_food_details_have_dairy,
            selected = state.draft.dairyAllowed,
            onToggle = { onIntent(OnboardingIntent.SetDairyAllowed(!state.draft.dairyAllowed)) },
        )
    }
}

@Composable
private fun SafetyContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    Text(stringResource(Res.string.ob_safety_allergies_label), style = MaterialTheme.typography.labelLarge)
    ChipFlow {
        OnboardingCopy.COMMON_ALLERGIES.forEach { (id, labelRes) ->
            ToggleChip(
                labelRes = labelRes,
                selected = id in state.draft.allergyIds,
                onToggle = { onIntent(OnboardingIntent.ToggleAllergy(id)) },
            )
        }
    }
    OutlinedTextField(
        value = state.draft.avoidanceText,
        onValueChange = { onIntent(OnboardingIntent.SetAvoidanceText(it)) },
        label = { Text(stringResource(Res.string.ob_safety_avoid_field)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TraditionsContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    OutlinedTextField(
        value = state.draft.customTraditionText,
        onValueChange = { onIntent(OnboardingIntent.SetCustomTraditionText(it)) },
        label = { Text(stringResource(Res.string.ob_traditions_field)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(Res.string.ob_traditions_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun HomeFoodContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    Text(stringResource(Res.string.ob_home_food_grains_label), style = MaterialTheme.typography.labelLarge)
    val grains = state.draft.householdProfile.stapleGrainIds
    ChipFlow {
        OnboardingCopy.STAPLE_GRAINS.forEach { (id, labelRes) ->
            ToggleChip(
                labelRes = labelRes,
                selected = id in grains,
                onToggle = {
                    val next = if (id in grains) grains - id else grains + id
                    onIntent(
                        OnboardingIntent.UpdateHouseholdProfile(
                            state.draft.householdProfile.copy(stapleGrainIds = next),
                        ),
                    )
                },
            )
        }
    }
    Text(stringResource(Res.string.ob_home_food_optional_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun VoiceLanguageContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    LanguageMode.entries.forEach { mode ->
        SelectableCard(
            selected = state.draft.languageMode == mode,
            onClick = { onIntent(OnboardingIntent.SetLanguageMode(mode)) },
        ) { Text(stringResource(OnboardingCopy.languageModeLabel(mode))) }
    }
    if (state.draft.languageMode != LanguageMode.ENGLISH_ONLY) {
        Text(stringResource(Res.string.ob_voice_logging_label), style = MaterialTheme.typography.labelLarge)
        ChipFlow {
            OnboardingCopy.LOGGING_LANGUAGES.forEach { (code, labelRes) ->
                ToggleChip(
                    labelRes = labelRes,
                    selected = code in state.draft.loggingLanguageCodes,
                    onToggle = { onIntent(OnboardingIntent.ToggleLoggingLanguage(code)) },
                )
            }
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
            Text(stringResource(OnboardingCopy.foodPatternLabel(pattern)), fontWeight = FontWeight.SemiBold)
            if (pattern == FoodPattern.VEG_PLUS_NON_VEG) {
                Text(
                    stringResource(Res.string.ob_food_pattern_nonveg_note),
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
        label = { Text(stringResource(Res.string.ob_region_search)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    IndiaRegionCatalog.grouped().forEach { (zone, all) ->
        val visible = all.filter { it in state.regionResults }
        if (visible.isNotEmpty()) {
            Text(
                stringResource(zoneLabel(zone)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            visible.forEach { entry ->
                SelectableCard(
                    selected = state.draft.stateUtCode == entry.code,
                    onClick = { onIntent(OnboardingIntent.SelectRegion(entry.code)) },
                ) { Text(entry.name) } // region names are data, not UI chrome
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
        ) { Text(stringResource(OnboardingCopy.goalLabel(goal))) }
    }
}

@Composable
private fun ReviewContent(state: OnboardingUiState, onIntent: (OnboardingIntent) -> Unit) {
    val d = state.draft
    val dash = stringResource(Res.string.value_dash)
    ReviewRow(
        stringResource(Res.string.profile_food_category),
        d.foodPattern?.let { stringResource(OnboardingCopy.foodPatternLabel(it)) } ?: dash,
    ) { onIntent(OnboardingIntent.GoToStep(OnboardingStep.FOOD_PATTERN)) }
    ReviewRow(
        stringResource(Res.string.profile_region),
        d.stateUtCode?.let { IndiaRegionCatalog.byCode(it)?.name } ?: stringResource(Res.string.ob_review_region_skipped),
    ) { onIntent(OnboardingIntent.GoToStep(OnboardingStep.STATE_UT)) }
    ReviewRow(
        stringResource(Res.string.profile_goal),
        d.primaryGoal?.let { stringResource(OnboardingCopy.goalLabel(it)) } ?: dash,
    ) { onIntent(OnboardingIntent.GoToStep(OnboardingStep.GOAL_BODY)) }
    Text(
        stringResource(Res.string.ob_review_priority_note),
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

private fun zoneLabel(zone: IndiaZone): StringResource = when (zone) {
    IndiaZone.NORTH -> Res.string.zone_north
    IndiaZone.WEST_AND_CENTRAL -> Res.string.zone_west_central
    IndiaZone.SOUTH -> Res.string.zone_south
    IndiaZone.EAST -> Res.string.zone_east
    IndiaZone.NORTHEAST -> Res.string.zone_northeast
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun ToggleChip(labelRes: StringResource, selected: Boolean, onToggle: () -> Unit) {
    FilterChip(selected = selected, onClick = onToggle, label = { Text(stringResource(labelRes)) })
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
