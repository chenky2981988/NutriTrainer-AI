package `in`.acstechnologies.nutritrainerai.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.MeasurementBasis
import `in`.acstechnologies.nutritrainerai.domain.model.NutrientVector
import `in`.acstechnologies.nutritrainerai.domain.usecase.NewFoodDetails
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.addfood_basis
import nutritrainerai.shared.generated.resources.addfood_basis_100g
import nutritrainerai.shared.generated.resources.addfood_basis_100ml
import nutritrainerai.shared.generated.resources.addfood_basis_serving
import nutritrainerai.shared.generated.resources.addfood_brand
import nutritrainerai.shared.generated.resources.addfood_cancel
import nutritrainerai.shared.generated.resources.addfood_entry_amount
import nutritrainerai.shared.generated.resources.addfood_entry_label
import nutritrainerai.shared.generated.resources.addfood_entry_unit
import nutritrainerai.shared.generated.resources.addfood_kcal
import nutritrainerai.shared.generated.resources.addfood_name
import nutritrainerai.shared.generated.resources.addfood_pack
import nutritrainerai.shared.generated.resources.addfood_save
import nutritrainerai.shared.generated.resources.addfood_scan_hint
import nutritrainerai.shared.generated.resources.addfood_serving_grams
import nutritrainerai.shared.generated.resources.addfood_title
import nutritrainerai.shared.generated.resources.addfood_unknown_prompt
import nutritrainerai.shared.generated.resources.nutrient_carbs
import nutritrainerai.shared.generated.resources.nutrient_fat
import nutritrainerai.shared.generated.resources.nutrient_fibre
import nutritrainerai.shared.generated.resources.nutrient_protein
import org.jetbrains.compose.resources.stringResource

/**
 * Manual nutrition entry — the "teach the app a food" form (PRD §6 rank 1, §7
 * unknown-product workflow). Shared by Coach (re-resolving a pending entry) and
 * Library ([prefillName] null, [showEntryAmount] false). Photo/OCR prefill lands
 * on top of this later.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFoodSheet(
    prefillName: String,
    showEntryAmount: Boolean,
    prefillAmount: Double,
    prefillUnit: String,
    onSave: (NewFoodDetails) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(prefillName) }
    var brand by remember { mutableStateOf("") }
    var pack by remember { mutableStateOf("") }
    var basis by remember {
        mutableStateOf(if (prefillUnit == "ml") MeasurementBasis.PER_100_ML else MeasurementBasis.PER_100_G)
    }
    var servingGrams by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fibre by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(if (prefillAmount > 0) trim(prefillAmount) else "") }
    var unit by remember { mutableStateOf(prefillUnit.ifBlank { "g" }) }

    val canSave = name.isNotBlank() &&
        kcal.toDoubleOrNull() != null &&
        (!showEntryAmount || amount.toDoubleOrNull()?.let { it > 0 } == true)

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.addfood_title), style = MaterialTheme.typography.headlineSmall)
        if (prefillName.isNotBlank()) {
            Text(
                stringResource(Res.string.addfood_unknown_prompt, prefillName),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Field(stringResource(Res.string.addfood_name), name) { name = it }
        Field(stringResource(Res.string.addfood_brand), brand) { brand = it }
        Field(stringResource(Res.string.addfood_pack), pack) { pack = it }

        Text(stringResource(Res.string.addfood_basis), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasisChip(Res.string.addfood_basis_100g.let { stringResource(it) }, basis == MeasurementBasis.PER_100_G) {
                basis = MeasurementBasis.PER_100_G
            }
            BasisChip(stringResource(Res.string.addfood_basis_100ml), basis == MeasurementBasis.PER_100_ML) {
                basis = MeasurementBasis.PER_100_ML
            }
            BasisChip(stringResource(Res.string.addfood_basis_serving), basis == MeasurementBasis.PER_SERVING) {
                basis = MeasurementBasis.PER_SERVING
            }
        }
        if (basis == MeasurementBasis.PER_SERVING) {
            NumberField(stringResource(Res.string.addfood_serving_grams), servingGrams) { servingGrams = it }
        }

        NumberField(stringResource(Res.string.addfood_kcal), kcal) { kcal = it }
        NumberField(stringResource(Res.string.nutrient_protein), protein) { protein = it }
        NumberField(stringResource(Res.string.nutrient_carbs), carbs) { carbs = it }
        NumberField(stringResource(Res.string.nutrient_fat), fat) { fat = it }
        NumberField(stringResource(Res.string.nutrient_fibre), fibre) { fibre = it }

        if (showEntryAmount) {
            Text(stringResource(Res.string.addfood_entry_label), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(stringResource(Res.string.addfood_entry_amount), amount, Modifier.weight(1f)) { amount = it }
                Field(stringResource(Res.string.addfood_entry_unit), unit, Modifier.weight(1f)) { unit = it }
            }
        }

        Text(stringResource(Res.string.addfood_scan_hint), style = MaterialTheme.typography.bodySmall)

        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.addfood_cancel))
            }
            Button(
                onClick = {
                    onSave(
                        NewFoodDetails(
                            name = name.trim(),
                            brand = brand,
                            pack = pack,
                            basis = basis,
                            nutrientsPerBase = NutrientVector.ofKcal(
                                energyKcal = kcal.toDouble(),
                                proteinG = protein.toDoubleOrNull() ?: 0.0,
                                carbohydrateG = carbs.toDoubleOrNull() ?: 0.0,
                                fatG = fat.toDoubleOrNull() ?: 0.0,
                                fibreG = fibre.toDoubleOrNull() ?: 0.0,
                            ),
                            servingGrams = servingGrams.toDoubleOrNull(),
                            entryAmount = amount.toDoubleOrNull() ?: 0.0,
                            entryUnit = unit.trim(),
                        ),
                    )
                },
                enabled = canSave,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(Res.string.addfood_save)) }
        }
    }
}

@Composable
private fun BasisChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

private fun trim(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
