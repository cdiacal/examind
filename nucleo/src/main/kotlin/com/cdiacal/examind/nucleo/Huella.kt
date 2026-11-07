package com.cdiacal.examind.nucleo

import java.security.MessageDigest
import java.text.Normalizer

/** Minúsculas, sin tildes, sin signos y con un solo espacio entre palabras. Sirve para comparar textos. */
fun normalizarTexto(texto: String): String {
    val sinTildes = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    return sinTildes.replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
}

/**
 * Huella de una pregunta para no guardarla dos veces: no cambia por mayúsculas, tildes, signos ni el orden de las
 * opciones. Dos preguntas con el mismo enunciado y las mismas opciones tienen la misma huella.
 */
fun huellaPregunta(enunciado: String, opciones: List<String>): String {
    val base = normalizarTexto(enunciado) + "|" + opciones.map(::normalizarTexto).sorted().joinToString("|")
    val bytes = MessageDigest.getInstance("SHA-256").digest(base.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }.take(32)
}

/** Resultado de separar las preguntas nuevas de las que ya estaban (o se repiten dentro del mismo lote). */
data class Separacion(val nuevas: List<PreguntaNueva>, val duplicadas: List<PreguntaNueva>)

/** Separa lo nuevo de lo repetido, comparando con las huellas ya guardadas y dentro del propio lote. */
fun separarDuplicadas(lote: List<PreguntaNueva>, huellasGuardadas: Set<String>): Separacion {
    val vistas = huellasGuardadas.toMutableSet()
    val nuevas = mutableListOf<PreguntaNueva>()
    val duplicadas = mutableListOf<PreguntaNueva>()
    for (pregunta in lote) {
        if (vistas.add(pregunta.huella)) nuevas += pregunta else duplicadas += pregunta
    }
    return Separacion(nuevas, duplicadas)
}
