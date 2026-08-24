package com.example.bunbun

import com.example.bunbun.navigation.CHATS_ROUTE
import com.example.bunbun.navigation.CHAT_ROUTE_PATTERN
import com.example.bunbun.navigation.MainNavigationTransition
import com.example.bunbun.navigation.SEARCH_ROUTE
import com.example.bunbun.navigation.classifyMainNavigationTransition
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTransitionPolicyTest {
    @Test fun chatsToChatUsesTerminalForwardTransition() {
        assertEquals(
            MainNavigationTransition.CHAT_FORWARD,
            classifyMainNavigationTransition(CHATS_ROUTE, CHAT_ROUTE_PATTERN),
        )
    }

    @Test fun chatToChatsUsesReverseTransition() {
        assertEquals(
            MainNavigationTransition.CHAT_BACK,
            classifyMainNavigationTransition(CHAT_ROUTE_PATTERN, CHATS_ROUTE),
        )
    }

    @Test fun otherRoutesKeepDefaultTransition() {
        assertEquals(
            MainNavigationTransition.DEFAULT,
            classifyMainNavigationTransition(CHATS_ROUTE, SEARCH_ROUTE),
        )
    }
}
