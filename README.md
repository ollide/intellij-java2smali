intellij-java2smali
===================

[![Travis Build Status](https://travis-ci.org/ollide/intellij-java2smali.svg?branch=develop)](https://travis-ci.org/ollide/intellij-java2smali)
[![AppVeyor Build status](https://ci.appveyor.com/api/projects/status/htqnq07bveqh8elv/branch/develop?svg=true)](https://ci.appveyor.com/project/ollide/intellij-java2smali)
[![Quality Gate status](https://sonarcloud.io/api/project_badges/measure?project=org.ollide%3Aintellij-java2smali&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.ollide%3Aintellij-java2smali)

Simple plugin for IntelliJ IDEA &amp; Android Studio to easily compile Java & Kotlin files to smali.

Have you ever compiled and packaged a dummy Android application just to grab some Java sources in smali? Stop that! Transforming Java to smali just got easier.

## Usage ##

1. create or open a Java source file with your desired functionality
2. click 'Build' and 'Compile to smali'
3. wait while IDEA's indicating the compilation process
4. the smali file is opened automatically in your editor

<img src="img/plugin_usage2.gif" alt="java2smali usage gif" width="555">

## Installation ##

java2smali is available at [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/7385) and can therefore be found in your IDE's plugin manager! Alternatively, you can download the plugin from the release section.

### Build From Source

This plugin uses JetBrains [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin) and can easily build from source.

Clone the repository:

    git clone https://github.com/ollide/intellij-java2smali.git
    cd intellij-java2smali

Build the plugin with Gradle:

    ./gradlew buildPlugin

The plugin can be found in `/build/distributions/java2smali-$VERSION.zip`.

## Troubleshooting ##

To enable debug logging of the plugin, select `Help -> Diagnostic Tools -> Debug Log Settings…`
and add the following line:
```
#org.ollide.java2smali
```

## Third-Party Libraries &amp; Credits ##

- [dx](http://developer.android.com/tools/help/index.html#tools-platform) from the Android platform-tools is used to create a .dex version of the compiled Java (.class) file
- [baksmali](https://github.com/JesusFreke/smali) –
  a great disassembler for Android's dex format is used to create the final .smali output file
- [intellij-emberjs](https://github.com/Turbo87/intellij-emberjs) –
  a nicely written plugin which uses the gradle-intellij-plugin with Kotlin
 