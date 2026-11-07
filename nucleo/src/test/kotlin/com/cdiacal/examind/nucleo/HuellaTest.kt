package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HuellaTest {
    @Test fun normalizaTildesMayusculasYSignos() {
        assertEquals("que es la mitosis", normalizarTexto("  ¿Qué es la MITOSIS?  "))
    }

    @Test fun mismaHuellaAunqueCambienOrdenYFormato() {
        val a = huellaPregunta("¿Qué es la mitosis?", listOf("División celular", "Fotosíntesis", "Respiración", "Digestión"))
        val b = huellaPregunta("que es la mitosis", listOf("digestion", "Respiración.", "fotosintesis", "division celular"))
        assertEquals(a, b)
    }

    @Test fun distintaHuellaSiCambianLasOpciones() {
        val a = huellaPregunta("¿Qué es la mitosis?", listOf("A", "B", "C", "D"))
        val b = huellaPregunta("¿Qué es la mitosis?", listOf("A", "B", "C", "E"))
        assertNotEquals(a, b)
    }

    @Test fun separaDuplicadasGuardadasYDelMismoLote() {
        val p1 = PreguntaNueva("Pregunta uno", listOf("a", "b", "c", "d"), 0)
        val p2 = PreguntaNueva("Pregunta dos", listOf("a", "b", "c", "d"), 1)
        val p1Repetida = p1.copy(enunciado = "PREGUNTA UNO?")
        val p3 = PreguntaNueva("Pregunta tres", listOf("a", "b", "c", "d"), 2)

        val resultado = separarDuplicadas(listOf(p1, p2, p1Repetida, p3), setOf(p3.huella))

        assertEquals(listOf(p1, p2), resultado.nuevas)
        assertEquals(listOf(p1Repetida, p3), resultado.duplicadas)
    }
}
