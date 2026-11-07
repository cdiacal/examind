package com.cdiacal.examind.estilo

/** Tecla de borrar del teclado numérico de "Nuevo movimiento" (junto a los dígitos, ',' y '+'). */
const val TECLA_BORRAR = '<'

/** Máximo de cifras enteras de cada sumando: evita importes absurdos al tocar sin mirar. */
private const val MAX_CIFRAS_ENTERAS = 7

/**
 * Aplica una pulsación del teclado numérico propio al importe escrito ("12,5+3"). Devuelve el texto nuevo; si la
 * tecla no tiene sentido (dos comas en un número, un tercer decimal, un "+" sin número antes) devuelve el mismo.
 * Lo escrito es lo que entiende [evaluarCantidad]. No depende de Android.
 */
fun teclear(texto: String, tecla: Char): String {
    val sumando = texto.substringAfterLast('+')
    val posicionSeparador = sumando.indexOfFirst { it == ',' || it == '.' }
    return when {
        tecla == TECLA_BORRAR -> texto.dropLast(1)
        tecla == '+' -> if (texto.isEmpty() || texto.endsWith("+")) texto else "$texto+"
        tecla == ',' -> when {
            posicionSeparador >= 0 -> texto
            sumando.isEmpty() -> "${texto}0,"
            else -> "$texto,"
        }
        tecla in '0'..'9' -> when {
            // Con coma: como mucho dos decimales
            posicionSeparador >= 0 -> if (sumando.length - posicionSeparador - 1 >= 2) texto else texto + tecla
            // Sin coma: un 0 inicial se sustituye ("0" + "5" = "5", no "05")
            sumando == "0" -> texto.dropLast(1) + tecla
            sumando.length >= MAX_CIFRAS_ENTERAS -> texto
            else -> texto + tecla
        }
        else -> texto
    }
}

/** El importe escrito tal como se ve en pantalla: coma decimal y sumas con espacios ("12,5 + 3"); "0" si está vacío. */
fun importeParaMostrar(texto: String): String =
    if (texto.isEmpty()) "0" else texto.replace('.', ',').replace("+", " + ")
