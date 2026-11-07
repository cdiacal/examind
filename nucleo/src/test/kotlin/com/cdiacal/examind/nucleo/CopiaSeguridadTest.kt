package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CopiaSeguridadTest {
    private val copia = CopiaSeguridad.Copia(
        creadaEl = 1_700_000_000_000,
        temarios = listOf(CopiaSeguridad.Temario(1, "Biología celular", "biología celular universidad", 42, 1_690_000_000_000)),
        fragmentos = listOf(CopiaSeguridad.Fragmento(1, 3, 0, "La célula es la unidad básica de la vida.")),
        preguntas = listOf(
            CopiaSeguridad.Pregunta(10, 1, 1_695_000_000_000, PreguntaNueva("¿Qué es la célula?", listOf("a", "b", "c", "d"), 2, "Porque sí", Origen.TEMARIO, 3)),
            CopiaSeguridad.Pregunta(11, 1, 1_695_000_000_001, PreguntaNueva("Online", listOf("a", "b", "c", "d"), 0, "", Origen.ONLINE, null, Fuente("Examen", "https://x.edu", 5)))
        ),
        tests = listOf(
            CopiaSeguridad.Test(100, 1, 1_699_000_000_000, "MEZCLA", 1, 1, 0, 5.0, 120),
            CopiaSeguridad.Test(101, null, 1_699_000_000_001, "TEMARIO", 0, 0, 1, 0.0, 10)
        ),
        respuestas = listOf(
            CopiaSeguridad.Respuesta(100, 10, 0, 2, true),
            CopiaSeguridad.Respuesta(100, 11, 1, 3, false),
            CopiaSeguridad.Respuesta(101, 10, 0, null, null)
        )
    )

    @Test fun idaYVuelta() {
        assertEquals(copia, CopiaSeguridad.desdeJson(CopiaSeguridad.aJson(copia)))
    }

    @Test fun rechazaArchivosQueNoSonCopias() {
        val e = assertThrows(CopiaSeguridad.CopiaNoValida::class.java) { CopiaSeguridad.desdeJson("hola") }
        assertTrue(e.message!!.contains("no es una copia de Examind"))
        assertThrows(CopiaSeguridad.CopiaNoValida::class.java) { CopiaSeguridad.desdeJson("""{"tipo": "otra"}""") }
    }

    @Test fun rechazaVersionesMasNuevas() {
        val e = assertThrows(CopiaSeguridad.CopiaNoValida::class.java) {
            CopiaSeguridad.desdeJson("""{"tipo": "examind-copia", "version": 99}""")
        }
        assertTrue(e.message!!.contains("Actualiza la app"))
    }

    @Test fun rechazaCopiasDanadas() {
        val e = assertThrows(CopiaSeguridad.CopiaNoValida::class.java) {
            CopiaSeguridad.desdeJson("""{"tipo": "examind-copia", "version": 1, "preguntas": [{"id": 1}]}""")
        }
        assertTrue(e.message!!.contains("no se ha restaurado nada"))
    }
}
