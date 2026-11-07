package com.cdiacal.examind.estilo

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Permite a cualquier componente saber si está pintándose en modo oscuro (los colores de estado cambian de tono). */
val LocalEsModoOscuro = compositionLocalOf { false }

/** Preferencia de tema del usuario: seguir al sistema, siempre claro o siempre oscuro. */
enum class ModoTema(val clave: String) {
    SISTEMA("sistema"), CLARO("claro"), OSCURO("oscuro");

    companion object {
        fun deClave(clave: String?): ModoTema = values().firstOrNull { it.clave == clave } ?: SISTEMA
    }
}

/**
 * La preferencia de tema como estado de Compose, guardada en SharedPreferences. Cambiarla con [establecer] repinta
 * toda la app al instante, sin cerrarla. Llama a [cargar] una vez al arrancar (por ejemplo en `onCreate`).
 */
object TemaState {
    private const val ARCHIVO = "estilo_prefs"
    private const val CLAVE = "tema"

    var modo by mutableStateOf(ModoTema.SISTEMA)
        private set

    private var cargado = false

    fun cargar(context: Context) {
        if (cargado) return
        modo = ModoTema.deClave(context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getString(CLAVE, null))
        cargado = true
    }

    fun establecer(context: Context, nuevoModo: ModoTema) {
        modo = nuevoModo
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putString(CLAVE, nuevoModo.clave).apply()
    }
}

private val EsquemaClaro = lightColorScheme(
    primary = AzulPrincipal,
    onPrimary = SuperficieClara,
    primaryContainer = AzulContenedorClaro,
    onPrimaryContainer = AzulPrincipal,
    secondary = VerdeIngreso,
    secondaryContainer = Color(0xFFE0EFE7),
    background = FondoClaro,
    onBackground = TextoPrincipalClaro,
    surface = SuperficieClara,
    onSurface = TextoPrincipalClaro,
    surfaceVariant = BordeClaro,
    onSurfaceVariant = TextoSecundarioClaro,
    outline = BordeClaro,
    error = RojoGasto
)

private val EsquemaOscuro = darkColorScheme(
    primary = AzulPrincipalOscuro,
    onPrimary = FondoOscuro,
    primaryContainer = AzulContenedorOscuro,
    onPrimaryContainer = AzulPrincipalOscuro,
    secondary = VerdeIngresoOscuro,
    secondaryContainer = AzulContenedorOscuro,
    background = FondoOscuro,
    onBackground = TextoPrincipalOscuro,
    surface = SuperficieOscura,
    onSurface = TextoPrincipalOscuro,
    surfaceVariant = BordeOscuro,
    onSurfaceVariant = TextoSecundarioOscuro,
    outline = BordeOscuro,
    error = RojoGastoOscuro
)

/**
 * El tema de la app. Envuelve toda la interfaz: `setContent { EstiloTheme { MiPantalla() } }`.
 * Por defecto lee la preferencia de [TemaState]; en las vistas previas se le puede pasar [modo] directamente.
 */
@Composable
fun EstiloTheme(modo: ModoTema = TemaState.modo, content: @Composable () -> Unit) {
    val oscuro = when (modo) {
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
        ModoTema.SISTEMA -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalEsModoOscuro provides oscuro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            content = content
        )
    }
}
