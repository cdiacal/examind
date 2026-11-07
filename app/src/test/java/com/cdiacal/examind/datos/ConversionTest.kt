package com.cdiacal.examind.datos

import com.cdiacal.examind.nucleo.Fuente
import com.cdiacal.examind.nucleo.Origen
import com.cdiacal.examind.nucleo.PreguntaNueva
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversionTest {
    @Test fun preguntaIdaYVuelta() {
        val nueva = PreguntaNueva(
            "¿Qué es un examen?", listOf("a", "b", "c", "d"), 1, "Explicación", Origen.ONLINE, null,
            Fuente("Examen 2024", "https://ejemplo.edu/e.pdf", 123)
        )
        val entidad = nueva.aEntidad(temarioId = 7, creadaEl = 99)
        assertEquals(7, entidad.temarioId)
        assertEquals("ONLINE", entidad.origen)
        assertEquals(nueva.huella, entidad.huella)
        assertEquals(nueva, entidad.aNueva())
    }

    @Test fun preguntaSinFuente() {
        val nueva = PreguntaNueva("¿Pregunta del temario?", listOf("a", "b", "c", "d"), 0, pagina = 12)
        assertEquals(nueva, nueva.aEntidad(1, 1).aNueva())
    }

    @Test fun opcionesComoJson() {
        val c = Conversores()
        val opciones = listOf("Con \"comillas\"", "Con, coma", "Ñandú", "")
        assertEquals(opciones, c.jsonAOpciones(c.opcionesAJson(opciones)))
    }

    @Test fun candidata() {
        val nunca = FilaCandidata(1, "TEMARIO", 0, 0, null).aCandidata()
        assertFalse(nunca.ultimaFallada)
        val fallada = FilaCandidata(2, "ONLINE", 3, 1, false).aCandidata()
        assertEquals(2, fallada.vecesFallada)
        assertTrue(fallada.ultimaFallada)
        assertEquals(Origen.ONLINE, fallada.origen)
        val enBlanco = FilaCandidata(3, "TEMARIO", 1, 0, null).aCandidata()
        assertTrue(enBlanco.ultimaFallada)
    }
}
