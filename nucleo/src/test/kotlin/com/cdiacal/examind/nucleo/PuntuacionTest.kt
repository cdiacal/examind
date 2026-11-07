package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuntuacionTest {
    @Test fun cuentaAciertosFallosYBlancos() {
        val r = Puntuacion.calcular(listOf(true, true, false, null, true))
        assertEquals(3, r.aciertos)
        assertEquals(1, r.fallos)
        assertEquals(1, r.enBlanco)
        assertEquals(5, r.total)
        assertEquals(6.0, r.nota, 1e-9)
        assertEquals(0.6, r.proporcionAciertos, 1e-9)
    }

    @Test fun penalizacionRestaPorFallo() {
        // 6 aciertos, 3 fallos, 1 en blanco con -1/3: (6 - 1) / 10 * 10 = 5
        val r = Puntuacion.calcular(List(6) { true } + List(3) { false } + listOf(null), penalizacion = 1.0 / 3)
        assertEquals(5.0, r.nota, 1e-9)
    }

    @Test fun laNotaNoBajaDeCero() {
        assertEquals(0.0, Puntuacion.calcular(listOf(false, false, true), penalizacion = 1.0).nota, 1e-9)
    }

    @Test fun testVacio() {
        val r = Puntuacion.calcular(emptyList())
        assertEquals(0.0, r.nota, 1e-9)
        assertEquals(0.0, r.proporcionAciertos, 1e-9)
    }

    @Test fun mediaPesadaPorPreguntas() {
        val a = Puntuacion.calcular(List(10) { true })          // 10/10
        val b = Puntuacion.calcular(listOf(false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true))
        // 10 + 20 aciertos de 40
        assertEquals(30.0 / 40, Puntuacion.mediaAciertos(listOf(a, b))!!, 1e-9)
        assertNull(Puntuacion.mediaAciertos(emptyList()))
    }
}
