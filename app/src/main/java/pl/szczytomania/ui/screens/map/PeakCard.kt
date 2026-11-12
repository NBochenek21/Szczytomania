package pl.szczytomania.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.location.SummitRules
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.Tag
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Karta szczytu z dołu mapy (mockup 02_Mapa_ekran_glowny). "Zdobądź szczyt" otwiera ekran zdobywania. */
@Composable
fun PeakCard(
    peak: Peak,
    conquered: Boolean,
    onConquer: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SzczytomaniaTheme.colors
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 20.dp),
    ) {
        // Uchwyt jak na mockupie – tylko ozdobny, kartę zamyka krzyżyk albo przycisk wstecz.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(end = 12.dp)
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.lines),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (peak.kgp) Tag(stringResource(R.string.peak_card_kgp), colors.trail)
                if (conquered) {
                    Tag(stringResource(R.string.peak_status_conquered), colors.conquered)
                } else {
                    Tag(stringResource(R.string.peak_status_to_conquer), MaterialTheme.colorScheme.onSurfaceVariant, dashed = true)
                }
            }
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close))
            }
        }

        Column(modifier = Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(peak.name, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.peak_card_elevation, peak.elevation),
                        style = NumberStyles.elevation,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " · ${peak.range}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DashedDivider()

            if (!conquered) {
                Button(
                    onClick = onConquer,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.conquered),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_flag), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.peak_card_conquer),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    text = stringResource(R.string.peak_card_rules, SummitRules.MAX_DISTANCE_METERS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
