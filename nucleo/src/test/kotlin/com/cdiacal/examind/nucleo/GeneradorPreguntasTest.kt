package com.cdiacal.examind.nucleo

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneradorPreguntasTest {
    private fun json(vararg enunciados: String) = enunciados.joinToString(",", "[", "]") {
        """{"pregunta": "$it", "opciones": ["uno", "dos", "tres", "cuatro"], "correcta": 1, "explicacion": "Porque sí."}"""
    }

    @Test fun elPromptLlevaElTextoLaPaginaYElFormato() {
        val prompt = PromptPreguntas.desdeTemario("  La mitosis tiene cuatro fases.  ", 7, 3)
        assertTrue(prompt.contains("escribe 3 preguntas"))
        assertTrue(prompt.contains("TEXTO (página 7)"))
        assertTrue(prompt.contains("\"\"\"\nLa mitosis tiene cuatro fases.\n\"\"\""))
        assertTrue(prompt.contains("\"opciones\""))
    }

    @Test fun juntaLasPreguntasDeTodosLosFragmentosSinDuplicar() = runBlocking {
        val respuestas = ArrayDeque(listOf(json("Pregunta A", "Pregunta B"), json("Pregunta b?", "Pregunta C")))
        val generador = GeneradorPreguntas({ respuestas.removeFirst() })
        val avances = mutableListOf<Avance>()

        val r = generador.generar(listOf(Fragmento(1, "uno"), Fragmento(4, "dos")), emptySet()) { avances += it }

        assertEquals(listOf("Pregunta A", "Pregunta B", "Pregunta C"), r.preguntas.map { it.enunciado })
        assertEquals(1, r.duplicadas)
        assertEquals(listOf(1, 1, 4), r.preguntas.map { it.pagina })
        assertEquals(Avance(2, 2, 3), avances.last())
        assertEquals(Avance(0, 2, 0), avances.first())
    }

    @Test fun sinPaginaNoSeInventaUna() = runBlocking {
        val conPaginaInventada = """[{"pregunta": "Pregunta sin página", "opciones": ["a", "b", "c", "d"], "correcta": 0, "pagina": 12}]"""
        var prompt = ""
        val r = GeneradorPreguntas({ prompt = it; conPaginaInventada }).generar(listOf(Fragmento(0, "Texto de un Word")), emptySet())
        assertNull(r.preguntas.single().pagina)
        assertTrue(prompt.contains("TEXTO:\n"))
        assertTrue(!prompt.contains("página 0"))
    }

    @Test fun noRepiteLasYaGuardadas() = runBlocking {
        val guardada = PreguntaNueva("Pregunta A", listOf("uno", "dos", "tres", "cuatro"), 1).huella
        val r = GeneradorPreguntas({ json("Pregunta A", "Pregunta B") }).generar(listOf(Fragmento(1, "x")), setOf(guardada))
        assertEquals(listOf("Pregunta B"), r.preguntas.map { it.enunciado })
        assertEquals(1, r.duplicadas)
    }

    @Test fun siUnFragmentoFallaSigueConLosDemas() = runBlocking {
        var llamada = 0
        val motor = MotorTexto {
            llamada++
            when (llamada) {
                1 -> throw IllegalStateException("sin memoria")
                2 -> "Lo siento, no puedo."
                else -> json("Pregunta buena")
            }
        }
        val r = GeneradorPreguntas(motor).generar(listOf(Fragmento(1, "a"), Fragmento(2, "b"), Fragmento(3, "c")), emptySet())
        assertEquals(1, r.preguntas.size)
        assertEquals(2, r.fragmentosFallidos)
        assertEquals("sin memoria", r.ultimoError?.message)
    }

    @Test fun sinFragmentosNoLlamaAlMotor() = runBlocking {
        val r = GeneradorPreguntas({ error("no debería llamarse") }).generar(emptyList(), emptySet())
        assertTrue(r.preguntas.isEmpty())
        assertNull(r.ultimoError)
    }
}
