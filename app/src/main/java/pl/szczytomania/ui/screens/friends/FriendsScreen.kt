package pl.szczytomania.ui.screens.friends

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.notifications.AskForNotificationsOnce
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.MountainThumb
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.SquareIconButton
import pl.szczytomania.ui.components.avatarColor
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.profile.avatarInitial
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Drobny tekst o liczbach: "31 szczytów · KGP 18/28". */
internal val monoSmall = NumberStyles.tag.copy(fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp)

/** Nagłówek sekcji: "OSTATNIO NA SZCZYTACH", "ZNAJOMI · 3". */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = NumberStyles.tag.copy(fontSize = 13.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp),
    )
}

/** Ekran "Znajomi" (mockup 08): zaproszenia, ostatnie zdobycze znajomych, lista znajomych. */
@Composable
fun FriendsScreen(onMenuClick: () -> Unit, onFriendClick: (String) -> Unit, viewModel: FriendsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AskForNotificationsOnce()
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ScreenHeader(
                title = stringResource(R.string.nav_friends),
                navIcon = R.drawable.ic_menu,
                navDescription = stringResource(R.string.menu_open),
                onNavClick = onMenuClick,
                modifier = Modifier.weight(1f),
            )
            SmallButton(stringResource(R.string.friends_add), onClick = { adding = true }, icon = R.drawable.ic_person_add)
        }

        if (state.loading) {
            LoadingBox()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.invitations, key = { "inv-" + it.friendship.id }) { item ->
                    InvitationCard(item, onAccept = { viewModel.accept(item.friendship) }, onReject = { viewModel.remove(item.friendship) })
                }
                if (state.recent.isNotEmpty()) {
                    item(key = "recent") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionLabel(stringResource(R.string.friends_recent))
                            RecentCard(state.recent, onFriendClick)
                        }
                    }
                }
                item(key = "friends-label") { SectionLabel(stringResource(R.string.friends_list, state.friends.size)) }
                if (state.friends.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            stringResource(R.string.friends_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.friends, key = { "friend-" + it.friendship.id }) { item ->
                    FriendRow(item, onClick = { onFriendClick(item.friendship.friendUid) })
                }
                if (state.sent.isNotEmpty()) {
                    item(key = "sent-label") { SectionLabel(stringResource(R.string.friends_sent)) }
                    items(state.sent, key = { "sent-" + it.friendship.id }) { item ->
                        SentRow(item, onCancel = { viewModel.remove(item.friendship) })
                    }
                }
            }
        }
    }

    if (adding) AddFriendDialog(viewModel, myNick = state.myNick, onDismiss = { adding = false })
}

/** "31 szczytów · KGP 18/28". */
@Composable
private fun summaryText(summary: FriendSummary): String {
    val peaks = "${summary.count} ${pluralStringResource(R.plurals.profile_peaks, summary.count)}"
    return summary.kgp?.let { "$peaks · KGP ${it.done}/${it.total}" } ?: peaks
}

/** Zaproszenie do znajomych (czerwona karta z mockupu). */
@Composable
private fun InvitationCard(item: FriendItem, onAccept: () -> Unit, onReject: () -> Unit) {
    val trail = SzczytomaniaTheme.colors.trail
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(trail.copy(alpha = 0.15f))
            .border(1.dp, trail, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatarInitial(item.nick), size = 52.dp, textStyle = MaterialTheme.typography.titleLarge, color = trail)
            Column(Modifier.padding(start = 14.dp)) {
                Text(item.nick, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                val body = stringResource(R.string.friends_invitation_body)
                Text(
                    text = item.summary?.let { "$body · ${summaryText(it)}" } ?: body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallButton(stringResource(R.string.friends_accept), onClick = onAccept, modifier = Modifier.weight(1f))
            SmallButton(stringResource(R.string.friends_reject), onClick = onReject, modifier = Modifier.weight(1f), filled = false)
        }
    }
}

/** "Ostatnio na szczytach": kto, jaki szczyt i kiedy. */
@Composable
private fun RecentCard(recent: List<RecentConquest>, onFriendClick: (String) -> Unit) {
    val now = remember { Date() }
    Column(Modifier.fillMaxWidth().framed()) {
        recent.forEachIndexed { index, item ->
            if (index > 0) DashedDivider(Modifier.padding(horizontal = 16.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onFriendClick(item.friendUid) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MountainThumb(item.peak.id)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    val verb = stringResource(R.string.friends_conquered_verb)
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(item.nick) }
                            append(" $verb")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            item.peak.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            stringResource(R.string.peaks_elevation, item.peak.elevation),
                            style = monoSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                        )
                    }
                }
                Text(
                    relativeDay(item.date, now),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** "dziś", "wczoraj", "3 dni temu", a starsze – data. */
@Composable
private fun relativeDay(date: Date, now: Date): String = when (val days = daysAgo(date, now)) {
    0L -> stringResource(R.string.friends_today)
    1L -> stringResource(R.string.friends_yesterday)
    in 2L..6L -> stringResource(R.string.friends_days_ago, days.toInt())
    else -> remember(date) { SimpleDateFormat("dd.MM", Locale.forLanguageTag("pl")).format(date) }
}

/** Znajomy na liście: awatar, nick, liczba szczytów i Korona; dotknięcie otwiera jego profil. */
@Composable
private fun FriendRow(item: FriendItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .framed(borderWidth = 1.dp)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(avatarInitial(item.nick), size = 52.dp, textStyle = MaterialTheme.typography.titleLarge, color = avatarColor(item.friendship.friendUid))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(item.nick, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            item.summary?.let { Text(summaryText(it), style = monoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Wysłane zaproszenie – czeka na drugą osobę, można je anulować. */
@Composable
private fun SentRow(item: FriendItem, onCancel: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().framed().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(avatarInitial(item.nick), size = 44.dp, textStyle = MaterialTheme.typography.titleMedium, color = avatarColor(item.friendship.friendUid))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(item.nick, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.friends_sent_waiting), style = monoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SmallButton(stringResource(R.string.cancel), onClick = onCancel, filled = false)
    }
}

/** Okienko "Dodaj znajomego" (mockup 10): wyszukiwarka po nicku i Twój nick do udostępnienia. */
@Composable
private fun AddFriendDialog(viewModel: FriendsViewModel, myNick: String, onDismiss: () -> Unit) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareText = stringResource(R.string.add_friend_share_text, myNick)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .framed(radius = 22.dp, borderWidth = 1.5.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_friend_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.add_friend_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SquareIconButton(R.drawable.ic_close, stringResource(R.string.close), onDismiss)
            }

            OutlinedTextField(
                value = search.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.add_friend_placeholder)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            when {
                search.searching -> Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
                search.failed -> HintText(stringResource(R.string.add_friend_error))
                search.query.trim().length >= FriendsViewModel.MIN_QUERY && search.hits.isEmpty() ->
                    HintText(stringResource(R.string.add_friend_no_results, search.query.trim()))
                else -> Column {
                    search.hits.forEach { hit ->
                        SearchRow(
                            hit,
                            onInvite = { viewModel.invite(hit.uid) },
                            onAccept = { hit.friendship?.let(viewModel::accept) },
                        )
                        DashedDivider()
                    }
                }
            }

            // Twój nick – do podania znajomym, z systemowym "Udostępnij" (SMS, Messenger…).
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SzczytomaniaTheme.colors.lines.copy(alpha = 0.45f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_friend_my_nick), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(myNick, style = NumberStyles.counter, color = MaterialTheme.colorScheme.onSurface)
                }
                SmallButton(
                    stringResource(R.string.add_friend_share),
                    icon = R.drawable.ic_share,
                    filled = false,
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        context.startActivity(Intent.createChooser(send, null))
                    },
                )
            }
            HintText(stringResource(R.string.add_friend_footer))
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Osoba w wyszukiwarce: Zaproś / Akceptuj albo opis (już znajomy, zaproszenie wysłane). */
@Composable
private fun SearchRow(hit: SearchHit, onInvite: () -> Unit, onAccept: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(avatarInitial(hit.nick), size = 44.dp, textStyle = MaterialTheme.typography.titleMedium, color = avatarColor(hit.uid))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(hit.nick, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val status = when (hit.relation) {
                Relation.FRIEND -> stringResource(R.string.add_friend_already)
                Relation.INVITED -> stringResource(R.string.add_friend_invited)
                Relation.INVITES_YOU -> stringResource(R.string.add_friend_invites_you)
                Relation.NONE -> null
            }
            status?.let { Text(it, style = monoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        when (hit.relation) {
            Relation.NONE -> SmallButton(stringResource(R.string.add_friend_invite), onClick = onInvite)
            Relation.INVITES_YOU -> SmallButton(stringResource(R.string.friends_accept), onClick = onAccept)
            else -> Unit
        }
    }
}

/** Mały przycisk z mockupów: zielony wypełniony (Zaproś, Akceptuj, Dodaj) albo w ramce (Odrzuć, Udostępnij). */
@Composable
internal fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    icon: Int? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val green = SzczytomaniaTheme.colors.conquered
    val content = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .clip(shape)
            .background(if (filled) green else Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}
