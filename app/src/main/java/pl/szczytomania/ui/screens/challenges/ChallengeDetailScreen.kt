package pl.szczytomania.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.CenteredMessage
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.sets.Counter
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Szczegóły wyzwania: rodzaj, stan wyścigu i ranking uczestników. Właściciel może je usunąć. */
@Composable
fun ChallengeDetailScreen(challengeId: String, onBack: () -> Unit, viewModel: ChallengesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = state.challenge(challengeId)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = view?.challenge?.setName.orEmpty(),
            overline = view?.let { modeLabel(it.challenge.mode) },
            subtitle = view?.let { statusLine(it) },
            navIcon = R.drawable.ic_arrow_back,
            navDescription = stringResource(R.string.back),
            onNavClick = onBack,
        )
        when {
            view != null -> Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.challenge_since, formatDay(view.challenge.createdAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.challenge_ranking).uppercase(),
                    style = NumberStyles.tag,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.fillMaxWidth().framed()) {
                    view.ranking.forEachIndexed { index, row ->
                        RankingRow(row, view.total)
                        if (index < view.ranking.lastIndex) DashedDivider(Modifier.padding(horizontal = 14.dp))
                    }
                }
                if (view.mine) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.challenge_delete), color = SzczytomaniaTheme.colors.trail)
                    }
                }
            }
            state.isLoading -> LoadingBox()
            // Wyzwanie usunięte przez właściciela, kiedy było otwarte.
            else -> CenteredMessage(stringResource(R.string.challenge_gone))
        }
    }

    if (confirmDelete && view != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.challenge_delete_confirm_title, view.challenge.setName)) },
            text = { Text(stringResource(R.string.challenge_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(challengeId)
                    onBack()
                }) { Text(stringResource(R.string.challenge_delete), color = SzczytomaniaTheme.colors.trail) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Miejsce, awatar, nick ("Ty" przy sobie), liczba szczytów i pasek postępu. */
@Composable
private fun RankingRow(row: RankRow, total: Int) {
    val nick = row.nick ?: stringResource(R.string.challenge_unknown)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (row.me) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${row.place}.",
            style = NumberStyles.counter,
            color = if (row.place == 1) SzczytomaniaTheme.colors.trail else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp),
        )
        Avatar(initial = nick.take(1).uppercase(), size = 36.dp, textStyle = MaterialTheme.typography.titleSmall)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (row.me) "$nick · ${stringResource(R.string.challenge_you)}" else nick,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (row.me) FontWeight.Bold else null,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ProgressBar(if (total == 0) 0f else row.done.toFloat() / total)
            row.completedAt?.let {
                Text(
                    stringResource(R.string.challenge_completed, formatDay(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = SzczytomaniaTheme.colors.conquered,
                )
            }
        }
        Counter(done = row.done, total = total)
    }
}
