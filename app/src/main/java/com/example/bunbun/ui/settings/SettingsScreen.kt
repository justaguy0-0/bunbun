package com.example.bunbun.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.bunbun.AppLinks
import com.example.bunbun.BuildConfig
import com.example.bunbun.R
import com.example.bunbun.ui.common.PrimaryTerminalButton
import com.example.bunbun.ui.common.SecondaryTerminalButton
import com.example.bunbun.ui.common.TerminalError
import com.example.bunbun.ui.common.TerminalPanel
import com.example.bunbun.ui.common.TerminalScreen
import com.example.bunbun.ui.common.TerminalSectionLabel
import com.example.bunbun.ui.common.TerminalTextField
import com.example.bunbun.ui.common.TerminalTopBar

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onDismissName: () -> Unit,
    onRequestLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    onDismissLogout: () -> Unit,
) {
    val context = LocalContext.current
    var linkError by remember { mutableStateOf(false) }

    TerminalScreen {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TerminalTopBar(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TerminalSectionLabel(stringResource(R.string.settings_profile_section))
                TerminalPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.user.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_username, state.user.username),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SecondaryTerminalButton(
                            text = stringResource(R.string.settings_change_name),
                            onClick = onEditName,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                TerminalSectionLabel(stringResource(R.string.settings_app_section))
                TerminalPanel(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryTerminalButton(
                            text = stringResource(R.string.settings_github_releases),
                            onClick = {
                                linkError = try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.GITHUB_RELEASES_URL)))
                                    false
                                } catch (_: ActivityNotFoundException) {
                                    true
                                } catch (_: SecurityException) {
                                    true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            versionLabel(BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (linkError) TerminalError(stringResource(R.string.settings_link_error))
                    }
                }

                TerminalSectionLabel(stringResource(R.string.settings_account_section))
                TerminalPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.error.copy(alpha = 0.65f)) {
                    DestructiveTerminalButton(
                        text = stringResource(R.string.settings_logout),
                        onClick = onRequestLogout,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.showNameDialog) {
        Dialog(onDismissRequest = onDismissName) {
            TerminalPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_name_dialog_title), style = MaterialTheme.typography.titleMedium)
                    TerminalTextField(
                        value = state.displayNameDraft,
                        onValueChange = onNameChange,
                        label = stringResource(R.string.auth_display_name),
                        enabled = !state.savingName,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.profileError?.let { TerminalError(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryTerminalButton(
                            text = stringResource(R.string.settings_cancel),
                            onClick = onDismissName,
                            enabled = !state.savingName,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryTerminalButton(
                            text = stringResource(R.string.settings_save),
                            onClick = onSaveName,
                            loading = state.savingName,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (state.showLogoutDialog) {
        Dialog(onDismissRequest = onDismissLogout) {
            TerminalPanel(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.error) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_logout_dialog_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.pendingMessageCount?.takeIf { it > 0 }?.let {
                            stringResource(R.string.settings_logout_pending_warning, it)
                        } ?: stringResource(R.string.settings_logout_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.logoutError?.let { TerminalError(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryTerminalButton(
                            text = stringResource(R.string.settings_cancel),
                            onClick = onDismissLogout,
                            enabled = !state.loggingOut,
                            modifier = Modifier.weight(1f),
                        )
                        DestructiveTerminalButton(
                            text = stringResource(R.string.settings_logout_confirm),
                            onClick = onConfirmLogout,
                            enabled = !state.loggingOut,
                            loading = state.loggingOut,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun DestructiveTerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(if (loading) stringResource(R.string.settings_logging_out) else text.uppercase())
    }
}
