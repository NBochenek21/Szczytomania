package pl.szczytomania.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.data.firestore.ChallengeMode
import pl.szczytomania.data.firestore.ChallengeRepository
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.PillButton
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.SegmentedTabs
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.sets.FriendPicker
import pl.szczytomania.ui.screens.sets.SetsViewModel
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Nowe wyzwanie: zestaw (gotowy, własny albo od znajomego), rodzaj wyścigu, czas trwania i znajomi. */
@Composable
fun NewChallengeScreen(
    onClose: () -> Unit,
    viewModel: ChallengesViewModel,
    setsViewModel: SetsViewModel = viewModel(factory = SetsViewModel.Factory),
) {
    val sets by setsViewModel.state.collectAsStateWithLifecycle()
    var setId by rememberSaveable { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf(ChallengeMode.FIRST) }
    var length by rememberSaveable { mutableStateOf(ChallengeLength.TWO_WEEKS) }
    var friends by rememberSaveable { mutableStateOf(listOf<String>()) }
    val green = SzczytomaniaTheme.colors.conquered
    val maxFriends = ChallengeRepository.MAX_PARTICIPANTS - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.challenges_new),
            navIcon = R.drawable.ic_close,
            navDescription = stringResource(R.string.close),
            onNavClick = onClose,
        )
        if (sets.isLoading) {
            Box(Modifier.weight(1f)) { LoadingBox() }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Label(stringResource(R.string.new_challenge_mode))
                SegmentedTabs(
                    labels = listOf(stringResource(R.string.challenge_mode_first), stringResource(R.string.challenge_mode_most)),
                    selectedIndex = mode.ordinal,
                    onSelect = { mode = ChallengeMode.entries[it] },
                )
                if (mode == ChallengeMode.MOST) {
                    Label(stringResource(R.string.new_challenge_length))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChallengeLength.entries.forEach { l ->
                            PillButton(lengthLabel(l), selected = l == length, onClick = { length = l })
                        }
                    }
                }
                Label(stringResource(R.string.new_challenge_set))
                Column(Modifier.fillMaxWidth().framed()) {
                    val all = sets.sets + sets.own + sets.sharedWithMe
                    all.forEachIndexed { index, progress ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { setId = progress.set.id }.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = progress.set.id == setId,
                                onClick = { setId = progress.set.id },
                                colors = RadioButtonDefaults.colors(selectedColor = green),
                            )
                            Text(progress.set.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Text("${progress.total}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                        }
                        if (index < all.lastIndex) DashedDivider(Modifier.padding(horizontal = 12.dp))
                    }
                }
                Label(stringResource(R.string.new_challenge_friends))
                FriendPicker(sets.friends, friends) { uid ->
                    friends = when {
                        uid in friends -> friends - uid
                        friends.size < maxFriends -> friends + uid
                        else -> friends
                    }
                }
            }
        }
        val set = setId?.let { sets.set(it) }
        val valid = set != null && set.total > 0 && friends.isNotEmpty()
        Button(
            onClick = {
                if (set != null) viewModel.create(set, mode, length, friends)
                onClose()
            },
            enabled = valid,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = green),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                stringResource(if (valid) R.string.new_challenge_save else R.string.new_challenge_save_empty),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun lengthLabel(length: ChallengeLength) = stringResource(
    when (length) {
        ChallengeLength.WEEK -> R.string.new_challenge_week
        ChallengeLength.TWO_WEEKS -> R.string.new_challenge_two_weeks
        ChallengeLength.MONTH -> R.string.new_challenge_month
    },
)

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
