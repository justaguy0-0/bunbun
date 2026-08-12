package com.example.bunbun.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.bunbun.R
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.ui.common.PrimaryTerminalButton
import com.example.bunbun.ui.common.SecondaryTerminalButton
import com.example.bunbun.ui.common.StatusDot
import com.example.bunbun.ui.common.TerminalError
import com.example.bunbun.ui.common.TerminalPanel
import com.example.bunbun.ui.common.TerminalScreen
import com.example.bunbun.ui.common.TerminalSectionLabel
import com.example.bunbun.ui.common.TerminalTextAction
import com.example.bunbun.ui.common.TerminalTextField

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String, (UserDto) -> Unit) -> Unit,
    onSuccess: (UserDto) -> Unit,
    onRegister: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var revealPassword by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val submit = { onLogin(username, password, onSuccess) }

    AuthFrame(
        node = stringResource(R.string.auth_login_node),
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle),
    ) {
        TerminalSectionLabel(stringResource(R.string.auth_identity_check), stringResource(R.string.auth_secure_session))
        Spacer(Modifier.height(18.dp))
        TerminalTextField(
            value = username,
            onValueChange = { username = it.take(32) },
            label = stringResource(R.string.auth_username),
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        TerminalTextField(
            value = password,
            onValueChange = { password = it.take(72) },
            label = stringResource(R.string.auth_password),
            enabled = !state.loading,
            visualTransformation = if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                TerminalTextAction(
                    if (revealPassword) stringResource(R.string.common_hide) else stringResource(R.string.common_show),
                    { revealPassword = !revealPassword },
                    !state.loading,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (username.isNotBlank() && password.isNotEmpty()) submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Spacer(Modifier.height(14.dp))
            TerminalError(it)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryTerminalButton(
            text = stringResource(R.string.auth_enter),
            onClick = submit,
            enabled = username.isNotBlank() && password.isNotEmpty(),
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        SecondaryTerminalButton(
            text = stringResource(R.string.auth_create_account),
            onClick = onRegister,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onRegister: (String, String, String, (UserDto) -> Unit) -> Unit,
    onSuccess: (UserDto) -> Unit,
    onBack: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var revealPassword by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val formValid = username.matches(Regex("^[a-z0-9_]{3,32}$")) && displayName.isNotBlank() && password.length in 8..72
    val submit = { onRegister(username, displayName, password, onSuccess) }

    AuthFrame(
        node = stringResource(R.string.auth_register_node),
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle),
    ) {
        TerminalSectionLabel(stringResource(R.string.auth_identity_profile), stringResource(R.string.auth_three_fields))
        Spacer(Modifier.height(18.dp))
        TerminalTextField(
            value = username,
            onValueChange = {
                username = it.lowercase().filter { char -> char in 'a'..'z' || char in '0'..'9' || char == '_' }.take(32)
            },
            label = stringResource(R.string.auth_username),
            supportingText = stringResource(R.string.auth_username_hint),
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TerminalTextField(
            value = displayName,
            onValueChange = { displayName = it.take(64) },
            label = stringResource(R.string.auth_display_name),
            supportingText = stringResource(R.string.auth_display_name_hint),
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        TerminalTextField(
            value = password,
            onValueChange = { password = it.take(72) },
            label = stringResource(R.string.auth_password),
            supportingText = stringResource(R.string.auth_password_hint),
            enabled = !state.loading,
            visualTransformation = if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                TerminalTextAction(
                    if (revealPassword) stringResource(R.string.common_hide) else stringResource(R.string.common_show),
                    { revealPassword = !revealPassword },
                    !state.loading,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (formValid) submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            TerminalError(it)
        }
        Spacer(Modifier.height(18.dp))
        PrimaryTerminalButton(
            text = stringResource(R.string.auth_register_action),
            onClick = submit,
            enabled = formValid,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        SecondaryTerminalButton(
            text = stringResource(R.string.auth_back_to_login),
            onClick = onBack,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AuthFrame(
    node: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    TerminalScreen {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatusDot()
                    Text(node, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.auth_brand),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(26.dp))
                TerminalPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = MaterialTheme.colorScheme.outline,
                ) { Column { content() } }
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.auth_tagline),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
