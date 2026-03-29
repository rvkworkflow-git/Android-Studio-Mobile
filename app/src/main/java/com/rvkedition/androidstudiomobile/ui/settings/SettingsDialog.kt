package com.rvkedition.androidstudiomobile.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rvkedition.androidstudiomobile.utils.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var isDarkTheme by ThemeManager.isDarkMode
    var fontSize by remember { mutableFloatStateOf(13f) }
    var showLineNumbers by remember { mutableStateOf(true) }
    var autoSave by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(false) }
    var selectedJdk by remember { mutableStateOf("JDK 17") }
    var selectedGradle by remember { mutableStateOf("Gradle 8.14.4") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = ThemeManager.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Appearance Section
                    Text("Appearance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ThemeManager.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dark Mode Toggle
                    SettingsSwitch(
                        title = "Dark Theme (Darcula)",
                        checked = isDarkTheme,
                        onCheckedChange = { isDarkTheme = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Editor Section
                    Text("Editor", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ThemeManager.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Font Size
                    Text("Font Size: ${fontSize.toInt()}sp", fontSize = 12.sp, color = ThemeManager.onSurface)
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 10f..24f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = ThemeManager.primary,
                            activeTrackColor = ThemeManager.primary
                        )
                    )

                    SettingsSwitch(
                        title = "Show Line Numbers",
                        checked = showLineNumbers,
                        onCheckedChange = { showLineNumbers = it }
                    )

                    SettingsSwitch(
                        title = "Auto Save",
                        checked = autoSave,
                        onCheckedChange = { autoSave = it }
                    )

                    SettingsSwitch(
                        title = "Word Wrap",
                        checked = wordWrap,
                        onCheckedChange = { wordWrap = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Build Section
                    Text("Build & SDK", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ThemeManager.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // JDK Selection
                    Text("Default JDK", fontSize = 12.sp, color = ThemeManager.onSurface.copy(alpha = 0.7f))
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        listOf("JDK 17", "JDK 21").forEach { jdk ->
                            FilterChip(
                                selected = selectedJdk == jdk,
                                onClick = { selectedJdk = jdk },
                                label = { Text(jdk, fontSize = 12.sp) },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeManager.primary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gradle Version
                    Text("Default Gradle Version", fontSize = 12.sp, color = ThemeManager.onSurface.copy(alpha = 0.7f))
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        listOf("Gradle 8.14.4", "Gradle 9.3.0", "Gradle 9.4.0").forEach { gradle ->
                            FilterChip(
                                selected = selectedGradle == gradle,
                                onClick = { selectedGradle = gradle },
                                label = { Text(gradle, fontSize = 11.sp) },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeManager.primary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeManager.primary)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = ThemeManager.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemeManager.primary,
                checkedTrackColor = ThemeManager.primary.copy(alpha = 0.3f)
            )
        )
    }
}
