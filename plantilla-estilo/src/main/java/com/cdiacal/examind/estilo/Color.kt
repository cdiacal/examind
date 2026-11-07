package com.cdiacal.examind.estilo

import androidx.compose.ui.graphics.Color

// ════════ IDENTIDAD: azul marino sobrio, estilo banca ════════
// Cabecera con degradado (claro / oscuro)
val HeroClaroInicio = Color(0xFF152C52)
val HeroClaroFin = Color(0xFF1E4B87)
val HeroOscuroInicio = Color(0xFF11223F)
val HeroOscuroFin = Color(0xFF183C6B)

// Azul de acento (barras, botones, elementos activos)
val AzulPrincipal = Color(0xFF1E4B87)
val AzulPrincipalOscuro = Color(0xFF5A9BF5)
val AzulContenedorClaro = Color(0xFFE3EAF5)
val AzulContenedorOscuro = Color(0xFF1B2C46)

// ════════ COLORES DE ESTADO (semánticos) ════════
val VerdeIngreso = Color(0xFF12734A)          // ganancia / ingreso, modo claro
val VerdeIngresoOscuro = Color(0xFF2DD48E)    // ganancia / ingreso, modo oscuro
val RojoGasto = Color(0xFFC6473A)             // gasto / pérdida, modo claro
val RojoGastoOscuro = Color(0xFFF0705F)       // gasto / pérdida, modo oscuro
val AmbarAviso = Color(0xFFD9932B)            // presupuesto cerca del límite, modo claro
val AmbarAvisoOscuro = Color(0xFFE5A93F)      // presupuesto cerca del límite, modo oscuro

// ════════ SUPERFICIES ════════
val FondoClaro = Color(0xFFF5F6FA)
val SuperficieClara = Color(0xFFFFFFFF)
val BordeClaro = Color(0xFFE6E9F0)
val TextoPrincipalClaro = Color(0xFF111420)
val TextoSecundarioClaro = Color(0xFF767C90)

val FondoOscuro = Color(0xFF0C0E16)
val SuperficieOscura = Color(0xFF161A26)
val BordeOscuro = Color(0xFF232838)
val TextoPrincipalOscuro = Color(0xFFEDEEF5)
val TextoSecundarioOscuro = Color(0xFF8189A0)

// ════════ PALETA PARA ICONOS DE CATEGORÍA/CUENTA/ACTIVO ════════
// Tonos apagados y coherentes entre sí, sin saturaciones chillonas
val PaletaIconosClaro = listOf(
    Color(0xFFE3EAF5), Color(0xFFE0EFE7), Color(0xFFF5EADF), Color(0xFFEDE6F3),
    Color(0xFFF6E4E4), Color(0xFFE1EFF2), Color(0xFFF0EEE3), Color(0xFFE7EAF0)
)
val PaletaIconosOscuro = listOf(
    Color(0xFF1E2A3E), Color(0xFF1B2F28), Color(0xFF322A20), Color(0xFF2A2438),
    Color(0xFF352424), Color(0xFF1C2E33), Color(0xFF2E2C22), Color(0xFF242833)
)

/** Color de fondo del icono, determinista a partir del id, según el modo. */
fun colorIconoPara(id: Long, oscuro: Boolean): Color {
    val paleta = if (oscuro) PaletaIconosOscuro else PaletaIconosClaro
    val indice = (id % paleta.size).let { if (it < 0) it + paleta.size else it }
    return paleta[indice.toInt()]
}

/** Compatibilidad con el gráfico de barras por categoría (colores más saturados para distinguirlas). */
val PaletaCategorias = listOf(
    Color(0xFF1E4B87), Color(0xFF12734A), Color(0xFFD9932B), Color(0xFFC6473A),
    Color(0xFF6B4E9E), Color(0xFF2A8A99), Color(0xFF8A6A3D), Color(0xFF4A5C7A)
)

fun colorParaCategoria(id: Long): Color {
    val indice = (id % PaletaCategorias.size).let { if (it < 0) it + PaletaCategorias.size else it }
    return PaletaCategorias[indice.toInt()]
}
