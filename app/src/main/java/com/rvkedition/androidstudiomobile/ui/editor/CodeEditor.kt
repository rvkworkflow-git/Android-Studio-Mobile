package com.rvkedition.androidstudiomobile.ui.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rvkedition.androidstudiomobile.ui.OpenFile
import com.rvkedition.androidstudiomobile.utils.SyntaxHighlighter
import com.rvkedition.androidstudiomobile.utils.ThemeManager

@Composable
fun CodeEditor(
    file: OpenFile,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(file.file.absolutePath) {
        mutableStateOf(TextFieldValue(file.content))
    }

    val language = remember(file.file.name) {
        SyntaxHighlighter.detectLanguage(file.file.name)
    }

    val scrollState = rememberScrollState()
    val lines = textFieldValue.text.split("\n")

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeManager.editorBg)
    ) {
        // Line numbers gutter
        Column(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .background(ThemeManager.gutterBg)
                .verticalScroll(scrollState)
                .padding(end = 4.dp, top = 4.dp)
        ) {
            lines.forEachIndexed { index, _ ->
                Text(
                    text = "${index + 1}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ThemeManager.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    lineHeight = 18.sp
                )
            }
        }

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ThemeManager.border)
        )

        // Code content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .padding(start = 8.dp, top = 4.dp, end = 4.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onContentChanged(newValue.text)
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = ThemeManager.onSurface,
                    lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(ThemeManager.primary),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    // Syntax highlighted overlay
                    Box {
                        // Show syntax highlighted text as background
                        val highlightedText = remember(textFieldValue.text, language) {
                            SyntaxHighlighter.highlight(textFieldValue.text, language)
                        }

                        Text(
                            text = highlightedText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Actual editable text field (transparent text to let highlights show)
                        innerTextField()
                    }
                }
            )
        }
    }
}
