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
import kotlin.math.roundToInt

@Composable
fun CoachScreen(viewModel: CoachViewModel) {
    val state by viewModel.state.collectAsState()
    CoachContent(state, viewModel::onIntent)
}

@Composable
private fun CoachContent(state: CoachUiState, onIntent: (CoachIntent) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Coach", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${state.dayTotals.consumed.energyKcal.roundToInt()} kcal so far today",
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
                placeholder = { Text("What did you eat?") },
            )
            Button(
                onClick = { onIntent(CoachIntent.Submit) },
                enabled = state.composerText.isNotBlank() && !state.submitting,
            ) { Text("Send") }
        }
    }
}

@Composable
private fun TurnCard(turn: CoachTurn) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("You: ${turn.userText}", style = MaterialTheme.typography.bodySmall)
            Text(turn.understanding, fontWeight = FontWeight.Medium)
            Text(turn.result)
            turn.observation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
