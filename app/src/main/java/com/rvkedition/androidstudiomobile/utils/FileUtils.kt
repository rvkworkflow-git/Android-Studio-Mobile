package com.rvkedition.androidstudiomobile.utils

import java.io.File

object FileUtils {

    data class FileNode(
        val file: File,
        val name: String,
        val isDirectory: Boolean,
        val children: MutableList<FileNode> = mutableListOf(),
        val depth: Int = 0,
        var isExpanded: Boolean = false
    )

    fun buildFileTree(rootDir: File, depth: Int = 0): FileNode {
        val node = FileNode(
            file = rootDir,
            name = rootDir.name,
            isDirectory = rootDir.isDirectory,
            depth = depth,
            isExpanded = depth == 0
        )

        if (rootDir.isDirectory) {
            val children = rootDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            children?.forEach { child ->
                if (!child.name.startsWith(".") || child.name == ".gradle") {
                    node.children.add(buildFileTree(child, depth + 1))
                }
            }
        }

        return node
    }

    fun flattenTree(node: FileNode): List<FileNode> {
        val result = mutableListOf<FileNode>()
        result.add(node)
        if (node.isExpanded && node.isDirectory) {
            for (child in node.children) {
                result.addAll(flattenTree(child))
            }
        }
        return result
    }

    fun getFileIcon(fileName: String): String {
        return when {
            fileName.endsWith(".kt") -> "\uD83D\uDCDC" // Kotlin
            fileName.endsWith(".java") -> "\u2615" // Java
            fileName.endsWith(".xml") -> "\uD83D\uDCC4" // XML
            fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts") -> "\uD83D\uDC18" // Gradle
            fileName.endsWith(".py") -> "\uD83D\uDC0D" // Python
            fileName.endsWith(".dart") -> "\uD83C\uDFAF" // Dart
            fileName.endsWith(".js") || fileName.endsWith(".jsx") -> "\uD83D\uDFE8" // JS
            fileName.endsWith(".ts") || fileName.endsWith(".tsx") -> "\uD83D\uDD35" // TS
            fileName.endsWith(".cpp") || fileName.endsWith(".c") || fileName.endsWith(".h") -> "\u2699\uFE0F" // C++
            fileName.endsWith(".json") -> "\uD83D\uDCC1" // JSON
            fileName.endsWith(".md") -> "\uD83D\uDCD6" // Markdown
            fileName.endsWith(".txt") -> "\uD83D\uDCC4" // Text
            fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".svg") -> "\uD83D\uDDBC\uFE0F" // Image
            fileName.endsWith(".apk") -> "\uD83D\uDCE6" // APK
            fileName.endsWith(".pro") -> "\uD83D\uDEE1\uFE0F" // ProGuard
            else -> "\uD83D\uDCC4"
        }
    }

    fun readFileContent(file: File): String {
        return try {
            if (file.length() > 2 * 1024 * 1024) {
                "// File too large to display (${file.length() / 1024}KB)"
            } else {
                file.readText()
            }
        } catch (e: Exception) {
            "// Error reading file: ${e.message}"
        }
    }

    fun saveFileContent(file: File, content: String): Boolean {
        return try {
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(parent: File, name: String): File? {
        val dir = File(parent, name)
        return if (dir.mkdirs()) dir else null
    }

    fun createFile(parent: File, name: String, content: String = ""): File? {
        val file = File(parent, name)
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun deleteRecursively(file: File): Boolean {
        return file.deleteRecursively()
    }

    fun copyFile(source: File, dest: File): Boolean {
        return try {
            source.copyTo(dest, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
