package pl.szczytomania.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Edycja konta: nick, adres e-mail i hasło – każde w osobnej karcie z własnym przyciskiem. */
@Composable
fun EditAccountScreen(
    currentNick: String,
    onBack: () -> Unit,
    viewModel: EditAccountViewModel = viewModel(factory = EditAccountViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var nick by rememberSaveable { mutableStateOf(currentNick) }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var emailPassword by rememberSaveable { mutableStateOf("") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.account_title),
            navIcon = R.drawable.ic_arrow_back,
            navDescription = stringResource(R.string.back),
            onNavClick = onBack,
        )

        Section(stringResource(R.string.auth_nick), AccountSection.NICK, state) {
            Field(nick, { nick = it }, stringResource(R.string.auth_nick))
            Action(stringResource(R.string.account_nick_save), state.busy == AccountSection.NICK) { viewModel.saveNick(nick) }
        }

        Section(stringResource(R.string.auth_email), AccountSection.EMAIL, state) {
            Text(
                stringResource(R.string.account_email_current, state.email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Field(newEmail, { newEmail = it }, stringResource(R.string.account_email_new), keyboard = KeyboardType.Email)
            Field(emailPassword, { emailPassword = it }, stringResource(R.string.account_current_password), password = true)
            Action(stringResource(R.string.account_email_change), state.busy == AccountSection.EMAIL) {
                viewModel.changeEmail(newEmail, emailPassword)
                emailPassword = ""
            }
        }

        Section(stringResource(R.string.auth_password), AccountSection.PASSWORD, state) {
            Field(currentPassword, { currentPassword = it }, stringResource(R.string.account_current_password), password = true)
            Field(newPassword, { newPassword = it }, stringResource(R.string.account_password_new), password = true)
            Action(stringResource(R.string.account_password_change), state.busy == AccountSection.PASSWORD) {
                viewModel.changePassword(currentPassword, newPassword)
                currentPassword = ""
                newPassword = ""
            }
        }
    }
}

@Composable
private fun Section(title: String, section: AccountSection, state: EditAccountUiState, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().framed().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        content()
        state.message?.takeIf { it.section == section }?.let {
            Text(
                text = stringResource(it.text),
                style = MaterialTheme.typography.bodyMedium,
                color = if (it.success) SzczytomaniaTheme.colors.conquered else SzczytomaniaTheme.colors.trail,
            )
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = SzczytomaniaTheme.colors.conquered,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Action(text: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SzczytomaniaTheme.colors.conquered),
        modifier = Modifier.fillMaxWidth().height(50.dp),
    ) {
        Text(if (busy) stringResource(R.string.account_busy) else text, style = MaterialTheme.typography.titleMedium)
    }
}
