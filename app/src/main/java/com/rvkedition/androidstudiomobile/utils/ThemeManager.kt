package com.rvkedition.androidstudiomobile.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

object ThemeManager {
    var isDarkMode = mutableStateOf(true)

    // Dark Theme (Darcula - Android Studio default)
    object Dark {
        val background = Color(0xFF2B2B2B)
        val surface = Color(0xFF3C3F41)
        val surfaceVariant = Color(0xFF313335)
        val primary = Color(0xFF4A88C7)
        val onPrimary = Color.White
        val onBackground = Color(0xFFBBBBBB)
        val onSurface = Color(0xFFBBBBBB)
        val editorBg = Color(0xFF2B2B2B)
        val gutterBg = Color(0xFF313335)
        val toolbarBg = Color(0xFF3C3F41)
        val tabActive = Color(0xFF4E5254)
        val tabInactive = Color(0xFF3C3F41)
        val border = Color(0xFF515151)
        val treeSelection = Color(0xFF2D5F8A)
        val terminalBg = Color(0xFF1E1E1E)
        val statusBar = Color(0xFF3C3F41)
    }

    // Light Theme
    object Light {
        val background = Color(0xFFF5F5F5)
        val surface = Color.White
        val surfaceVariant = Color(0xFFF0F0F0)
        val primary = Color(0xFF4A88C7)
        val onPrimary = Color.White
        val onBackground = Color.Black
        val onSurface = Color.Black
        val editorBg = Color.White
        val gutterBg = Color(0xFFF0F0F0)
        val toolbarBg = Color(0xFFF2F2F2)
        val tabActive = Color.White
        val tabInactive = Color(0xFFEBEBEB)
        val border = Color(0xFFCCCCCC)
        val treeSelection = Color(0xFF3875D7)
        val terminalBg = Color.White
        val statusBar = Color(0xFFF2F2F2)
    }

    // Syntax Highlighting (Darcula-based)
    object Syntax {
        val keyword = Color(0xFFCC7832)      // Orange
        val string = Color(0xFF6A8759)       // Green
        val number = Color(0xFF6897BB)       // Blue
        val comment = Color(0xFF808080)      // Gray
        val annotation = Color(0xFFBBB529)   // Yellow-green
        val type = Color(0xFFA9B7C6)         // Light blue-gray
        val function = Color(0xFFFFC66D)     // Gold
        val variable = Color(0xFF9876AA)     // Purple
        val operator = Color(0xFFA9B7C6)     // Light blue-gray
        val errorUnderline = Color.Red
        val xmlTag = Color(0xFFE8BF6A)       // Gold
        val xmlAttr = Color(0xFFBABABA)      // Light gray
        val xmlValue = Color(0xFF6A8759)     // Green
    }

    fun current(): Any = if (isDarkMode.value) Dark else Light

    val background: Color get() = if (isDarkMode.value) Dark.background else Light.background
    val surface: Color get() = if (isDarkMode.value) Dark.surface else Light.surface
    val surfaceVariant: Color get() = if (isDarkMode.value) Dark.surfaceVariant else Light.surfaceVariant
    val primary: Color get() = if (isDarkMode.value) Dark.primary else Light.primary
    val onPrimary: Color get() = if (isDarkMode.value) Dark.onPrimary else Light.onPrimary
    val onBackground: Color get() = if (isDarkMode.value) Dark.onBackground else Light.onBackground
    val onSurface: Color get() = if (isDarkMode.value) Dark.onSurface else Light.onSurface
    val editorBg: Color get() = if (isDarkMode.value) Dark.editorBg else Light.editorBg
    val gutterBg: Color get() = if (isDarkMode.value) Dark.gutterBg else Light.gutterBg
    val toolbarBg: Color get() = if (isDarkMode.value) Dark.toolbarBg else Light.toolbarBg
    val tabActive: Color get() = if (isDarkMode.value) Dark.tabActive else Light.tabActive
    val tabInactive: Color get() = if (isDarkMode.value) Dark.tabInactive else Light.tabInactive
    val border: Color get() = if (isDarkMode.value) Dark.border else Light.border
    val treeSelection: Color get() = if (isDarkMode.value) Dark.treeSelection else Light.treeSelection
    val terminalBg: Color get() = if (isDarkMode.value) Dark.terminalBg else Light.terminalBg
    val statusBar: Color get() = if (isDarkMode.value) Dark.statusBar else Light.statusBar
}
