package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusquedaWebTest {
    // Estructura de html.duckduckgo.com/html (la que leen también librerías como ddgs)
    private val htmlDdg = """
<div class="result results_links results_links_deep web-result ">
  <div class="links_main links_deep result__body">
    <h2 class="result__title">
      <a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.ejemplo.edu%2Fexamenes%2Fbiologia%2D2024.pdf&amp;rut=abc">Examen de <b>Biología</b> celular 2024 &#8211; soluciones</a>
    </h2>
    <a class="result__snippet" href="//duckduckgo.com/l/?uddg=x">Preguntas <b>tipo test</b> con soluciones &amp; explicaciones.</a>
  </div>
</div>
<div class="result results_links results_links_deep result--ad">
  <div class="links_main links_deep result__body">
    <h2 class="result__title"><a rel="nofollow" class="result__a" href="https://duckduckgo.com/y.js?ad_domain=anuncio.com">Anuncio</a></h2>
  </div>
</div>
<div class="result results_links results_links_deep web-result ">
  <div class="links_main links_deep result__body">
    <h2 class="result__title"><a rel="nofollow" class="result__a" href="https://test.ejemplo.org/celula">Test de la célula</a></h2>
    <a class="result__snippet" href="https://test.ejemplo.org/celula">20 preguntas.</a>
  </div>
</div>"""

    // Estructura de mojeek.com/search
    private val htmlMojeek = """
<ul class="results-standard">
<li class=""><a class="ob" href="https://www.uni.es/bio/test.html">www.uni.es › bio</a>
<h2><a class="title" href="https://www.uni.es/bio/test.html?a=1&amp;b=2">Test de biología &ntilde;</a></h2>
<p class="s">Banco de preguntas de <strong>biología</strong>.</p></li>
<li><h2><a class="title" href="/relativa">Relativa</a></h2><p class="s">no vale</p></li>
</ul>"""

    @Test fun leeDuckDuckGo() {
        val r = BusquedaWeb.deDuckDuckGo(htmlDdg)
        assertEquals(2, r.size)
        assertEquals("Examen de Biología celular 2024 – soluciones", r[0].titulo)
        assertEquals("https://www.ejemplo.edu/examenes/biologia-2024.pdf", r[0].url)
        assertEquals("Preguntas tipo test con soluciones & explicaciones.", r[0].resumen)
        assertTrue(r[0].esPdf)
        assertEquals("ejemplo.edu", r[0].dominio)
        assertEquals("https://test.ejemplo.org/celula", r[1].url)
        assertFalse(r[1].esPdf)
    }

    @Test fun leeMojeek() {
        val r = BusquedaWeb.deMojeek(htmlMojeek)
        assertEquals(1, r.size)
        assertEquals("Test de biología ñ", r[0].titulo)
        assertEquals("https://www.uni.es/bio/test.html?a=1&b=2", r[0].url)
        assertEquals("Banco de preguntas de biología.", r[0].resumen)
    }

    @Test fun paginaSinResultados() {
        assertTrue(BusquedaWeb.deDuckDuckGo("<html><body>No results.</body></html>").isEmpty())
        assertTrue(BusquedaWeb.deMojeek("<html></html>").isEmpty())
    }

    @Test fun unirIntercalaSinRepetirNiDescartados() {
        val a = listOf(
            ResultadoWeb("A1", "https://www.uno.es/a/", ""),
            ResultadoWeb("Vídeo", "https://www.youtube.com/watch?v=1", ""),
            ResultadoWeb("A2", "https://dos.es/b", "")
        )
        val b = listOf(ResultadoWeb("B1 (repetido)", "http://uno.es/a", ""), ResultadoWeb("B2", "https://tres.es", ""))
        assertEquals(listOf("A1", "B2", "A2"), BusquedaWeb.unir(listOf(a, b)).map { it.titulo })
        assertEquals(1, BusquedaWeb.unir(listOf(a, b), maximo = 1).size)
    }

    @Test fun consultas() {
        val c = BusquedaWeb.consultas("  Biología   celular ")
        assertEquals(3, c.size)
        assertTrue(c[0].startsWith("Biología celular examen tipo test"))
        assertTrue(c.any { it.contains("filetype:pdf") })
        assertTrue(BusquedaWeb.consultas(" ").isEmpty())
    }

    @Test fun urlPegadaAMano() {
        assertEquals("https://uni.es/examen.pdf", BusquedaWeb.urlValida(" uni.es/examen.pdf "))
        assertEquals("http://uni.es", BusquedaWeb.urlValida("http://uni.es"))
        assertNull(BusquedaWeb.urlValida("esto no es una web"))
        assertNull(BusquedaWeb.urlValida("localhost"))
    }

    @Test fun textoDeUnaPagina() {
        val html = """<html><head><title>Test &amp; soluciones</title><style>p{color:red}</style><script>var x=1;</script></head>
<body><nav>Inicio | Contacto</nav><h1>Examen</h1><p>1. ¿Qué es la <b>mitosis</b>?</p><ul><li>a) División</li><li>b) Fusión</li></ul>
<!-- comentario --><footer>© 2024</footer></body></html>"""
        val p = TextoWeb.desdeHtml(html)
        assertEquals("Test & soluciones", p.titulo)
        assertTrue(p.texto.contains("1. ¿Qué es la mitosis?"))
        assertTrue(p.texto.contains("a) División"))
        assertFalse(p.texto.contains("color:red"))
        assertFalse(p.texto.contains("var x"))
        assertFalse(p.texto.contains("Contacto"))
        assertFalse(p.texto.contains("2024"))
        assertFalse(p.texto.contains("comentario"))
    }
}
