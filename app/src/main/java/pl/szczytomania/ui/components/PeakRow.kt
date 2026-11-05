package pl.szczytomania.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/**
 * Wiersz szczytu (mockupy "Wszystkie szczyty" i "Szczegóły zestawu"): trójkąt, nazwa,
 * plakietka KGP, pasmo, wysokość i status.
 *
 * @param number numer w zestawie (1, 2, 3…) – tylko w szczegółach zestawu.
 * @param showToConquer czy pisać "do zdobycia" przy niezdobytych (w zestawie tak, na liście wszystkich nie).
 */
@Composable
fun PeakRow(
    peak: Peak,
    conquered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    showKgpBadge: Boolean = true,
    showToConquer: Boolean = false,
) {
    val status = when {
        conquered -> stringResource(R.string.peak_status_conquered)
        showToConquer -> stringResource(R.string.peak_status_to_conquer)
        else -> null
    }
    val elevation = stringResource(R.string.peaks_elevation, peak.elevation)

    // clickable łączy teksty wiersza w jeden element dla czytnika ekranu.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                style = NumberStyles.tag,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.width(32.dp),
            )
        }
        PeakTriangle(conquered = conquered)
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = peak.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (peak.kgp && showKgpBadge) {
                    Tag(
                        text = stringResource(R.string.peaks_kgp_badge),
                        color = SzczytomaniaTheme.colors.trail,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Text(
                text = peak.range,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
            Text(elevation, style = NumberStyles.elevation, color = MaterialTheme.colorScheme.onBackground)
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (conquered) SzczytomaniaTheme.colors.conquered else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
