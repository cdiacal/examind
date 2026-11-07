package com.cdiacal.examind.nucleo

import kotlin.random.Random

/** Cuándo se ve si una respuesta es correcta. */
enum class Correccion { AL_MOMENTO, AL_FINAL }

/**
 * Una pregunta tal y como sale en el test: con las opciones ya barajadas. [orden] dice de qué opción guardada viene
 * cada una (orden[i] = índice original de la opción que se ve en la posición i).
 */
data class PreguntaEnTest(
    val id: Long,
    val enunciado: String,
    val opciones: List<String>,
    val correcta: Int,
    val orden: List<Int>,
    val explicacion: String,
    val origen: Origen,
    val pagina: Int?,
    val fuente: Fuente?
) {
    companion object {
        fun desde(id: Long, pregunta: PreguntaNueva, random: Random = Random.Default): PreguntaEnTest {
            val orden = pregunta.opciones.indices.shuffled(random)
            return PreguntaEnTest(
                id = id,
                enunciado = pregunta.enunciado,
                opciones = orden.map { pregunta.opciones[it] },
                correcta = orden.indexOf(pregunta.correcta),
                orden = orden,
                explicacion = pregunta.explicacion,
                origen = pregunta.origen,
                pagina = pregunta.pagina,
                fuente = pregunta.fuente
            )
        }
    }
}

/** Cómo quedó una pregunta al terminar. [elegidaOriginal] es el índice en las opciones guardadas; null = en blanco. */
data class RespuestaFinal(val preguntaId: Long, val orden: Int, val elegidaOriginal: Int?, val acertada: Boolean?)

data class TestTerminado(val resultado: ResultadoTest, val respuestas: List<RespuestaFinal>, val duracionSegundos: Long)

/**
 * Estado de un test en curso. Es inmutable: cada acción devuelve un estado nuevo (así la interfaz solo tiene que
 * pintar el último). [elegidas] guarda la opción elegida en cada pregunta (posición en pantalla) o null.
 */
data class SesionTest(
    val preguntas: List<PreguntaEnTest>,
    val correccion: Correccion,
    val empezadoEl: Long,
    val elegidas: List<Int?> = List(preguntas.size) { null },
    val actual: Int = 0
) {
    init {
        require(preguntas.isNotEmpty()) { "Un test necesita al menos una pregunta" }
        require(elegidas.size == preguntas.size)
    }

    val pregunta: PreguntaEnTest get() = preguntas[actual]
    val total: Int get() = preguntas.size
    val esPrimera: Boolean get() = actual == 0
    val esUltima: Boolean get() = actual == preguntas.lastIndex
    val respondidas: Int get() = elegidas.count { it != null }
    val elegidaActual: Int? get() = elegidas[actual]

    /** En «al momento», una pregunta contestada ya está corregida y no se puede cambiar. */
    fun corregida(indice: Int = actual): Boolean = correccion == Correccion.AL_MOMENTO && elegidas[indice] != null

    /** Elige una opción de la pregunta actual. En «al momento» no se puede cambiar; en «al final», sí (o quitarla). */
    fun responder(opcion: Int): SesionTest {
        require(opcion in pregunta.opciones.indices) { "Opción fuera de rango" }
        if (corregida()) return this
        val nueva = if (correccion == Correccion.AL_FINAL && elegidas[actual] == opcion) null else opcion
        return copy(elegidas = elegidas.toMutableList().also { it[actual] = nueva })
    }

    fun irA(indice: Int): SesionTest = copy(actual = indice.coerceIn(preguntas.indices))
    fun siguiente(): SesionTest = irA(actual + 1)
    fun anterior(): SesionTest = irA(actual - 1)

    /** Aciertos llevados hasta ahora (solo tiene sentido enseñarlo en «al momento»). */
    val aciertosHastaAhora: Int get() = preguntas.indices.count { elegidas[it] != null && elegidas[it] == preguntas[it].correcta }

    fun terminar(ahora: Long, penalizacion: Double = 0.0): TestTerminado {
        val respuestas = preguntas.mapIndexed { i, p ->
            val elegida = elegidas[i]
            RespuestaFinal(
                preguntaId = p.id,
                orden = i,
                elegidaOriginal = elegida?.let { p.orden[it] },
                acertada = elegida?.let { it == p.correcta }
            )
        }
        val resultado = Puntuacion.calcular(respuestas.map { it.acertada }, penalizacion)
        return TestTerminado(resultado, respuestas, ((ahora - empezadoEl) / 1000).coerceAtLeast(0))
    }
}
