package com.cdiacal.examind.nucleo

/** De dónde sale una pregunta: del PDF del temario o de un examen encontrado en internet. */
enum class Origen { TEMARIO, ONLINE }

/** Fuente de una pregunta sacada de internet. [consultadaEl] en milisegundos desde 1970. */
data class Fuente(val titulo: String, val url: String, val consultadaEl: Long)

/**
 * Una pregunta tipo test todavía sin guardar (la acaba de generar la IA o se está importando).
 * [correcta] es el índice de la opción buena dentro de [opciones]. [pagina] es la del PDF, si se sabe.
 */
data class PreguntaNueva(
    val enunciado: String,
    val opciones: List<String>,
    val correcta: Int,
    val explicacion: String = "",
    val origen: Origen = Origen.TEMARIO,
    val pagina: Int? = null,
    val fuente: Fuente? = null
) {
    val huella: String get() = huellaPregunta(enunciado, opciones)
}
