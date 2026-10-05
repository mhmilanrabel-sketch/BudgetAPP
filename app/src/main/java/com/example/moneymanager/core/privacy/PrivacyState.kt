package com.example.moneymanager.core.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global, in-memory privacy flag for financial amounts.
 * When true (default), all amounts render as "Rs. ••••••".
 * Toggled by the eye icon in each screen's top area.
 */
object PrivacyState {
    var isPrivate: Boolean by mutableStateOf(true)
        private set

    fun toggle() { isPrivate = !isPrivate }
    fun show() { isPrivate = false }
    fun hide() { isPrivate = true }
}