package `in`.acstechnologies.nutritrainerai.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.usecase.DayTotals
import kotlinx.coroutines.launch
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.today_consumed
import nutritrainerai.shared.generated.resources.today_consumed_summary
import nutritrainerai.shared.generated.resources.today_empty
import nutritrainerai.shared.generated.resources.today_item_removed
import nutritrainerai.shared.generated.resources.today_planned_not_counted
import nutritrainerai.shared.generated.resources.today_remove_item
import nutritrainerai.shared.generated.resources.today_title
import nutritrainerai.shared.generated.resources.today_undo
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsState()
    TodayContent(state, viewModel::onIntent)
}

@Composable
private fun TodayContent(state: TodayUiState, onIntent: (TodayIntent) -> Unit) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(Res.string.today_undo)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.today_title), style = MaterialTheme.typography.headlineSmall)
            TotalsCard(state.totals)

            if (state.items.isEmpty()) {
                Text(
                    stringResource(Res.string.today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        MealRow(
                            item = item,
                            onRemove = {
                                onIntent(TodayIntent.Delete(item.id))
                                scope.launch {
                                    val result = snackbarHost.showSnackbar(
                                        message = getString(Res.string.today_item_removed, item.foodName),
                                        actionLabel = undoLabel,
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onIntent(TodayIntent.UndoDelete(item.id))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(totals: DayTotals) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.today_consumed), style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(
                    Res.string.today_consumed_summary,
                    totals.consumed.energyKcal.roundToInt(),
                    totals.consumed.proteinG.roundToInt(),
                ),
                fontWeight = FontWeight.SemiBold,
            )
            if (totals.plannedItemCount > 0) {
                Text(
                    stringResource(Res.string.today_planned_not_counted, totals.planned.energyKcal.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MealRow(item: MealItem, onRemove: () -> Unit) {
    val unresolved = item.confidence == ConfidenceBand.UNRESOLVED
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.foodName,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        item.quantityGrams?.let { "${it.roundToInt()} g" },
                        item.status.name.lowercase(),
                        item.confidence.name.lowercase().replace('_', ' '),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (unresolved) "—" else "${item.nutrients.energyKcal.roundToInt()} kcal",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = onRemove) {
                    Text(stringResource(Res.string.today_remove_item))
                }
            }
        }
    }
}
