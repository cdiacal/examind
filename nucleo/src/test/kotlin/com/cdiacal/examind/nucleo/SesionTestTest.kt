package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SesionTestTest {
    private val base = listOf(
        PreguntaNueva("Primera pregunta", listOf("a", "b", "c", "d"), 0),
        PreguntaNueva("Segunda pregunta", listOf("e", "f", "g", "h"), 3),
        PreguntaNueva("Tercera pregunta", listOf("i", "j", "k", "l"), 1)
    )

    private fun sesion(correccion: Correccion, semilla: Int = 1) =
        SesionTest(base.mapIndexed { i, p -> PreguntaEnTest.desde(i + 10L, p, Random(semilla + i)) }, correccion, empezadoEl = 1_000)

    @Test fun barajarConservaLaCorrectaYElOrdenOriginal() {
        repeat(20) { s ->
            val p = PreguntaEnTest.desde(1, base[1], Random(s))
            assertEquals("h", p.opciones[p.correcta])
            p.opciones.forEachIndexed { i, texto -> assertEquals(base[1].opciones[p.orden[i]], texto) }
        }
    }

    @Test fun alMomentoNoDejaCambiarLaRespuesta() {
        var s = sesion(Correccion.AL_MOMENTO)
        val mala = (s.pregunta.correcta + 1) % 4
        s = s.responder(mala)
        assertTrue(s.corregida())
        s = s.responder(s.pregunta.correcta)
        assertEquals(mala, s.elegidaActual)
    }

    @Test fun alFinalDejaCambiarYQuitar() {
        var s = sesion(Correccion.AL_FINAL)
        s = s.responder(1).responder(2)
        assertEquals(2, s.elegidaActual)
        assertFalse(s.corregida())
        s = s.responder(2)
        assertNull(s.elegidaActual)
    }

    @Test fun navegarNoSeSaleDeLosLimites() {
        var s = sesion(Correccion.AL_FINAL)
        assertTrue(s.esPrimera)
        s = s.anterior()
        assertEquals(0, s.actual)
        s = s.siguiente().siguiente().siguiente()
        assertEquals(2, s.actual)
        assertTrue(s.esUltima)
    }

    @Test fun terminarCuentaYTraduceAlOrdenOriginal() {
        var s = sesion(Correccion.AL_MOMENTO)
        s = s.responder(s.pregunta.correcta).siguiente()          // acierto
        s = s.responder((s.pregunta.correcta + 1) % 4).siguiente() // fallo
        // la tercera, en blanco
        assertEquals(1, s.aciertosHastaAhora)
        assertEquals(2, s.respondidas)

        val fin = s.terminar(ahora = 61_000)
        assertEquals(1, fin.resultado.aciertos)
        assertEquals(1, fin.resultado.fallos)
        assertEquals(1, fin.resultado.enBlanco)
        assertEquals(60, fin.duracionSegundos)

        val (r1, r2, r3) = fin.respuestas
        assertEquals(RespuestaFinal(10, 0, 0, true), r1) // la correcta original de la primera es la 0
        assertEquals(false, r2.acertada)
        assertTrue(r2.elegidaOriginal != 3)
        assertEquals(RespuestaFinal(12, 2, null, null), r3)
    }
}
