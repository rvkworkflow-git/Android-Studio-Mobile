package com.rvkedition.androidstudiomobile.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.R
import com.rvkedition.androidstudiomobile.utils.ThemeManager

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ThemeManager.primary)
            }
        },
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Android Studio Mobile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Presented By RVK EDITION",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ThemeManager.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Version 1.0.0",
                    fontSize = 13.sp,
                    color = ThemeManager.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "A fully functional multi-platform mobile IDE supporting Android (Kotlin/Java), Python, React Native, C++, and Flutter development.",
                    fontSize = 12.sp,
                    color = ThemeManager.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = ThemeManager.border)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Features:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                val features = listOf(
                    "Real-time syntax highlighting & error detection",
                    "Full terminal with chmod/shell support",
                    "Multi-platform project creation",
                    "Live Gradle sync & dependency management",
                    "Real APK build engine",
                    "Dual C++/Python backend architecture",
                    "Dark/Light theme support",
                    "JDK 17/21, NDK, SDK integration",
                    "WebSocket-based live build logs"
                )

                features.forEach { feature ->
                    Text(
                        text = "  $feature",
                        fontSize = 11.sp,
                        color = ThemeManager.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Built with Kotlin, Jetpack Compose, C++ JNI, Python",
                    fontSize = 10.sp,
                    color = Color(0xFF808080),
                    textAlign = TextAlign.Center
                )
            }
        },
        containerColor = ThemeManager.surface
    )
}
