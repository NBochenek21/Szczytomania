package pl.szczytomania.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.data.firestore.ChallengeMode
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.Tag
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.sets.Counter
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ekran "Wyzwania": wyścigi ze znajomymi – rodzaj, zestaw, moje miejsce i postęp. */
@Composable
fun ChallengesScreen(
    onMenuClick: () -> Unit,
    onChallengeClick: (String) -> Unit,
    onCreate: () -> Unit,
    viewModel: ChallengesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        ScreenHeader(
            title = stringResource(R.string.nav_challenges),
            navIcon = R.drawable.ic_menu,
            navDescription = stringResource(R.string.menu_open),
            onNavClick = onMenuClick,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        if (state.isLoading) {
            LoadingBox()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Button(
                        onClick = onCreate,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SzczytomaniaTheme.colors.conquered),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("+  " + stringResource(R.string.challenges_new), style = MaterialTheme.typography.titleMedium) }
                }
                if (state.challenges.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.challenges_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.challenges, key = { it.challenge.id }) { ChallengeCard(it, onClick = { onChallengeClick(it.challenge.id) }) }
            }
        }
    }
}

@Composable
private fun ChallengeCard(view: ChallengeView, onClick: () -> Unit) {
    val me = view.me
    Column(
        modifier = Modifier.fillMaxWidth().framed().clickable(onClick = onClick).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(modeLabel(view.challenge.mode), SzczytomaniaTheme.colors.trail)
            Text(
                statusLine(view),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                view.challenge.setName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Counter(done = me?.done ?: 0, total = view.total, modifier = Modifier.padding(start = 12.dp))
        }
        ProgressBar(if (view.total == 0) 0f else (me?.done ?: 0).toFloat() / view.total)
        if (me != null) {
            Text(
                stringResource(R.string.challenge_my_place, me.place, view.ranking.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun modeLabel(mode: ChallengeMode) = stringResource(
    when (mode) {
        ChallengeMode.FIRST -> R.string.challenge_mode_first
        ChallengeMode.MOST -> R.string.challenge_mode_most
    },
)

/** "Do 15.12.2026" / "Trwa" / "Wygrywa: Jakub" / "Wygrywasz!". */
@Composable
internal fun statusLine(view: ChallengeView): String {
    val winner = view.winner
    return when {
        winner?.me == true -> stringResource(R.string.challenge_you_win)
        winner != null -> stringResource(R.string.challenge_winner, winner.nick ?: stringResource(R.string.challenge_unknown))
        view.finished -> stringResource(R.string.challenge_finished)
        view.challenge.endsAt != null -> stringResource(R.string.challenge_until, formatDay(lastDay(view.challenge.endsAt)))
        else -> stringResource(R.string.challenge_running)
    }
}

/** Ostatni dzień wyścigu – koniec jest o północy następnego dnia. */
private fun lastDay(endsAt: Date) = Date(endsAt.time - 1)

internal fun formatDay(date: Date): String = SimpleDateFormat("d.MM.yyyy", Locale("pl")).format(date)
