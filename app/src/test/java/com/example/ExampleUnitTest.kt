package com.example

import com.example.moneymanager.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun screenNavigationItems_areNonNull() {
        val items = Screen.bottomNavItems
        assertEquals(6, items.size)
        items.forEach { screen ->
            assertNotNull("Screen in bottomNavItems must not be null", screen)
            assertNotNull("Screen route must not be null", screen.route)
            assertNotNull("Screen title must not be null", screen.title)
            assertNotNull("Screen icon must not be null", screen.icon)
        }
    }
}
