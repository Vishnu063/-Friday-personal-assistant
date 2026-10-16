buildscript {
    repositories { google(); mavenCentral() }
    // AGP's documented compiler upgrade path for LiteRT-LM's Kotlin 2.4 metadata.
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") }
}
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
