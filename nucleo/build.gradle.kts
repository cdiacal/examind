// Lógica pura de Examind, sin Android: se prueba con pruebas unitarias normales de JVM.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Solo para comprobar si se ha cancelado la generación entre fragmentos
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // Android ya trae org.json; aquí solo hace falta para compilar y para las pruebas
    compileOnly("org.json:json:20240303")
    testImplementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}
