package com.rvkedition.androidstudiomobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun WelcomeScreen(
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Android Studio Mobile",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeManager.onBackground
        )

        Text(
            text = "Presented By RVK EDITION",
            fontSize = 13.sp,
            color = ThemeManager.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // New Project Button
        OutlinedButton(
            onClick = onNewProject,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ThemeManager.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(ThemeManager.primary)
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Project", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Open Project Button
        OutlinedButton(
            onClick = onOpenProject,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ThemeManager.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(ThemeManager.border)
            )
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Project", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Supports: Android (Kotlin/Java), Python, React Native, C++, Flutter",
            fontSize = 11.sp,
            color = Color(0xFF808080),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
