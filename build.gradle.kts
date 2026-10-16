buildscript {
    repositories { google(); mavenCentral() }
    // LiteRT-LM 0.18 is built with Kotlin 2.4; align its Android bytecode toolchain.
    dependencies { classpath("com.android.tools:r8:9.1.29") }
}
plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
