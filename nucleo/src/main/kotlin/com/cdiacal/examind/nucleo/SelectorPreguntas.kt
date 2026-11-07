package com.cdiacal.examind.nucleo

import kotlin.random.Random

/** Qué preguntas entran en un test. */
enum class OrigenTest { TEMARIO, ONLINE, MEZCLA }

data class ConfigTest(
    val numPreguntas: Int = 10,
    val origen: OrigenTest = OrigenTest.TEMARIO,
    val soloFalladas: Boolean = false
)

/** Lo que el selector necesita saber de cada pregunta guardada. */
data class Candidata(
    val id: Long,
    val origen: Origen,
    val vecesRespondida: Int = 0,
    val vecesFallada: Int = 0,
    /** true si la última vez que salió se falló (o se dejó en blanco). */
    val ultimaFallada: Boolean = false
)

object SelectorPreguntas {

    /**
     * Elige las preguntas de un test. Da prioridad a las que nunca han salido y a las que más se fallan, pero con
     * algo de azar para que los tests no se repitan. En «Mezcla» intenta repartir mitad y mitad; si un origen no
     * llega, completa con el otro. Devuelve los ids en el orden en que saldrán.
     */
    fun elegir(candidatas: List<Candidata>, config: ConfigTest, random: Random = Random.Default): List<Long> {
        val validas = candidatas.filter { c ->
            when (config.origen) {
                OrigenTest.TEMARIO -> c.origen == Origen.TEMARIO
                OrigenTest.ONLINE -> c.origen == Origen.ONLINE
                OrigenTest.MEZCLA -> true
            } && (!config.soloFalladas || c.ultimaFallada)
        }
        val n = config.numPreguntas.coerceAtLeast(0)
        val elegidas = if (config.origen == OrigenTest.MEZCLA) {
            val delTemario = ordenar(validas.filter { it.origen == Origen.TEMARIO }, random)
            val online = ordenar(validas.filter { it.origen == Origen.ONLINE }, random)
            var deTemario = minOf((n + 1) / 2, delTemario.size)
            val deOnline = minOf(n - deTemario, online.size)
            // Si internet no llega a su mitad, se completa con más del temario
            deTemario = minOf(n - deOnline, delTemario.size)
            delTemario.take(deTemario) + online.take(deOnline)
        } else {
            ordenar(validas, random).take(n)
        }
        return elegidas.shuffled(random).map { it.id }
    }

    /** Ordena por prioridad (más alta primero) con un poco de azar para desempatar y variar. */
    private fun ordenar(lista: List<Candidata>, random: Random): List<Candidata> =
        lista.map { it to prioridad(it) + random.nextDouble() }.sortedByDescending { it.second }.map { it.first }

    /** Nunca vista: 2. Última fallada: +1,5. Más el porcentaje de fallos (0..1). */
    internal fun prioridad(c: Candidata): Double {
        if (c.vecesRespondida == 0) return 2.0
        val tasaFallos = c.vecesFallada.toDouble() / c.vecesRespondida
        return tasaFallos + if (c.ultimaFallada) 1.5 else 0.0
    }

    /** Baraja las opciones de una pregunta y devuelve dónde ha quedado la correcta. */
    fun barajarOpciones(opciones: List<String>, correcta: Int, random: Random = Random.Default): Pair<List<String>, Int> {
        val orden = opciones.indices.shuffled(random)
        return orden.map { opciones[it] } to orden.indexOf(correcta)
    }
}
