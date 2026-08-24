package com.example.bunbun.navigation

internal const val CHATS_ROUTE = "chats"
internal const val SEARCH_ROUTE = "search"
internal const val SETTINGS_ROUTE = "settings"
internal const val CHAT_FROM_PUSH_ARG = "fromPush"
internal const val CHAT_ROUTE_PATTERN = "chat/{chatId}/{peerName}?$CHAT_FROM_PUSH_ARG={$CHAT_FROM_PUSH_ARG}"

internal enum class MainNavigationTransition {
    CHAT_FORWARD,
    CHAT_BACK,
    DEFAULT,
}

internal fun classifyMainNavigationTransition(
    initialRoute: String?,
    targetRoute: String?,
): MainNavigationTransition = when {
    initialRoute == CHATS_ROUTE && targetRoute == CHAT_ROUTE_PATTERN -> MainNavigationTransition.CHAT_FORWARD
    initialRoute == CHAT_ROUTE_PATTERN && targetRoute == CHATS_ROUTE -> MainNavigationTransition.CHAT_BACK
    else -> MainNavigationTransition.DEFAULT
}
