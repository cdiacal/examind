pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Examind"
include(":app")
// Lógica pura sin Android (lectores, cálculo, CSV, copia de seguridad), con sus pruebas
include(":nucleo")
// Estilo visual compartido con Ceca (ver plantilla-estilo/ESTILO.md)
include(":plantilla-estilo")
