package com.example.bunbun.navigation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bunbun.R
import com.example.bunbun.AppContainer
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
import com.example.bunbun.ui.common.TerminalPanel
import com.example.bunbun.ui.common.PrimaryTerminalButton
import com.example.bunbun.ui.common.SecondaryTerminalButton
import com.example.bunbun.ui.common.TerminalState
import com.example.bunbun.ui.common.localizedErrorMessage
import com.example.bunbun.ui.search.SearchScreen
import com.example.bunbun.ui.search.SearchViewModel
import com.example.bunbun.ui.settings.SettingsScreen
import com.example.bunbun.ui.settings.SettingsViewModel

@Composable
fun BunbunApp(container: AppContainer) {
    val repository = container.repository
    val factory = remember(repository, container.logoutCoordinator) {
        ViewModelFactory { AppViewModel(repository, container.logoutCoordinator) }
    }
    val appViewModel: AppViewModel = viewModel(factory = factory)
    when (val session = appViewModel.state.collectAsState().value) {
        SessionState.Checking -> LoadingScreen()
        SessionState.SignedOut -> {
            LaunchedEffect(Unit) { container.pendingChatNavigation.clear() }
            AuthNavigation(repository, appViewModel::signedIn)
        }
        is SessionState.SignedIn -> {
            LaunchedEffect(session.user.id) { container.presenceSynchronizer.onAuthenticated() }
            MainNavigation(
                repository,
                session.user,
                appViewModel::profileUpdated,
                appViewModel::logout,
                container.foregroundChatTracker,
                container.pendingChatNavigation,
            )
            NotificationPermissionPrompt(container.notificationPermissionPreferences)
        }
        is SessionState.Error -> StartupError(session.message, appViewModel::restore)
    }
}

@Composable
private fun AuthNavigation(repository: BunbunRepository, onSignedIn: (UserDto) -> Unit) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "login",
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
private fun MainNavigation(
    repository: BunbunRepository,
    currentUser: UserDto,
    onProfileUpdated: (UserDto) -> Unit,
    onLogout: suspend () -> Unit,
    foregroundChatTracker: com.example.bunbun.push.ForegroundChatTracker,
    pendingChatNavigation: com.example.bunbun.push.PendingChatNavigation,
) {
    val nav = rememberNavController()
    val pendingTarget by pendingChatNavigation.target.collectAsState()
    LaunchedEffect(pendingTarget) {
        pendingTarget?.let { target ->
            nav.navigate(chatRoute(target.chatId, target.peerName, fromPush = true)) {
                launchSingleTop = true
                popUpTo(CHATS_ROUTE) { inclusive = false }
            }
            pendingChatNavigation.consume(target)
        }
    }
    NavHost(
        navController = nav,
        startDestination = CHATS_ROUTE,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        enterTransition = {
            when {
                targetState.arguments?.getBoolean(CHAT_FROM_PUSH_ARG) == true -> pushChatEnter()
                classifyMainNavigationTransition(initialState.destination.route, targetState.destination.route) ==
                    MainNavigationTransition.CHAT_FORWARD -> chatForwardEnter()
                else -> forwardEnter()
            }
        },
        exitTransition = {
            when {
                targetState.arguments?.getBoolean(CHAT_FROM_PUSH_ARG) == true -> pushChatExit()
                classifyMainNavigationTransition(initialState.destination.route, targetState.destination.route) ==
                    MainNavigationTransition.CHAT_FORWARD -> chatsForwardExit()
                else -> forwardExit()
            }
        },
        popEnterTransition = {
            if (classifyMainNavigationTransition(initialState.destination.route, targetState.destination.route) ==
                MainNavigationTransition.CHAT_BACK
            ) chatsBackEnter() else backEnter()
        },
        popExitTransition = {
            if (classifyMainNavigationTransition(initialState.destination.route, targetState.destination.route) ==
                MainNavigationTransition.CHAT_BACK
            ) chatBackExit() else backExit()
        },
    ) {
        composable(CHATS_ROUTE) {
            val vm: ChatsViewModel = viewModel(factory = ViewModelFactory { ChatsViewModel(currentUser.id, repository) })
            val state by vm.state.collectAsState()
            ChatsScreen(
                currentUser = currentUser,
                state = state,
                onRefresh = vm::refresh,
                onSearch = { nav.navigate(SEARCH_ROUTE) },
                onSettings = { nav.navigate(SETTINGS_ROUTE) },
                onChat = { nav.navigate(chatRoute(it.id, it.peerDisplayName)) },
            )
        }
        composable(SETTINGS_ROUTE) {
            val vm: SettingsViewModel = viewModel(
                factory = ViewModelFactory {
                    SettingsViewModel(currentUser, repository, onProfileUpdated, onLogout)
                },
            )
            val state by vm.state.collectAsState()
            SettingsScreen(
                state = state,
                onBack = { nav.popBackStack() },
                onEditName = vm::showNameDialog,
                onNameChange = vm::changeDisplayName,
                onSaveName = vm::saveDisplayName,
                onDismissName = vm::dismissNameDialog,
                onRequestLogout = vm::showLogoutDialog,
                onConfirmLogout = vm::confirmLogout,
                onDismissLogout = vm::dismissLogoutDialog,
            )
        }
        composable(SEARCH_ROUTE) {
            val vm: SearchViewModel = viewModel(factory = ViewModelFactory { SearchViewModel(repository) })
            val state by vm.state.collectAsState()
            SearchScreen(
                state = state,
                onSearch = vm::search,
                onCreate = { id, _, callback -> state.users.firstOrNull { it.id == id }?.let { vm.createDirect(it, callback) } },
                onChat = { nav.navigate(chatRoute(it.id, it.peer.displayName)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            route = CHAT_ROUTE_PATTERN,
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("peerName") { type = NavType.StringType },
                navArgument(CHAT_FROM_PUSH_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val chatId = entry.arguments?.getLong("chatId") ?: return@composable
            DisposableEffect(chatId) {
                foregroundChatTracker.setActiveChat(chatId)
                onDispose { foregroundChatTracker.clearActiveChat(chatId) }
            }
            val peerName = Uri.decode(entry.arguments?.getString("peerName").orEmpty())
            val vm: ChatViewModel = viewModel(
                key = "chat-$chatId",
                factory = ViewModelFactory { ChatViewModel(currentUser.id, chatId, repository) },
            )
            val state by vm.state.collectAsState()
            ChatScreen(peerName, state, vm::setDraft, vm::send, vm::retry, vm::retryMessage) { nav.popBackStack() }
        }
    }
}

@Composable
private fun NotificationPermissionPrompt(preferences: com.example.bunbun.push.NotificationPermissionPreferences) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    var visible by remember {
        mutableStateOf(
            !preferences.wasPrompted() &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    if (!visible) return

    Dialog(onDismissRequest = {
        preferences.markPrompted()
        visible = false
    }) {
        TerminalPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text(
                    stringResource(R.string.notification_permission_title),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
                androidx.compose.material3.Text(
                    stringResource(R.string.notification_permission_message),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                PrimaryTerminalButton(
                    text = stringResource(R.string.notification_permission_allow),
                    onClick = {
                        preferences.markPrompted()
                        visible = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryTerminalButton(
                    text = stringResource(R.string.notification_permission_not_now),
                    onClick = {
                        preferences.markPrompted()
                        visible = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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

private fun chatRoute(chatId: Long, peerName: String, fromPush: Boolean = false): String =
    "chat/$chatId/${Uri.encode(peerName)}?$CHAT_FROM_PUSH_ARG=$fromPush"

private fun chatForwardEnter(): EnterTransition = fadeIn(
    animationSpec = tween(180, easing = FastOutSlowInEasing),
    initialAlpha = 0.90f,
) + slideInHorizontally(
    animationSpec = tween(200, easing = FastOutSlowInEasing),
) { width -> width / 8 }

private fun chatsForwardExit(): ExitTransition = fadeOut(
    animationSpec = tween(160, easing = FastOutSlowInEasing),
    targetAlpha = 0.90f,
) + slideOutHorizontally(
    animationSpec = tween(200, easing = FastOutSlowInEasing),
) { width -> -width / 20 }

private fun chatsBackEnter(): EnterTransition = fadeIn(
    animationSpec = tween(180, easing = FastOutSlowInEasing),
    initialAlpha = 0.90f,
) + slideInHorizontally(
    animationSpec = tween(200, easing = FastOutSlowInEasing),
) { width -> -width / 20 }

private fun chatBackExit(): ExitTransition = fadeOut(
    animationSpec = tween(160, easing = FastOutSlowInEasing),
    targetAlpha = 0.90f,
) + slideOutHorizontally(
    animationSpec = tween(200, easing = FastOutSlowInEasing),
) { width -> width / 8 }

private fun pushChatEnter(): EnterTransition = fadeIn(
    animationSpec = tween(140, easing = FastOutSlowInEasing),
    initialAlpha = 0.88f,
)

private fun pushChatExit(): ExitTransition = fadeOut(
    animationSpec = tween(100, easing = FastOutSlowInEasing),
    targetAlpha = 0.96f,
)
