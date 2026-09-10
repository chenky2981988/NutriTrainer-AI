package `in`.acstechnologies.nutritrainerai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.acstechnologies.nutritrainerai.domain.model.ThemePreference
import `in`.acstechnologies.nutritrainerai.domain.repository.AppSettingsRepository
import kotlinx.coroutines.launch
import nutritrainerai.shared.generated.resources.Res
import nutritrainerai.shared.generated.resources.action_back
import nutritrainerai.shared.generated.resources.settings_appearance
import nutritrainerai.shared.generated.resources.settings_title
import nutritrainerai.shared.generated.resources.theme_dark
import nutritrainerai.shared.generated.resources.theme_light
import nutritrainerai.shared.generated.resources.theme_system
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Settings — reached from Profile. Currently just Appearance; units, notifications
 * and backup land here later.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings = koinInject<AppSettingsRepository>()
    val scope = rememberCoroutineScope()
    val current by settings.themePreference().collectAsState(ThemePreference.DEFAULT)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(Res.string.action_back)) }
            Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        }

        Text(
            stringResource(Res.string.settings_appearance),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(Modifier.selectableGroup().fillMaxWidth()) {
            ThemePreference.entries.forEach { pref ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current == pref,
                            onClick = { scope.launch { settings.setThemePreference(pref) } },
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = current == pref, onClick = null)
                    Text(
                        stringResource(
                            when (pref) {
                                ThemePreference.SYSTEM -> Res.string.theme_system
                                ThemePreference.LIGHT -> Res.string.theme_light
                                ThemePreference.DARK -> Res.string.theme_dark
                            },
                        ),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
