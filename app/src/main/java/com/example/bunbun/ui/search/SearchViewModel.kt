package com.example.bunbun.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bunbun.data.model.ChatDto
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.common.asUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val users: List<UserDto> = emptyList(),
    val loading: Boolean = false,
    val creatingFor: Long? = null,
    val error: String? = null,
)

class SearchViewModel(private val repository: BunbunRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    fun search(query: String) {
        if (query.trim().isEmpty()) return
        mutableState.value = SearchUiState(loading = true)
        viewModelScope.launch {
            runCatching { repository.searchUsers(query) }
                .onSuccess { mutableState.value = SearchUiState(users = it) }
                .onFailure { mutableState.value = SearchUiState(error = it.asUiError("SEARCH_UNKNOWN")) }
        }
    }

    fun createDirect(user: UserDto, onSuccess: (ChatDto) -> Unit) {
        mutableState.value = mutableState.value.copy(creatingFor = user.id, error = null)
        viewModelScope.launch {
            runCatching { repository.createDirect(user.id) }
                .onSuccess { mutableState.value = mutableState.value.copy(creatingFor = null); onSuccess(it) }
                .onFailure { mutableState.value = mutableState.value.copy(creatingFor = null, error = it.asUiError("CREATE_CHAT_UNKNOWN")) }
        }
    }
}
