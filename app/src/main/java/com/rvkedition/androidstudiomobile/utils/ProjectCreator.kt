package com.rvkedition.androidstudiomobile.utils

import java.io.File

/**
 * Creates real project structures for all supported platforms.
 */
object ProjectCreator {

    enum class ProjectType {
        ANDROID_KOTLIN, ANDROID_JAVA, PYTHON, REACT_NATIVE, CPP, FLUTTER
    }

    data class ProjectConfig(
        val name: String,
        val packageName: String,
        val type: ProjectType,
        val location: String,
        val minSdk: Int = 26,
        val targetSdk: Int = 34,
        val useCompose: Boolean = true,
        val gradleVersion: String = "8.14.4"
    )

    fun createProject(config: ProjectConfig): Boolean {
        val projectDir = File(config.location, config.name)
        if (projectDir.exists()) return false

        return when (config.type) {
            ProjectType.ANDROID_KOTLIN -> createAndroidKotlinProject(projectDir, config)
            ProjectType.ANDROID_JAVA -> createAndroidJavaProject(projectDir, config)
            ProjectType.PYTHON -> createPythonProject(projectDir, config)
            ProjectType.REACT_NATIVE -> createReactNativeProject(projectDir, config)
            ProjectType.CPP -> createCppProject(projectDir, config)
            ProjectType.FLUTTER -> createFlutterProject(projectDir, config)
        }
    }

    private fun createAndroidKotlinProject(dir: File, config: ProjectConfig): Boolean {
        try {
            val pkgPath = config.packageName.replace(".", "/")
            val dirs = listOf(
                "app/src/main/java/$pkgPath",
                "app/src/main/res/layout",
                "app/src/main/res/values",
                "app/src/main/res/drawable",
                "app/src/main/res/mipmap-hdpi",
                "gradle/wrapper"
            )
            dirs.forEach { File(dir, it).mkdirs() }

            // settings.gradle.kts
            File(dir, "settings.gradle.kts").writeText("""
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "${config.name}"
include(":app")
""".trimIndent())

            // Root build.gradle.kts
            File(dir, "build.gradle.kts").writeText("""
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
""".trimIndent())

            // App build.gradle.kts
            File(dir, "app/build.gradle.kts").writeText("""
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "${config.packageName}"
    compileSdk = ${config.targetSdk}

    defaultConfig {
        applicationId = "${config.packageName}"
        minSdk = ${config.minSdk}
        targetSdk = ${config.targetSdk}
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
${if (config.useCompose) """
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
""" else ""}
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
${if (config.useCompose) """
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.1")
""" else """
    implementation("com.google.android.material:material:1.10.0")
"""}
}
""".trimIndent())

            // AndroidManifest.xml
            File(dir, "app/src/main/AndroidManifest.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="${config.name}"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent())

            // MainActivity.kt
            if (config.useCompose) {
                File(dir, "app/src/main/java/$pkgPath/MainActivity.kt").writeText("""
package ${config.packageName}

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Greeting("${config.name}")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello, ${'$'}name!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Welcome to your new project")
    }
}
""".trimIndent())
            } else {
                File(dir, "app/src/main/java/$pkgPath/MainActivity.kt").writeText("""
package ${config.packageName}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(android.widget.TextView(this).apply {
            text = "Hello, ${config.name}!"
            textSize = 24f
        })
    }
}
""".trimIndent())
            }

            // gradle-wrapper.properties
            File(dir, "gradle/wrapper/gradle-wrapper.properties").writeText("""
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""".trimIndent())

            // gradlew
            File(dir, "gradlew").writeText("#!/bin/sh\nexec gradle \"\$@\"\n")
            File(dir, "gradlew").setExecutable(true)

            // strings.xml
            File(dir, "app/src/main/res/values/strings.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${config.name}</string>
</resources>
""".trimIndent())

            // gradle.properties
            File(dir, "gradle.properties").writeText("""
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
""".trimIndent())

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun createAndroidJavaProject(dir: File, config: ProjectConfig): Boolean {
        val modifiedConfig = config.copy(useCompose = false)
        return createAndroidKotlinProject(dir, modifiedConfig)
    }

    private fun createPythonProject(dir: File, config: ProjectConfig): Boolean {
        try {
            dir.mkdirs()
            File(dir, "src").mkdirs()
            File(dir, "tests").mkdirs()

            File(dir, "main.py").writeText("""
#!/usr/bin/env python3
\"\"\"${config.name} - Main entry point.\"\"\"


def main():
    print("Hello from ${config.name}!")


if __name__ == "__main__":
    main()
""".trimIndent())

            File(dir, "requirements.txt").writeText("# Add your dependencies here\n")

            File(dir, "src/__init__.py").writeText("")
            File(dir, "tests/__init__.py").writeText("")

            File(dir, "tests/test_main.py").writeText("""
import unittest

class TestMain(unittest.TestCase):
    def test_hello(self):
        self.assertTrue(True)

if __name__ == "__main__":
    unittest.main()
""".trimIndent())

            File(dir, "README.md").writeText("# ${config.name}\n\nA Python project.\n")

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun createReactNativeProject(dir: File, config: ProjectConfig): Boolean {
        try {
            val dirs = listOf("src", "src/components", "src/screens", "android", "ios", "__tests__")
            dirs.forEach { File(dir, it).mkdirs() }

            File(dir, "package.json").writeText("""
{
  "name": "${config.name.lowercase().replace(" ", "-")}",
  "version": "1.0.0",
  "main": "index.js",
  "scripts": {
    "start": "react-native start",
    "android": "react-native run-android",
    "ios": "react-native run-ios",
    "test": "jest"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-native": "^0.73.0"
  },
  "devDependencies": {
    "@babel/core": "^7.23.0",
    "@babel/runtime": "^7.23.0",
    "jest": "^29.7.0",
    "react-test-renderer": "^18.2.0"
  }
}
""".trimIndent())

            File(dir, "index.js").writeText("""
import { AppRegistry } from 'react-native';
import App from './src/App';

AppRegistry.registerComponent('${config.name}', () => App);
""".trimIndent())

            File(dir, "src/App.js").writeText("""
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const App = () => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Welcome to ${config.name}</Text>
      <Text style={styles.subtitle}>Built with React Native</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#2B2B2B' },
  title: { fontSize: 24, fontWeight: 'bold', color: '#FFFFFF', marginBottom: 8 },
  subtitle: { fontSize: 16, color: '#BBBBBB' },
});

export default App;
""".trimIndent())

            File(dir, "babel.config.js").writeText("module.exports = { presets: ['module:metro-react-native-babel-preset'] };\n")

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun createCppProject(dir: File, config: ProjectConfig): Boolean {
        try {
            val dirs = listOf("src", "include", "build")
            dirs.forEach { File(dir, it).mkdirs() }

            File(dir, "CMakeLists.txt").writeText("""
cmake_minimum_required(VERSION 3.16)
project(${config.name} VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(${'$'}{PROJECT_NAME} src/main.cpp)
target_include_directories(${'$'}{PROJECT_NAME} PRIVATE include)
""".trimIndent())

            File(dir, "src/main.cpp").writeText("""
#include <iostream>

int main() {
    std::cout << "Hello from ${config.name}!" << std::endl;
    return 0;
}
""".trimIndent())

            File(dir, "include/app.h").writeText("""
#pragma once

namespace ${config.name.lowercase().replace(" ", "_")} {
    void run();
}
""".trimIndent())

            File(dir, "Makefile").writeText("""
.PHONY: build clean run

build:
	mkdir -p build && cd build && cmake .. && make

clean:
	rm -rf build

run: build
	./build/${config.name}
""".trimIndent())

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun createFlutterProject(dir: File, config: ProjectConfig): Boolean {
        try {
            val dirs = listOf("lib", "lib/screens", "lib/widgets", "test", "android", "ios", "assets")
            dirs.forEach { File(dir, it).mkdirs() }

            File(dir, "pubspec.yaml").writeText("""
name: ${config.name.lowercase().replace(" ", "_")}
description: A new Flutter project.
version: 1.0.0+1

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  cupertino_icons: ^1.0.6

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0

flutter:
  uses-material-design: true
  assets:
    - assets/
""".trimIndent())

            File(dir, "lib/main.dart").writeText("""
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '${config.name}',
      theme: ThemeData(
        colorSchemeSeed: Colors.blue,
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorSchemeSeed: Colors.blue,
        useMaterial3: true,
        brightness: Brightness.dark,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('${config.name}')),
      body: const Center(
        child: Text('Welcome to ${config.name}!', style: TextStyle(fontSize: 24)),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        child: const Icon(Icons.add),
      ),
    );
  }
}
""".trimIndent())

            File(dir, "test/widget_test.dart").writeText("""
import 'package:flutter_test/flutter_test.dart';
import 'package:${config.name.lowercase().replace(" ", "_")}/main.dart';

void main() {
  testWidgets('App loads', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());
    expect(find.text('${config.name}'), findsOneWidget);
  });
}
""".trimIndent())

            return true
        } catch (e: Exception) {
            return false
        }
    }
}
