plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
    // AGP 9 ya trae Kotlin para Android; este plugin fija la versión de Kotlin y el compilador de Compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
