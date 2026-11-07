package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalificacionTest {
    @Test fun tramos() {
        assertEquals(Calificacion.SUSPENSO, Calificacion.de(4.9))
        assertEquals(Calificacion.APROBADO, Calificacion.de(4.96)) // se ve «5»
        assertEquals(Calificacion.APROBADO, Calificacion.de(6.9))
        assertEquals(Calificacion.NOTABLE, Calificacion.de(7.0))
        assertEquals(Calificacion.SOBRESALIENTE, Calificacion.de(9.0))
        assertEquals(Calificacion.SOBRESALIENTE, Calificacion.de(10.0))
        assertFalse(Calificacion.SUSPENSO.aprobado)
        assertTrue(Calificacion.NOTABLE.aprobado)
    }

    @Test fun resumenSinTests() {
        val r = ResumenInicio.calcular(1, 30, emptyList())
        assertEquals(0, r.tests)
        assertNull(r.mediaAciertos)
    }

    @Test fun resumenSoloCuentaLosRecientes() {
        val recientes = List(20) { Puntuacion.calcular(listOf(true, true)) }
        val antiguo = Puntuacion.calcular(List(10) { false })
        val r = ResumenInicio.calcular(2, 50, recientes + antiguo)
        assertEquals(21, r.tests)
        assertEquals(1.0, r.mediaAciertos!!, 1e-9)
    }
}
