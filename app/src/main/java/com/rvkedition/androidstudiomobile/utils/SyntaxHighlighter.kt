package com.rvkedition.androidstudiomobile.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration

/**
 * Syntax highlighter supporting Kotlin, Java, Python, Dart, C++, JavaScript/TypeScript, XML, Groovy, and KTS.
 */
object SyntaxHighlighter {

    enum class Language {
        KOTLIN, JAVA, PYTHON, DART, CPP, JAVASCRIPT, XML, GROOVY, UNKNOWN
    }

    fun detectLanguage(fileName: String): Language {
        return when {
            fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Language.KOTLIN
            fileName.endsWith(".java") -> Language.JAVA
            fileName.endsWith(".py") -> Language.PYTHON
            fileName.endsWith(".dart") -> Language.DART
            fileName.endsWith(".cpp") || fileName.endsWith(".c") || fileName.endsWith(".h") || fileName.endsWith(".hpp") -> Language.CPP
            fileName.endsWith(".js") || fileName.endsWith(".jsx") || fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> Language.JAVASCRIPT
            fileName.endsWith(".xml") -> Language.XML
            fileName.endsWith(".gradle") -> Language.GROOVY
            else -> Language.UNKNOWN
        }
    }

    private val kotlinKeywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "enum", "sealed", "data",
        "abstract", "open", "override", "private", "protected", "public", "internal",
        "companion", "init", "constructor", "return", "if", "else", "when", "for",
        "while", "do", "break", "continue", "throw", "try", "catch", "finally",
        "import", "package", "is", "as", "in", "out", "by", "suspend", "inline",
        "reified", "crossinline", "noinline", "tailrec", "operator", "infix",
        "lateinit", "const", "typealias", "annotation", "this", "super", "null",
        "true", "false", "it"
    )

    private val javaKeywords = setOf(
        "public", "private", "protected", "static", "final", "abstract", "class",
        "interface", "extends", "implements", "return", "if", "else", "for", "while",
        "do", "switch", "case", "break", "continue", "new", "this", "super", "void",
        "int", "long", "double", "float", "boolean", "char", "byte", "short",
        "try", "catch", "finally", "throw", "throws", "import", "package", "null",
        "true", "false", "instanceof", "synchronized", "volatile", "transient",
        "enum", "default", "native", "strictfp", "assert", "var"
    )

    private val pythonKeywords = setOf(
        "def", "class", "return", "if", "elif", "else", "for", "while", "break",
        "continue", "import", "from", "as", "try", "except", "finally", "raise",
        "with", "yield", "lambda", "pass", "and", "or", "not", "in", "is", "None",
        "True", "False", "global", "nonlocal", "del", "assert", "async", "await",
        "self", "print"
    )

    private val cppKeywords = setOf(
        "int", "long", "double", "float", "char", "void", "bool", "auto", "const",
        "static", "extern", "register", "volatile", "inline", "virtual", "override",
        "class", "struct", "union", "enum", "namespace", "using", "template",
        "typename", "public", "private", "protected", "friend", "operator",
        "new", "delete", "return", "if", "else", "for", "while", "do", "switch",
        "case", "break", "continue", "goto", "throw", "try", "catch", "include",
        "define", "ifdef", "ifndef", "endif", "nullptr", "true", "false",
        "sizeof", "typedef", "this", "constexpr", "noexcept", "static_cast",
        "dynamic_cast", "reinterpret_cast", "const_cast"
    )

    private val dartKeywords = setOf(
        "abstract", "as", "assert", "async", "await", "break", "case", "catch",
        "class", "const", "continue", "default", "deferred", "do", "dynamic",
        "else", "enum", "export", "extends", "extension", "external", "factory",
        "false", "final", "finally", "for", "Function", "get", "hide", "if",
        "implements", "import", "in", "interface", "is", "late", "library",
        "mixin", "new", "null", "on", "operator", "part", "required", "rethrow",
        "return", "set", "show", "static", "super", "switch", "sync", "this",
        "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield"
    )

    private val jsKeywords = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while",
        "do", "switch", "case", "break", "continue", "class", "extends", "new",
        "this", "super", "import", "export", "from", "default", "try", "catch",
        "finally", "throw", "async", "await", "yield", "typeof", "instanceof",
        "in", "of", "delete", "void", "null", "undefined", "true", "false",
        "static", "get", "set", "constructor", "interface", "type", "enum",
        "implements", "package", "private", "protected", "public"
    )

    data class ErrorInfo(val line: Int, val startCol: Int, val endCol: Int, val message: String)

    fun highlight(
        code: String,
        language: Language,
        errors: List<ErrorInfo> = emptyList()
    ): AnnotatedString {
        return when (language) {
            Language.XML -> highlightXml(code, errors)
            else -> highlightCode(code, language, errors)
        }
    }

    private fun getKeywords(language: Language): Set<String> {
        return when (language) {
            Language.KOTLIN -> kotlinKeywords
            Language.JAVA -> javaKeywords
            Language.PYTHON -> pythonKeywords
            Language.CPP -> cppKeywords
            Language.DART -> dartKeywords
            Language.JAVASCRIPT -> jsKeywords
            Language.GROOVY -> javaKeywords + setOf("def", "in", "trait", "as")
            else -> emptySet()
        }
    }

    private fun highlightCode(
        code: String,
        language: Language,
        errors: List<ErrorInfo>
    ): AnnotatedString {
        val keywords = getKeywords(language)

        return buildAnnotatedString {
            append(code)

            // Apply keyword highlighting
            val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
            wordRegex.findAll(code).forEach { match ->
                val word = match.value
                when {
                    word in keywords -> {
                        addStyle(
                            SpanStyle(color = ThemeManager.Syntax.keyword),
                            match.range.first, match.range.last + 1
                        )
                    }
                    word.first().isUpperCase() && word.length > 1 -> {
                        addStyle(
                            SpanStyle(color = ThemeManager.Syntax.type),
                            match.range.first, match.range.last + 1
                        )
                    }
                }
            }

            // String literals (double-quoted)
            val stringRegex = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")
            stringRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.string),
                    match.range.first, match.range.last + 1
                )
            }

            // String literals (single-quoted)
            val singleStringRegex = Regex("'(?:[^'\\\\]|\\\\.)*'")
            singleStringRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.string),
                    match.range.first, match.range.last + 1
                )
            }

            // Numbers
            val numberRegex = Regex("\\b\\d+(\\.\\d+)?[fFdDlL]?\\b")
            numberRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.number),
                    match.range.first, match.range.last + 1
                )
            }

            // Comments (single line)
            val commentRegex = Regex("//.*$", RegexOption.MULTILINE)
            commentRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.comment, fontStyle = FontStyle.Italic),
                    match.range.first, match.range.last + 1
                )
            }

            // Comments (Python #)
            if (language == Language.PYTHON) {
                val pyCommentRegex = Regex("#.*$", RegexOption.MULTILINE)
                pyCommentRegex.findAll(code).forEach { match ->
                    addStyle(
                        SpanStyle(color = ThemeManager.Syntax.comment, fontStyle = FontStyle.Italic),
                        match.range.first, match.range.last + 1
                    )
                }
            }

            // Annotations
            val annotationRegex = Regex("@[A-Za-z][A-Za-z0-9_]*")
            annotationRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.annotation),
                    match.range.first, match.range.last + 1
                )
            }

            // Error underlines
            val lines = code.split("\n")
            for (error in errors) {
                if (error.line < lines.size) {
                    var offset = 0
                    for (i in 0 until error.line) {
                        offset += lines[i].length + 1
                    }
                    val start = offset + error.startCol
                    val end = (offset + error.endCol).coerceAtMost(code.length)
                    if (start < code.length && end <= code.length) {
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline, color = ThemeManager.Syntax.errorUnderline),
                            start, end
                        )
                    }
                }
            }
        }
    }

    private fun highlightXml(code: String, errors: List<ErrorInfo>): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            // XML tags
            val tagRegex = Regex("</?[a-zA-Z][a-zA-Z0-9_.:-]*")
            tagRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.xmlTag),
                    match.range.first, match.range.last + 1
                )
            }

            // XML attributes
            val attrRegex = Regex("\\s[a-zA-Z][a-zA-Z0-9_:-]*(?==)")
            attrRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.xmlAttr),
                    match.range.first, match.range.last + 1
                )
            }

            // XML attribute values
            val valueRegex = Regex("\"[^\"]*\"")
            valueRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.xmlValue),
                    match.range.first, match.range.last + 1
                )
            }

            // XML comments
            val xmlCommentRegex = Regex("<!--[\\s\\S]*?-->")
            xmlCommentRegex.findAll(code).forEach { match ->
                addStyle(
                    SpanStyle(color = ThemeManager.Syntax.comment, fontStyle = FontStyle.Italic),
                    match.range.first, match.range.last + 1
                )
            }
        }
    }
}
