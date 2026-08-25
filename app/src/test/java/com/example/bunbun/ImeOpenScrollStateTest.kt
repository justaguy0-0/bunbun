package com.example.bunbun

import com.example.bunbun.ui.chat.ImeOpenScrollState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeOpenScrollStateTest {
    @Test fun hiddenToVisibleScrollsExactlyOnce() {
        val state = ImeOpenScrollState()

        assertTrue(state.onVisibilityChanged(true))
        assertFalse(state.onVisibilityChanged(true))
    }

    @Test fun visibleToHiddenDoesNotScroll() {
        val state = ImeOpenScrollState(initiallyVisible = true)

        assertFalse(state.onVisibilityChanged(false))
    }

    @Test fun hiddenToHiddenDoesNotScroll() {
        assertFalse(ImeOpenScrollState().onVisibilityChanged(false))
    }
}
