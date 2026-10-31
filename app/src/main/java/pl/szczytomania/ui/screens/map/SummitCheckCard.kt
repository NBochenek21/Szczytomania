package pl.szczytomania.ui.screens.map

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.location.SummitCheck
import pl.szczytomania.location.SummitCheckResult
import pl.szczytomania.location.SummitRules
import kotlin.math.ceil

/**
 * Prosta karta wybranego szczytu z wynikiem sprawdzania GPS. Pełną kartę i ekran zdobywania
 * (zdjęcie, zapis) dodaje interfejs – wystarczy przenieść tam [SummitCheckDetails].
 */
@Composable
fun SummitCheckCard(
    peak: Peak,
    state: SummitCheckState,
    onCheck: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(peak.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${stringResource(R.string.peaks_elevation, peak.elevation)} · ${peak.range}",
                style = MaterialTheme.typography.bodySmall,
            )

            when (state) {
                SummitCheckState.Idle -> Unit
                SummitCheckState.Checking -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.summit_checking), style = MaterialTheme.typography.bodyMedium)
                }
                is SummitCheckState.Done -> when (val result = state.result) {
                    is SummitCheckResult.Checked -> SummitCheckDetails(result.check)
                    SummitCheckResult.NoPreciseLocation -> Problem(R.string.location_permission_approximate)
                    SummitCheckResult.LocationOff -> Problem(R.string.summit_location_off)
                    SummitCheckResult.NoSignal -> Problem(R.string.summit_no_signal)
                }
            }

            Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.summit_close)) }
                Button(onClick = onCheck, enabled = state != SummitCheckState.Checking) {
                    Text(
                        stringResource(
                            if (state is SummitCheckState.Done) R.string.summit_check_again else R.string.summit_check,
                        ),
                    )
                }
            }
        }
    }
}

/** Trzy warunki zaliczenia szczytu, jak na mockupie "Zdobywanie – błąd". */
@Composable
fun SummitCheckDetails(check: SummitCheck, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(check.headline()),
            style = MaterialTheme.typography.titleSmall,
            color = if (check.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        ConditionRow(
            ok = check.inRange,
            label = stringResource(R.string.summit_distance),
            value = stringResource(
                R.string.summit_distance_value,
                roundedUp(check.distanceMeters),
                SummitRules.MAX_DISTANCE_METERS,
            ),
        )
        ConditionRow(
            ok = check.accurate,
            label = stringResource(R.string.summit_accuracy),
            value = stringResource(
                R.string.summit_accuracy_value,
                roundedUp(check.reading.accuracyMeters.toDouble()),
                SummitRules.MAX_ACCURACY_METERS,
            ),
        )
        ConditionRow(
            ok = check.genuine,
            label = stringResource(if (check.genuine) R.string.summit_not_mocked else R.string.summit_mocked),
        )
    }
}

// Niedokładny odczyt mówi niewiele o odległości, więc jego problem pokazujemy przed nią.
@StringRes
private fun SummitCheck.headline() = when {
    !genuine -> R.string.summit_headline_mocked
    !accurate -> R.string.summit_headline_inaccurate
    !inRange -> R.string.summit_headline_too_far
    else -> R.string.summit_headline_passed
}

// W górę, żeby 150,4 m nie wyglądało na spełniony limit "150 / 150 m".
private fun roundedUp(meters: Double) = ceil(meters).toLong()

@Composable
private fun ConditionRow(ok: Boolean, label: String, value: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (ok) "✓" else "✕",
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Problem(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}
