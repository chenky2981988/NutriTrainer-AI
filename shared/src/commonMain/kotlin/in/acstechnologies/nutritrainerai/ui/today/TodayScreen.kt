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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.MealItem
import `in`.acstechnologies.nutritrainerai.domain.usecase.DayTotals
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.today_consumed
import nutritrainerai.shared.generated.resources.today_consumed_summary
import nutritrainerai.shared.generated.resources.today_planned_not_counted
import nutritrainerai.shared.generated.resources.today_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.state.collectAsState()
    TodayContent(state)
}

@Composable
private fun TodayContent(state: TodayUiState) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.today_title), style = MaterialTheme.typography.headlineSmall)
        TotalsCard(state.totals)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(state.items, key = { it.id }) { item -> MealRow(item) }
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
private fun MealRow(item: MealItem) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(item.foodName, fontWeight = FontWeight.Medium)
                Text(
                    listOfNotNull(
                        item.quantityGrams?.let { "${it.roundToInt()} g" },
                        item.status.name.lowercase(),
                        item.confidence.name.lowercase().replace('_', ' '),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("${item.nutrients.energyKcal.roundToInt()} kcal")
        }
    }
}
