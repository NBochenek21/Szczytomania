package pl.szczytomania.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R

/** Ekran logowania i rejestracji (mockup 01_Logowanie). Wygląd dopasujemy do motywu aplikacji, gdy powstanie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRegister = state.mode == AuthMode.REGISTER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.auth_tagline), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AuthMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, AuthMode.entries.size),
                    enabled = !state.isLoading,
                ) {
                    Text(stringResource(if (mode == AuthMode.LOGIN) R.string.auth_tab_login else R.string.auth_tab_register))
                }
            }
        }

        if (isRegister) {
            OutlinedTextField(
                value = state.nick,
                onValueChange = viewModel::setNick,
                label = { Text(stringResource(R.string.auth_nick)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::setEmail,
            label = { Text(stringResource(R.string.auth_email)) },
            placeholder = { Text(stringResource(R.string.auth_email_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::setPassword,
            label = { Text(stringResource(R.string.auth_password)) },
            placeholder = { Text(stringResource(R.string.auth_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }
        if (state.resetEmailSent) {
            Text(stringResource(R.string.auth_reset_sent, state.email.trim()))
        }

        Button(
            onClick = viewModel::submit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(if (isRegister) R.string.auth_submit_register else R.string.auth_submit_login))
            }
        }

        if (!isRegister) {
            TextButton(onClick = viewModel::sendPasswordReset, enabled = !state.isLoading) {
                Text(stringResource(R.string.auth_forgot_password))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.osm_attribution), style = MaterialTheme.typography.bodySmall)
    }
}
