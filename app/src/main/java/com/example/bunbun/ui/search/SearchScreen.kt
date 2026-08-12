package com.example.bunbun.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bunbun.R
import com.example.bunbun.data.model.ChatDto
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.ui.common.PressableTerminalRow
import com.example.bunbun.ui.common.TerminalError
import com.example.bunbun.ui.common.TerminalScreen
import com.example.bunbun.ui.common.TerminalSectionLabel
import com.example.bunbun.ui.common.TerminalState
import com.example.bunbun.ui.common.TerminalTextAction
import com.example.bunbun.ui.common.TerminalTextField
import com.example.bunbun.ui.common.TerminalTopBar

@Composable
fun SearchScreen(
    state: SearchUiState,
    onSearch: (String) -> Unit,
    onCreate: (Long, String, (ChatDto) -> Unit) -> Unit,
    onChat: (ChatDto) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var hasSearched by rememberSaveable { mutableStateOf(false) }
    val submitSearch = {
        if (query.isNotBlank()) {
            hasSearched = true
            onSearch(query)
        }
    }

    TerminalScreen {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TerminalTopBar(
                title = stringResource(R.string.search_title),
                subtitle = stringResource(R.string.search_subtitle),
                onBack = onBack,
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                TerminalSectionLabel(
                    stringResource(R.string.search_parameters),
                    stringResource(R.string.search_max_chars),
                )
                Spacer(Modifier.height(10.dp))
                TerminalTextField(
                    value = query,
                    onValueChange = {
                        query = it.take(64)
                        if (it.isBlank()) hasSearched = false
                    },
                    label = stringResource(R.string.search_field),
                    trailingContent = {
                        TerminalTextAction(
                            stringResource(R.string.search_run),
                            submitSearch,
                            query.isNotBlank() && !state.loading,
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                    enabled = state.creatingFor == null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                state.loading -> TerminalState(
                    code = stringResource(R.string.search_loading_code),
                    title = stringResource(R.string.search_loading_title),
                    message = stringResource(R.string.search_loading_message),
                    loading = true,
                    modifier = Modifier.fillMaxSize(),
                )

                state.error != null -> Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    TerminalError(state.error)
                    TerminalState(
                        code = stringResource(R.string.search_error_code),
                        title = stringResource(R.string.search_error_title),
                        message = stringResource(R.string.search_error_message),
                        actionLabel = stringResource(R.string.search_retry),
                        onAction = submitSearch,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                !hasSearched -> TerminalState(
                    code = stringResource(R.string.search_idle_code),
                    title = stringResource(R.string.search_idle_title),
                    message = stringResource(R.string.search_idle_message),
                    modifier = Modifier.fillMaxSize(),
                )

                state.users.isEmpty() -> TerminalState(
                    code = stringResource(R.string.search_empty_code),
                    title = stringResource(R.string.search_empty_title),
                    message = stringResource(R.string.search_empty_message),
                    modifier = Modifier.fillMaxSize(),
                )

                else -> {
                    TerminalSectionLabel(
                        label = stringResource(R.string.search_matches),
                        detail = state.users.size.toString().padStart(2, '0'),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.users, key = { it.id }) { user ->
                            SearchResultRow(
                                user = user,
                                loading = state.creatingFor == user.id,
                                enabled = state.creatingFor == null,
                                onClick = { onCreate(user.id, user.displayName, onChat) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(user: UserDto, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    PressableTerminalRow(onClick = onClick, enabled = enabled) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        user.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        user.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "@${user.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        stringResource(R.string.search_message_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)))
        }
    }
}
