package `in`.acstechnologies.nutritrainerai.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.Measurement
import `in`.acstechnologies.nutritrainerai.domain.repository.MeasurementRepository
import `in`.acstechnologies.nutritrainerai.domain.usecase.GetWeightTrendUseCase
import kotlinx.coroutines.launch
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_save
import nutritrainerai.shared.generated.resources.progress_kg
import nutritrainerai.shared.generated.resources.progress_need_readings
import nutritrainerai.shared.generated.resources.progress_seven_day_avg
import nutritrainerai.shared.generated.resources.progress_title
import nutritrainerai.shared.generated.resources.progress_weight_field
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/**
 * Progress — default is the seven-day weight average (PRD §4.4). A minimal
 * "add a reading" field until the full charts land.
 */
@Composable
fun ProgressScreen(day: Long) {
    val trendUseCase = koinInject<GetWeightTrendUseCase>()
    val measurements = koinInject<MeasurementRepository>()
    val scope = rememberCoroutineScope()

    var trend by remember { mutableStateOf<Double?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        trend = trendUseCase(asOfEpochDay = day)
        loaded = true
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.progress_title), style = MaterialTheme.typography.headlineSmall)

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(Res.string.progress_seven_day_avg), style = MaterialTheme.typography.labelMedium)
                Text(
                    when {
                        !loaded -> "…"
                        trend == null -> stringResource(Res.string.progress_need_readings)
                        else -> stringResource(
                            Res.string.progress_kg,
                            // Keep up to 3 decimals; Double.toString() trims trailing zeros.
                            ((trend ?: 0.0) * 1000).roundToInt() / 1000.0,
                        )
                    },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(Res.string.progress_weight_field)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                enabled = input.toDoubleOrNull()?.let { it in 20.0..400.0 } == true,
                onClick = {
                    val kg = input.toDouble()
                    scope.launch {
                        measurements.upsert(
                            Measurement(
                                id = "w-$day",
                                takenEpochDay = day,
                                weightKg = kg,
                                createdAtEpochMillis = day * 86_400_000L,
                            ),
                        )
                        input = ""
                        refresh++
                    }
                },
            ) { Text(stringResource(Res.string.action_save)) }
        }
    }
}
