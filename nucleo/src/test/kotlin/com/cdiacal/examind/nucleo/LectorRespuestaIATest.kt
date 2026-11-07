package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LectorRespuestaIATest {
    private val buena = """{"pregunta": "¿Qué orgánulo produce ATP?", "opciones": ["Mitocondria", "Ribosoma", "Lisosoma", "Núcleo"], "correcta": 0, "explicacion": "La respiración celular ocurre en la mitocondria.", "pagina": 4}"""

    @Test fun leeUnaListaCorrecta() {
        val r = LectorRespuestaIA.leer("[$buena]")
        assertEquals(1, r.preguntas.size)
        assertEquals(0, r.descartadas)
        val p = r.preguntas[0]
        assertEquals("¿Qué orgánulo produce ATP?", p.enunciado)
        assertEquals(0, p.correcta)
        assertEquals(4, p.pagina)
        assertEquals(Origen.TEMARIO, p.origen)
    }

    @Test fun aceptaTextoYBloqueDeCodigoAlrededor() {
        val r = LectorRespuestaIA.leer("Aquí tienes las preguntas:\n```json\n[$buena]\n```\nEspero que te sirvan.")
        assertEquals(1, r.preguntas.size)
    }

    @Test fun aceptaObjetoContenedor() {
        val r = LectorRespuestaIA.leer("""{"preguntas": [$buena, $buena]}""")
        assertEquals(2, r.preguntas.size)
    }

    @Test fun aprovechaLasCompletasSiLaRespuestaSeCorta() {
        val r = LectorRespuestaIA.leer("""[$buena, {"pregunta": "¿Cuál es la función del ribos""")
        assertEquals(1, r.preguntas.size)
    }

    @Test fun aprovechaLasCompletasConContenedorCortado() {
        val r = LectorRespuestaIA.leer("""{"preguntas": [$buena, $buena, {"pregunta": "a medias""")
        assertEquals(2, r.preguntas.size)
    }

    @Test fun correctaComoLetraTextoOBaseUno() {
        fun con(correcta: String, base1: Boolean = false) = LectorRespuestaIA.leer(
            """[{"pregunta": "Pregunta de prueba", "opciones": ["uno", "dos", "tres", "cuatro"], "correcta": $correcta}]""",
            base1 = base1
        ).preguntas.single().correcta
        assertEquals(1, con("\"B\""))
        assertEquals(2, con("\"c)\""))
        assertEquals(3, con("\"Opción D\""))
        assertEquals(1, con("\"dos\""))
        assertEquals(1, con("\"1\""))
        assertEquals(0, con("1", base1 = true))
    }

    @Test fun quitaLetrasDelantedeLasOpciones() {
        val r = LectorRespuestaIA.leer("""[{"pregunta": "Pregunta de prueba", "opciones": ["A) uno", "b. dos", "C: tres", "- cuatro"], "correcta": 0}]""")
        assertEquals(listOf("uno", "dos", "tres", "cuatro"), r.preguntas.single().opciones)
    }

    @Test fun descartaLasNoValidas() {
        val r = LectorRespuestaIA.leer(
            """[
              {"pregunta": "Tres opciones", "opciones": ["a", "b", "c"], "correcta": 0},
              {"pregunta": "Opciones repetidas", "opciones": ["a", "A", "b", "c"], "correcta": 0},
              {"pregunta": "Correcta fuera de rango", "opciones": ["a", "b", "c", "d"], "correcta": 7},
              {"pregunta": "", "opciones": ["a", "b", "c", "d"], "correcta": 0},
              {"pregunta": "Sin correcta", "opciones": ["a", "b", "c", "d"]},
              $buena
            ]"""
        )
        assertEquals(1, r.preguntas.size)
        assertEquals(5, r.descartadas)
    }

    @Test fun usaLaPaginaDelFragmentoSiNoLaDa() {
        val sinPagina = buena.replace(", \"pagina\": 4", "")
        val r = LectorRespuestaIA.leer("[$sinPagina]", paginaPorDefecto = 9)
        assertEquals(9, r.preguntas.single().pagina)
    }

    @Test fun guardaLaFuenteYElOrigenOnline() {
        val fuente = Fuente("Examen de Biología 2024 · Universidad X", "https://ejemplo.edu/examen.pdf", 1_700_000_000_000)
        val r = LectorRespuestaIA.leer("[$buena]", origen = Origen.ONLINE, fuente = fuente)
        assertEquals(Origen.ONLINE, r.preguntas.single().origen)
        assertEquals(fuente, r.preguntas.single().fuente)
    }

    @Test fun llavesDentroDeTextosNoConfunden() {
        val r = LectorRespuestaIA.leer("""[{"pregunta": "¿Qué indica {x} en un conjunto?", "opciones": ["a}", "{b", "c", "d"], "correcta": 0}]""")
        assertEquals("¿Qué indica {x} en un conjunto?", r.preguntas.single().enunciado)
    }

    @Test fun textoSinJsonNoDaNada() {
        val r = LectorRespuestaIA.leer("Lo siento, no puedo generar preguntas.")
        assertTrue(r.vacio)
        assertEquals(0, r.descartadas)
    }

    @Test fun limpiarOpcionNoTocaPalabrasNormales() {
        assertEquals("Agua", LectorRespuestaIA.limpiarOpcion("Agua"))
        assertEquals("1945", LectorRespuestaIA.limpiarOpcion("1945"))
        assertEquals("A - B", LectorRespuestaIA.limpiarOpcion("A - B"))
    }
}
