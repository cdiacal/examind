plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.cdiacal.examind"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cdiacal.examind"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    // Clave de depuración fija y compartida (no es secreta): así todos los APK debug que
    // compile el workflow llevan la misma firma y se pueden instalar unos sobre otros.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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

    buildFeatures {
        compose = true
    }

    // La librería de LiteRT-LM trae su .so nativo; si llega dos veces, se usa uno
    packaging {
        jniLibs {
            pickFirsts.add("**/liblitertlm_jni.so")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // LiteRT-LM se compila con un Kotlin más nuevo; su metadato se puede leer igualmente
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

// Room guarda aquí el esquema de cada versión de la base de datos (para revisar y probar migraciones)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":plantilla-estilo"))
    implementation(project(":nucleo"))
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Room: datos locales con migraciones explícitas
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // IA en el móvil: Gemini Nano del sistema (ML Kit) y modelos Gemma/Qwen descargables (LiteRT-LM)
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta5")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.1")

    // Leer el texto de los PDF
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation("junit:junit:4.13.2")
    // Las clases org.json del SDK de Android están vacías en las pruebas locales: esta es la implementación real
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
