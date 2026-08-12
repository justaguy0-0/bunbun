package com.example.bunbun.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bunbun.R
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.ui.AppViewModel
import com.example.bunbun.ui.SessionState
import com.example.bunbun.ui.auth.AuthViewModel
import com.example.bunbun.ui.auth.LoginScreen
import com.example.bunbun.ui.auth.RegisterScreen
import com.example.bunbun.ui.chat.ChatScreen
import com.example.bunbun.ui.chat.ChatViewModel
import com.example.bunbun.ui.chats.ChatsScreen
import com.example.bunbun.ui.chats.ChatsViewModel
import com.example.bunbun.ui.common.ViewModelFactory
import com.example.bunbun.ui.common.TerminalScreen
import com.example.bunbun.ui.common.TerminalState
import com.example.bunbun.ui.common.localizedErrorMessage
import com.example.bunbun.ui.search.SearchScreen
import com.example.bunbun.ui.search.SearchViewModel

@Composable
fun BunbunApp(repository: BunbunRepository) {
    val factory = remember(repository) { ViewModelFactory { AppViewModel(repository) } }
    val appViewModel: AppViewModel = viewModel(factory = factory)
    when (val session = appViewModel.state.collectAsState().value) {
        SessionState.Checking -> LoadingScreen()
        SessionState.SignedOut -> AuthNavigation(repository, appViewModel::signedIn)
        is SessionState.SignedIn -> MainNavigation(repository, session.user, appViewModel::logout)
        is SessionState.Error -> StartupError(session.message, appViewModel::restore)
    }
}

@Composable
private fun AuthNavigation(repository: BunbunRepository, onSignedIn: (UserDto) -> Unit) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "login",
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        composable("login") {
            val vm: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(repository) })
            val state by vm.state.collectAsState()
            LoginScreen(state, vm::login, onSignedIn) { nav.navigate("register") }
        }
        composable("register") {
            val vm: AuthViewModel = viewModel(factory = ViewModelFactory { AuthViewModel(repository) })
            val state by vm.state.collectAsState()
            RegisterScreen(state, vm::register, onSignedIn) { nav.popBackStack() }
        }
    }
}

@Composable
private fun MainNavigation(repository: BunbunRepository, currentUser: UserDto, onLogout: () -> Unit) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "chats",
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        composable("chats") {
            val vm: ChatsViewModel = viewModel(factory = ViewModelFactory { ChatsViewModel(repository) })
            val state by vm.state.collectAsState()
            ChatsScreen(
                currentUser = currentUser,
                state = state,
                onRefresh = vm::refresh,
                onSearch = { nav.navigate("search") },
                onChat = { nav.navigate("chat/${it.id}/${Uri.encode(it.peer.displayName)}") },
                onLogout = onLogout,
            )
        }
        composable("search") {
            val vm: SearchViewModel = viewModel(factory = ViewModelFactory { SearchViewModel(repository) })
            val state by vm.state.collectAsState()
            SearchScreen(
                state = state,
                onSearch = vm::search,
                onCreate = { id, _, callback -> state.users.firstOrNull { it.id == id }?.let { vm.createDirect(it, callback) } },
                onChat = { nav.navigate("chat/${it.id}/${Uri.encode(it.peer.displayName)}") },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            route = "chat/{chatId}/{peerName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("peerName") { type = NavType.StringType },
            ),
        ) { entry ->
            val chatId = entry.arguments?.getLong("chatId") ?: return@composable
            val peerName = Uri.decode(entry.arguments?.getString("peerName").orEmpty())
            val vm: ChatViewModel = viewModel(
                key = "chat-$chatId",
                factory = ViewModelFactory { ChatViewModel(chatId, repository) },
            )
            val state by vm.state.collectAsState()
            ChatScreen(peerName, currentUser.id, state, vm::setDraft, vm::send, vm::retry) { nav.popBackStack() }
        }
    }
}

@Composable
private fun LoadingScreen() {
    TerminalScreen {
        TerminalState(
            code = stringResource(R.string.startup_code),
            title = stringResource(R.string.startup_title),
            message = stringResource(R.string.startup_message),
            loading = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StartupError(message: String, retry: () -> Unit) {
    TerminalScreen {
        TerminalState(
            code = stringResource(R.string.startup_error_code),
            title = stringResource(R.string.startup_error_title),
            message = localizedErrorMessage(message),
            actionLabel = stringResource(R.string.startup_retry),
            onAction = retry,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun forwardEnter(): EnterTransition = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 10 }
private fun forwardExit(): ExitTransition = fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 14 }
private fun backEnter(): EnterTransition = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { -it / 10 }
private fun backExit(): ExitTransition = fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 14 }
