package `in`.acstechnologies.nutritrainerai.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.CoachTurn
import `in`.acstechnologies.nutritrainerai.ui.food.AddFoodSheet
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_send
import nutritrainerai.shared.generated.resources.coach_composer_placeholder
import nutritrainerai.shared.generated.resources.coach_empty_hint
import nutritrainerai.shared.generated.resources.coach_kcal_so_far
import nutritrainerai.shared.generated.resources.coach_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun CoachScreen(viewModel: CoachViewModel) {
    val state by viewModel.state.collectAsState()
    val pending = state.pendingFood
    if (pending != null) {
        AddFoodSheet(
            prefillName = pending.guessedName,
            showEntryAmount = true,
            prefillAmount = pending.guessedAmount,
            prefillUnit = pending.guessedUnit,
            onSave = { viewModel.onIntent(CoachIntent.SubmitNewFood(it)) },
            onCancel = { viewModel.onIntent(CoachIntent.DismissAddFood) },
            onlineSearching = pending.onlineSearching,
            onlineResults = pending.onlineResults,
        )
    } else {
        CoachContent(state, viewModel::onIntent)
    }
}

@Composable
private fun CoachContent(state: CoachUiState, onIntent: (CoachIntent) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.turns.size) {
        if (state.turns.isNotEmpty()) listState.animateScrollToItem(state.turns.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(Res.string.coach_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.coach_kcal_so_far, state.dayTotals.consumed.energyKcal.roundToInt()),
            style = MaterialTheme.typography.bodySmall,
        )

        if (state.turns.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.coach_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.turns) { turn -> TurnBubbles(turn) }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.composerText,
                onValueChange = { onIntent(CoachIntent.ComposerChanged(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(Res.string.coach_composer_placeholder)) },
            )
            Button(
                onClick = { onIntent(CoachIntent.Submit) },
                enabled = state.composerText.isNotBlank() && !state.submitting,
            ) { Text(stringResource(Res.string.action_send)) }
        }
    }
}

@Composable
private fun TurnBubbles(turn: CoachTurn) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // What the user said — right-aligned.
        Bubble(
            alignEnd = true,
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
        ) {
            Text(turn.userText, style = MaterialTheme.typography.bodyMedium)
        }
        // The Coach's reply — left-aligned.
        Bubble(
            alignEnd = false,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(turn.understanding, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                if (turn.result != "—") Text(turn.result, style = MaterialTheme.typography.bodyMedium)
                turn.observation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun Bubble(
    alignEnd: Boolean,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    body: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = container,
            contentColor = content,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (alignEnd) 16.dp else 4.dp,
                bottomEnd = if (alignEnd) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { body() }
        }
    }
}
