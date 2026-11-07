package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SelectorPreguntasTest {
    private fun temario(id: Long, respondida: Int = 0, fallada: Int = 0, ultimaFallada: Boolean = false) =
        Candidata(id, Origen.TEMARIO, respondida, fallada, ultimaFallada)

    private fun online(id: Long) = Candidata(id, Origen.ONLINE)

    @Test fun filtraPorOrigen() {
        val todas = listOf(temario(1), temario(2), online(3), online(4))
        val soloTemario = SelectorPreguntas.elegir(todas, ConfigTest(10, OrigenTest.TEMARIO), Random(1))
        assertEquals(setOf(1L, 2L), soloTemario.toSet())
        val soloOnline = SelectorPreguntas.elegir(todas, ConfigTest(10, OrigenTest.ONLINE), Random(1))
        assertEquals(setOf(3L, 4L), soloOnline.toSet())
    }

    @Test fun noRepiteYRespetaElNumero() {
        val todas = (1L..50L).map { temario(it) }
        val elegidas = SelectorPreguntas.elegir(todas, ConfigTest(20), Random(7))
        assertEquals(20, elegidas.size)
        assertEquals(20, elegidas.toSet().size)
    }

    @Test fun priorizaNuncaVistasYFalladas() {
        val dominadas = (1L..20L).map { temario(it, respondida = 5, fallada = 0) }
        val nueva = temario(100)
        val fallada = temario(101, respondida = 3, fallada = 2, ultimaFallada = true)
        repeat(20) { semilla ->
            val elegidas = SelectorPreguntas.elegir(dominadas + nueva + fallada, ConfigTest(5), Random(semilla))
            assertTrue(100L in elegidas)
            assertTrue(101L in elegidas)
        }
    }

    @Test fun soloFalladas() {
        val todas = listOf(temario(1, 2, 1, ultimaFallada = true), temario(2, 2, 1, ultimaFallada = false), temario(3))
        assertEquals(listOf(1L), SelectorPreguntas.elegir(todas, ConfigTest(10, soloFalladas = true), Random(3)))
    }

    @Test fun mezclaRepartePorMitades() {
        val todas = (1L..20L).map { temario(it) } + (101L..120L).map { online(it) }
        val elegidas = SelectorPreguntas.elegir(todas, ConfigTest(10, OrigenTest.MEZCLA), Random(5))
        assertEquals(5, elegidas.count { it < 100 })
        assertEquals(5, elegidas.count { it > 100 })
    }

    @Test fun mezclaCompletaSiUnOrigenNoLlega() {
        val todas = (1L..20L).map { temario(it) } + listOf(online(101), online(102))
        val elegidas = SelectorPreguntas.elegir(todas, ConfigTest(10, OrigenTest.MEZCLA), Random(5))
        assertEquals(10, elegidas.size)
        assertEquals(2, elegidas.count { it > 100 })
    }

    @Test fun mezclaConMenosPreguntasQueLasPedidas() {
        val todas = listOf(temario(1), online(101))
        assertEquals(2, SelectorPreguntas.elegir(todas, ConfigTest(10, OrigenTest.MEZCLA), Random(5)).size)
    }

    @Test fun barajarOpcionesMantieneLaCorrecta() {
        val opciones = listOf("a", "b", "c", "d")
        repeat(30) { semilla ->
            val (barajadas, correcta) = SelectorPreguntas.barajarOpciones(opciones, 2, Random(semilla))
            assertEquals("c", barajadas[correcta])
            assertEquals(opciones.toSet(), barajadas.toSet())
        }
    }
}
