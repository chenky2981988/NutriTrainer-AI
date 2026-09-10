package `in`.acstechnologies.nutritrainerai.ui.library

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.resolve.SeedFoodResolver
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.library_kcal_per_100g
import nutritrainerai.shared.generated.resources.library_search_foods
import nutritrainerai.shared.generated.resources.library_seed_note
import nutritrainerai.shared.generated.resources.library_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Library — search foods and recipes (PRD §4.5). Currently backed by the seed
 * table; the real multi-source catalogue arrives with the content pipeline.
 */
@Composable
fun LibraryScreen() {
    val foods = koinInject<SeedFoodResolver>()
    var query by remember { mutableStateOf("") }
    val all = remember { foods.all() }
    val results = all.filter { it.canonicalName.contains(query.trim().lowercase()) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.library_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(Res.string.library_search_foods)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(stringResource(Res.string.library_seed_note), style = MaterialTheme.typography.bodySmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.canonicalName }) { food ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(food.canonicalName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(
                                Res.string.library_kcal_per_100g,
                                food.nutrientsPerBase.energyKcal.roundToInt(),
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}
