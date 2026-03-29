package com.rvkedition.androidstudiomobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.utils.ThemeManager

@Composable
fun IDEToolbar(
    projectName: String,
    onMenuClick: () -> Unit,
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onSave: () -> Unit,
    onBuild: () -> Unit,
    onRun: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onSync: () -> Unit
) {
    var showFileMenu by remember { mutableStateOf(false) }
    var showBuildMenu by remember { mutableStateOf(false) }

    Surface(
        color = ThemeManager.toolbarBg,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File tree toggle
            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Menu, contentDescription = "Toggle File Tree",
                    tint = ThemeManager.onSurface, modifier = Modifier.size(20.dp))
            }

            // Project name
            Text(
                text = projectName,
                fontSize = 14.sp,
                color = ThemeManager.onSurface,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                maxLines = 1
            )

            // File menu
            Box {
                IconButton(onClick = { showFileMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = "File",
                        tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showFileMenu,
                    onDismissRequest = { showFileMenu = false },
                    modifier = Modifier.background(ThemeManager.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("New Project", color = ThemeManager.onSurface, fontSize = 13.sp) },
                        onClick = { showFileMenu = false; onNewProject() },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Open Project", color = ThemeManager.onSurface, fontSize = 13.sp) },
                        onClick = { showFileMenu = false; onOpenProject() },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp)) }
                    )
                    Divider(color = ThemeManager.border)
                    DropdownMenuItem(
                        text = { Text("Save", color = ThemeManager.onSurface, fontSize = 13.sp) },
                        onClick = { showFileMenu = false; onSave() },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp)) }
                    )
                    Divider(color = ThemeManager.border)
                    DropdownMenuItem(
                        text = { Text("Settings", color = ThemeManager.onSurface, fontSize = 13.sp) },
                        onClick = { showFileMenu = false; onSettings() },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("About", color = ThemeManager.onSurface, fontSize = 13.sp) },
                        onClick = { showFileMenu = false; onAbout() },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp)) }
                    )
                }
            }

            // Sync button
            IconButton(onClick = onSync, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Sync, contentDescription = "Sync",
                    tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp))
            }

            // Build button
            IconButton(onClick = onBuild, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Build, contentDescription = "Build",
                    tint = ThemeManager.onSurface, modifier = Modifier.size(18.dp))
            }

            // Run button
            IconButton(onClick = onRun, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run",
                    tint = androidx.compose.ui.graphics.Color(0xFF6A8759), modifier = Modifier.size(22.dp))
            }
        }
    }
}
