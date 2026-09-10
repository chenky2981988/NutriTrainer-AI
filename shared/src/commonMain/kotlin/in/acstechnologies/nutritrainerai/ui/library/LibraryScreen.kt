package `in`.acstechnologies.nutritrainerai.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.ConfidenceBand
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.model.SourceType
import `in`.acstechnologies.nutritrainerai.domain.repository.FoodRepository
import `in`.acstechnologies.nutritrainerai.domain.resolve.SeedFoodResolver
import `in`.acstechnologies.nutritrainerai.domain.usecase.AddUserFoodUseCase
import `in`.acstechnologies.nutritrainerai.ui.food.AddFoodSheet
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.library_add_food
import nutritrainerai.shared.generated.resources.library_built_in
import nutritrainerai.shared.generated.resources.library_kcal_per_100g
import nutritrainerai.shared.generated.resources.library_search_foods
import nutritrainerai.shared.generated.resources.library_title
import nutritrainerai.shared.generated.resources.library_your_foods
import nutritrainerai.shared.generated.resources.nutrient_carbs
import nutritrainerai.shared.generated.resources.nutrient_fat
import nutritrainerai.shared.generated.resources.nutrient_fibre
import nutritrainerai.shared.generated.resources.nutrient_grams
import nutritrainerai.shared.generated.resources.nutrient_protein
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** A flat food row for the list (user-confirmed or seed). */
private data class FoodListItem(
    val key: String,
    val name: String,
    val nutrientsPerBase: NutrientVector,
    val source: SourceType,
    val confidence: ConfidenceBand,
    val subtitle: String?,
)

/**
 * Library — search foods, see full macros, and add new ones to grow the local
 * DB (PRD §4.5). Backed by user-confirmed foods + the seed table for now.
 */
@Composable
fun LibraryScreen() {
    val seed = koinInject<SeedFoodResolver>()
    val foodRepo = koinInject<FoodRepository>()
    val addFood = koinInject<AddUserFoodUseCase>()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    val userFoods by remember {
        foodRepo.observeAll().map { list ->
            list.map {
                FoodListItem(
                    key = "u-${it.id}",
                    name = it.canonicalName,
                    nutrientsPerBase = it.nutrientsPerBase,
                    source = it.source,
                    confidence = it.confidence,
                    subtitle = listOfNotNull(it.brand, it.pack).joinToString(" · ").ifBlank { null },
                )
            }
        }
    }.collectAsState(emptyList())

    val seedFoods = remember {
        seed.all().map {
            FoodListItem("s-${it.canonicalName}", it.canonicalName, it.nutrientsPerBase, it.source, it.confidence, null)
        }
    }

    if (showAdd) {
        AddFoodSheet(
            prefillName = "",
            showEntryAmount = false,
            prefillAmount = 0.0,
            prefillUnit = "",
            onSave = {
                scope.launch { addFood.add(it) }
                showAdd = false
            },
            onCancel = { showAdd = false },
        )
        return
    }

    val q = query.trim().lowercase()
    val userMatches = userFoods.filter { it.name.lowercase().contains(q) }
    val seedMatches = seedFoods.filter { it.name.lowercase().contains(q) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(stringResource(Res.string.library_title), style = MaterialTheme.typography.headlineSmall)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.library_search_foods)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            ElevatedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.library_add_food))
            }
        }

        if (userMatches.isNotEmpty()) {
            item { SectionHeader(stringResource(Res.string.library_your_foods)) }
            items(userMatches, key = { it.key }) { FoodRow(it) }
        }
        item { SectionHeader(stringResource(Res.string.library_built_in)) }
        items(seedMatches, key = { it.key }) { FoodRow(it) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun FoodRow(item: FoodListItem) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium)
                    item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(
                        item.confidence.name.lowercase().replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    stringResource(Res.string.library_kcal_per_100g, item.nutrientsPerBase.energyKcal.roundToInt()),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    NutrientLine(stringResource(Res.string.nutrient_protein), item.nutrientsPerBase.proteinG)
                    NutrientLine(stringResource(Res.string.nutrient_carbs), item.nutrientsPerBase.carbohydrateG)
                    NutrientLine(stringResource(Res.string.nutrient_fat), item.nutrientsPerBase.fatG)
                    NutrientLine(stringResource(Res.string.nutrient_fibre), item.nutrientsPerBase.fibreG)
                    Text(
                        "per ${item.nutrientsPerBase.energyKcal.roundToInt()} kcal basis · source: " +
                            item.source.name.lowercase().replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NutrientLine(label: String, grams: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(Res.string.nutrient_grams, (grams * 10).roundToInt() / 10.0),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
