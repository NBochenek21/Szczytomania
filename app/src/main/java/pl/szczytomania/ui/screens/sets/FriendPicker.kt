package pl.szczytomania.ui.screens.sets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.szczytomania.R
import pl.szczytomania.ui.components.PillButton

/** Znajomi do zaznaczenia jako "pigułki" – komu udostępnić zestaw. Bez znajomych – podpowiedź, gdzie ich dodać. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendPicker(friends: List<Pair<String, String>>, selected: List<String>, onToggle: (String) -> Unit) {
    if (friends.isEmpty()) {
        Text(
            stringResource(R.string.set_share_no_friends),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        friends.forEach { (uid, nick) -> PillButton(nick, selected = uid in selected, onClick = { onToggle(uid) }) }
    }
}

/** Okno udostępniania istniejącego zestawu: zaznacz znajomych i zapisz. */
@Composable
fun ShareSetDialog(
    friends: List<Pair<String, String>>,
    initial: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_share_title)) },
        text = {
            Column {
                FriendPicker(friends, selected) { uid -> selected = if (uid in selected) selected - uid else selected + uid }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }, enabled = friends.isNotEmpty()) { Text(stringResource(R.string.set_share_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
