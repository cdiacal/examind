package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TroceadorTest {
    @Test fun detectaPdfEscaneadoSinTexto() {
        assertFalse(Troceador.tieneTexto(listOf("", " 1 ", "")))
        assertFalse(Troceador.tieneTexto(emptyList()))
        assertTrue(Troceador.tieneTexto(listOf("La célula es la unidad básica de la vida. ".repeat(5))))
    }

    @Test fun unePalabrasPartidasConGuion() {
        val limpio = Troceador.limpiarPaginas(listOf("La mito-\nsis es un proceso."))
        assertEquals("La mitosis es un proceso.", limpio[0])
    }

    @Test fun noUneGuionAntesDeMayuscula() {
        val limpio = Troceador.limpiarPaginas(listOf("Norte-\nSur"))
        assertEquals("Norte-\nSur", limpio[0])
    }

    @Test fun quitaNumerosDePaginaYCabecerasRepetidas() {
        val paginas = (1..4).map { n -> "Tema 3. La célula\nContenido propio de la página $n.\n$n" } +
            listOf("Tema 3. La célula\nÚltima página.\nPágina 5 de 5")
        val limpio = Troceador.limpiarPaginas(paginas)
        assertEquals("Contenido propio de la página 1.", limpio[0])
        assertEquals("Última página.", limpio[4])
    }

    @Test fun noQuitaLineasRepetidasConPocasPaginas() {
        val limpio = Troceador.limpiarPaginas(listOf("Título\nUno", "Título\nDos"))
        assertEquals("Título\nUno", limpio[0])
    }

    @Test fun agrupaParrafosHastaElMaximoYGuardaLaPagina() {
        val parrafo = "Frase de ejemplo con bastante texto. ".repeat(4).trim() // 147 caracteres
        val paginas = listOf("$parrafo\n\n$parrafo", "$parrafo\n\n$parrafo")
        val fragmentos = Troceador.trocear(paginas, maximo = 320)

        assertEquals(2, fragmentos.size)
        assertEquals(1, fragmentos[0].pagina)
        assertEquals(2, fragmentos[1].pagina)
        assertTrue(fragmentos.all { it.texto.length <= 320 })
    }

    @Test fun parteParrafosMuyLargosPorFrases() {
        val largo = (1..40).joinToString(" ") { "Esta es la frase número $it." }
        val fragmentos = Troceador.trocear(listOf(largo), maximo = 300)
        assertTrue(fragmentos.size > 1)
        assertTrue(fragmentos.all { it.texto.length <= 300 })
        assertTrue(fragmentos.all { it.texto.endsWith(".") })
        assertEquals(largo, fragmentos.joinToString(" ") { it.texto })
    }

    @Test fun parteFrasesSinPuntoPorPalabras() {
        val largo = "palabra ".repeat(200).trim()
        val fragmentos = Troceador.trocear(listOf(largo), maximo = 250)
        assertTrue(fragmentos.all { it.texto.length <= 250 })
        assertEquals(200, fragmentos.sumOf { it.texto.split(' ').size })
    }

    @Test fun unaLineaPartidaDentroDeUnParrafoSeJunta() {
        val fragmentos = Troceador.trocear(listOf("Primera línea\nsigue aquí.\n\nOtro párrafo."))
        assertEquals("Primera línea sigue aquí.\n\nOtro párrafo.", fragmentos.single().texto)
    }

    @Test fun paginasVaciasNoGeneranFragmentos() {
        assertEquals(emptyList<Fragmento>(), Troceador.trocear(listOf("", "  ")))
    }
}
