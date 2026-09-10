package `in`.acstechnologies.nutritrainerai.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.ui.food.AddFoodSheet
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_send
import nutritrainerai.shared.generated.resources.coach_composer_placeholder
import nutritrainerai.shared.generated.resources.coach_kcal_so_far
import nutritrainerai.shared.generated.resources.coach_title
import nutritrainerai.shared.generated.resources.coach_you_prefix
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
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(Res.string.coach_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(Res.string.coach_kcal_so_far, state.dayTotals.consumed.energyKcal.roundToInt()),
            style = MaterialTheme.typography.bodySmall,
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.turns) { _, turn -> TurnCard(turn) }
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
private fun TurnCard(turn: CoachTurn) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(Res.string.coach_you_prefix, turn.userText),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(turn.understanding, fontWeight = FontWeight.Medium)
            Text(turn.result)
            turn.observation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
