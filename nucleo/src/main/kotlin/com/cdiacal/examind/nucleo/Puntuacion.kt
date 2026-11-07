package com.cdiacal.examind.nucleo

/** Resultado de un test. [nota] va de 0 a 10. */
data class ResultadoTest(
    val aciertos: Int,
    val fallos: Int,
    val enBlanco: Int,
    val nota: Double
) {
    val total: Int get() = aciertos + fallos + enBlanco
    /** Proporción de aciertos sobre el total (0..1). */
    val proporcionAciertos: Double get() = if (total == 0) 0.0 else aciertos.toDouble() / total
}

object Puntuacion {

    /**
     * Calcula el resultado. Cada elemento de [respuestas] es true (acierto), false (fallo) o null (en blanco).
     * [penalizacion]: lo que resta cada fallo en «aciertos» (0 en universidad; 1/3 si con 4 opciones se quiere
     * que contestar al azar no compense). La nota nunca baja de 0.
     */
    fun calcular(respuestas: List<Boolean?>, penalizacion: Double = 0.0): ResultadoTest {
        require(penalizacion >= 0.0) { "La penalización no puede ser negativa" }
        val aciertos = respuestas.count { it == true }
        val fallos = respuestas.count { it == false }
        val enBlanco = respuestas.count { it == null }
        val total = respuestas.size
        val nota = if (total == 0) 0.0 else ((aciertos - fallos * penalizacion) / total * 10).coerceIn(0.0, 10.0)
        return ResultadoTest(aciertos, fallos, enBlanco, nota)
    }

    /** Media de aciertos de varios tests, pesando cada uno por su número de preguntas. null si no hay ninguno. */
    fun mediaAciertos(resultados: List<ResultadoTest>): Double? {
        val total = resultados.sumOf { it.total }
        if (total == 0) return null
        return resultados.sumOf { it.aciertos }.toDouble() / total
    }
}
