package com.rvkedition.androidstudiomobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rvkedition.androidstudiomobile.backend.build.BuildService
import com.rvkedition.androidstudiomobile.backend.python.PythonServerService
import com.rvkedition.androidstudiomobile.utils.ThemeManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start Python server service
        startPythonServer()

        setContent {
            IDEApp(
                onStartBuild = { projectPath, buildType ->
                    startBuild(projectPath, buildType)
                }
            )
        }
    }

    private fun startPythonServer() {
        try {
            val intent = Intent(this, PythonServerService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start Python server", e)
        }
    }

    private fun startBuild(projectPath: String, buildType: String) {
        try {
            val intent = Intent(this, BuildService::class.java).apply {
                putExtra(BuildService.EXTRA_PROJECT_PATH, projectPath)
                putExtra(BuildService.EXTRA_BUILD_TYPE, buildType)
            }
            startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start build", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, PythonServerService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDEApp(onStartBuild: (String, String) -> Unit = { _, _ -> }) {
    val isDark by ThemeManager.isDarkMode

    var currentProjectPath by remember { mutableStateOf<String?>(null) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenProjectDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var drawerOpen by remember { mutableStateOf(false) }
    var openFiles by remember { mutableStateOf(listOf<OpenFile>()) }
    var activeFileIndex by remember { mutableIntStateOf(-1) }
    var terminalOutput by remember { mutableStateOf("") }

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme(
            primary = ThemeManager.primary,
            onPrimary = ThemeManager.onPrimary,
            surface = ThemeManager.surface,
            onSurface = ThemeManager.onSurface,
            background = ThemeManager.background,
            onBackground = ThemeManager.onBackground
        ) else lightColorScheme(
            primary = ThemeManager.primary,
            onPrimary = ThemeManager.onPrimary,
            surface = ThemeManager.surface,
            onSurface = ThemeManager.onSurface,
            background = ThemeManager.background,
            onBackground = ThemeManager.onBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeManager.background)
        ) {
            // Top Toolbar
            com.rvkedition.androidstudiomobile.ui.components.IDEToolbar(
                projectName = currentProjectPath?.let { java.io.File(it).name } ?: "Android Studio Mobile",
                onMenuClick = { drawerOpen = !drawerOpen },
                onNewProject = { showNewProjectDialog = true },
                onOpenProject = { showOpenProjectDialog = true },
                onSave = {
                    if (activeFileIndex >= 0 && activeFileIndex < openFiles.size) {
                        val file = openFiles[activeFileIndex]
                        com.rvkedition.androidstudiomobile.utils.FileUtils.saveFileContent(
                            file.file, file.content
                        )
                    }
                },
                onBuild = {
                    currentProjectPath?.let { onStartBuild(it, "debug") }
                },
                onRun = {
                    currentProjectPath?.let { onStartBuild(it, "debug") }
                },
                onSettings = { showSettingsDialog = true },
                onAbout = { showAboutDialog = true },
                onSync = {
                    currentProjectPath?.let { path ->
                        com.rvkedition.androidstudiomobile.backend.python.WebSocketClient
                            .getInstance().sendSync(path)
                    }
                }
            )

            // Main content area
            Row(modifier = Modifier.weight(1f)) {
                // File Tree Panel (collapsible sidebar)
                if (drawerOpen && currentProjectPath != null) {
                    com.rvkedition.androidstudiomobile.ui.filetree.FileTreePanel(
                        rootPath = currentProjectPath!!,
                        modifier = Modifier.width(androidx.compose.ui.unit.Dp(220f)),
                        onFileSelected = { file ->
                            if (!file.isDirectory) {
                                val existingIndex = openFiles.indexOfFirst { it.file.absolutePath == file.absolutePath }
                                if (existingIndex >= 0) {
                                    activeFileIndex = existingIndex
                                } else {
                                    val content = com.rvkedition.androidstudiomobile.utils.FileUtils.readFileContent(file)
                                    openFiles = openFiles + OpenFile(file, content)
                                    activeFileIndex = openFiles.size - 1
                                }
                            }
                        }
                    )

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(androidx.compose.ui.unit.Dp(1f))
                            .background(ThemeManager.border)
                    )
                }

                // Editor/Terminal area
                Column(modifier = Modifier.weight(1f)) {
                    when (selectedBottomTab) {
                        0 -> {
                            // Editor tab area
                            if (openFiles.isNotEmpty()) {
                                // Tab bar
                                com.rvkedition.androidstudiomobile.ui.editor.EditorTabBar(
                                    files = openFiles,
                                    activeIndex = activeFileIndex,
                                    onTabSelected = { activeFileIndex = it },
                                    onTabClosed = { index ->
                                        openFiles = openFiles.toMutableList().apply { removeAt(index) }
                                        if (activeFileIndex >= openFiles.size) {
                                            activeFileIndex = openFiles.size - 1
                                        }
                                    }
                                )

                                // Code editor
                                if (activeFileIndex >= 0 && activeFileIndex < openFiles.size) {
                                    com.rvkedition.androidstudiomobile.ui.editor.CodeEditor(
                                        file = openFiles[activeFileIndex],
                                        onContentChanged = { newContent ->
                                            openFiles = openFiles.toMutableList().apply {
                                                this[activeFileIndex] = this[activeFileIndex].copy(content = newContent, modified = true)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                // Welcome screen
                                com.rvkedition.androidstudiomobile.ui.components.WelcomeScreen(
                                    onNewProject = { showNewProjectDialog = true },
                                    onOpenProject = { showOpenProjectDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        1 -> {
                            // Terminal
                            com.rvkedition.androidstudiomobile.ui.terminal.TerminalView(
                                workingDir = currentProjectPath ?: "/",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        2 -> {
                            // Build output
                            com.rvkedition.androidstudiomobile.ui.components.BuildOutputView(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bottom Navigation
            NavigationBar(
                containerColor = ThemeManager.statusBar,
                contentColor = ThemeManager.onSurface,
                modifier = Modifier.height(androidx.compose.ui.unit.Dp(56f))
            ) {
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                    label = { Text("Editor", fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemeManager.primary,
                        unselectedIconColor = ThemeManager.onSurface
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 1,
                    onClick = { selectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                    label = { Text("Terminal", fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemeManager.primary,
                        unselectedIconColor = ThemeManager.onSurface
                    )
                )
                NavigationBarItem(
                    selected = selectedBottomTab == 2,
                    onClick = { selectedBottomTab = 2 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Build") },
                    label = { Text("Build", fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemeManager.primary,
                        unselectedIconColor = ThemeManager.onSurface
                    )
                )
            }
        }

        // Dialogs
        if (showNewProjectDialog) {
            com.rvkedition.androidstudiomobile.ui.project.NewProjectDialog(
                onDismiss = { showNewProjectDialog = false },
                onProjectCreated = { projectPath ->
                    currentProjectPath = projectPath
                    showNewProjectDialog = false
                    drawerOpen = true
                }
            )
        }

        if (showOpenProjectDialog) {
            com.rvkedition.androidstudiomobile.ui.project.OpenProjectDialog(
                onDismiss = { showOpenProjectDialog = false },
                onProjectOpened = { projectPath ->
                    currentProjectPath = projectPath
                    showOpenProjectDialog = false
                    drawerOpen = true
                }
            )
        }

        if (showAboutDialog) {
            com.rvkedition.androidstudiomobile.ui.about.AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }

        if (showSettingsDialog) {
            com.rvkedition.androidstudiomobile.ui.settings.SettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

data class OpenFile(
    val file: java.io.File,
    val content: String,
    val modified: Boolean = false
)
