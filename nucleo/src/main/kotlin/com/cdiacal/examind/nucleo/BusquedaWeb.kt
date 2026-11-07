package com.cdiacal.examind.nucleo

import java.net.URI
import java.net.URLDecoder

/** Un resultado de búsqueda: título, dirección y el resumen que da el buscador. */
data class ResultadoWeb(val titulo: String, val url: String, val resumen: String) {
    val dominio: String get() = BusquedaWeb.dominio(url)
    val esPdf: Boolean get() = url.substringBefore('?').substringBefore('#').endsWith(".pdf", ignoreCase = true)
}

/**
 * Búsqueda de exámenes en internet sin claves: qué buscar, cómo leer las páginas de resultados de DuckDuckGo y Mojeek
 * (versión HTML sencilla) y cómo quedarse con lo útil. Código puro: la descarga la hace la app.
 */
object BusquedaWeb {

    /** Sitios que casi nunca tienen exámenes en texto (vídeo, redes, tiendas). */
    private val DESCARTADOS = listOf(
        "youtube.com", "youtu.be", "facebook.com", "instagram.com", "tiktok.com", "pinterest.", "twitter.com", "x.com",
        "amazon.", "ebay.", "linkedin.com", "duckduckgo.com", "mojeek.com", "spotify.com"
    )

    /** Las búsquedas que se hacen para un tema: en español y en inglés, y una que pide PDF. */
    fun consultas(tema: String): List<String> {
        val t = tema.trim().replace(Regex("\\s+"), " ")
        if (t.isEmpty()) return emptyList()
        return listOf(
            "$t examen tipo test preguntas con soluciones",
            "$t exam multiple choice questions answers",
            "$t examen test filetype:pdf"
        )
    }

    fun dominio(url: String): String = runCatching { URI(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")

    /** Junta los resultados de varias búsquedas, sin repetir direcciones ni sitios descartados, hasta [maximo]. */
    fun unir(listas: List<List<ResultadoWeb>>, maximo: Int = 15): List<ResultadoWeb> {
        val vistas = mutableSetOf<String>()
        val salida = mutableListOf<ResultadoWeb>()
        // Se intercalan las listas para que salgan resultados de todas las búsquedas
        val n = listas.maxOfOrNull { it.size } ?: 0
        for (i in 0 until n) for (lista in listas) {
            val r = lista.getOrNull(i) ?: continue
            val clave = normalizarUrl(r.url)
            if (!r.url.startsWith("http") || DESCARTADOS.any { r.dominio.contains(it) }) continue
            if (vistas.add(clave)) salida += r
            if (salida.size >= maximo) return salida
        }
        return salida
    }

    /** Para comparar direcciones: sin «www.», sin «#…», sin barra final y sin http/https. */
    fun normalizarUrl(url: String): String =
        url.trim().substringBefore('#').removeSuffix("/")
            .replace(Regex("^https?://(www\\.)?", RegexOption.IGNORE_CASE), "").lowercase()

    /** Comprueba y arregla una dirección pegada a mano. Devuelve null si no es una dirección web. */
    fun urlValida(texto: String): String? {
        val t = texto.trim()
        val conEsquema = if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(t)) t else "https://$t"
        val uri = runCatching { URI(conEsquema) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (!host.contains('.') || host.contains(' ')) return null
        return conEsquema
    }

    /**
     * Resultados de html.duckduckgo.com: cada uno es un `<a class="result__a" href="…">título</a>` seguido de un
     * `<a class="result__snippet">resumen</a>`. Los anuncios (y.js) se descartan; los enlaces de redirección
     * (`/l/?uddg=…`) se convierten en la dirección real.
     */
    fun deDuckDuckGo(html: String): List<ResultadoWeb> {
        val enlaces = Regex("<a\\b[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*>(.*?)</a>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val resumen = Regex("class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</(a|div|td)>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val encontrados = enlaces.findAll(html).toList()
        return encontrados.mapIndexedNotNull { i, m ->
            val href = atributo(m.value, "href") ?: return@mapIndexedNotNull null
            val url = urlReal(href) ?: return@mapIndexedNotNull null
            if (url.contains("duckduckgo.com/y.js")) return@mapIndexedNotNull null
            val hasta = encontrados.getOrNull(i + 1)?.range?.first ?: html.length
            val trozo = html.substring(m.range.last + 1, hasta)
            ResultadoWeb(limpiarHtml(m.groupValues[1]), url, resumen.find(trozo)?.groupValues?.get(1)?.let(::limpiarHtml).orEmpty())
        }
    }

    /** Resultados de mojeek.com: `<ul class="results-standard"><li>…<h2><a href="…">título</a></h2>…<p class="s">resumen</p>`. */
    fun deMojeek(html: String): List<ResultadoWeb> {
        val lista = Regex("<ul\\b[^>]*class=\"[^\"]*results[^\"]*\"[^>]*>(.*?)</ul>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .findAll(html).joinToString("") { it.groupValues[1] }
        val items = Regex("<li\\b[^>]*>(.*?)</li>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).findAll(lista)
        return items.mapNotNull { li ->
            val bloque = li.groupValues[1]
            val h2 = Regex("<h2\\b[^>]*>.*?(<a\\b[^>]*>)(.*?)</a>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(bloque) ?: return@mapNotNull null
            val url = atributo(h2.groupValues[1], "href")?.let(::decodificarEntidades) ?: return@mapNotNull null
            val resumen = Regex("<p\\b[^>]*class=\"s\"[^>]*>(.*?)</p>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).find(bloque)?.groupValues?.get(1)
            ResultadoWeb(limpiarHtml(h2.groupValues[2]), url, resumen?.let(::limpiarHtml).orEmpty())
        }.filter { it.url.startsWith("http") }.toList()
    }

    private fun atributo(etiqueta: String, nombre: String): String? =
        Regex("\\b$nombre\\s*=\\s*(\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE).find(etiqueta)?.let { it.groupValues[2].ifEmpty { it.groupValues[3] } }

    /** «//duckduckgo.com/l/?uddg=https%3A%2F%2F…&rut=…» → «https://…»; las direcciones normales se quedan igual. */
    internal fun urlReal(href: String): String? {
        val h = decodificarEntidades(href)
        val uddg = Regex("[?&]uddg=([^&]+)").find(h)?.groupValues?.get(1)
        val url = if (uddg != null) URLDecoder.decode(uddg, "UTF-8") else if (h.startsWith("//")) "https:$h" else h
        return url.takeIf { it.startsWith("http") }
    }

    /** Quita etiquetas, decodifica entidades y deja un solo espacio. */
    fun limpiarHtml(html: String): String =
        decodificarEntidades(html.replace(ETIQUETAS_EN_LINEA, "").replace(Regex("<[^>]+>"), " ")).replace(Regex("\\s+"), " ").trim()

    /** Negritas, cursivas, enlaces…: se quitan sin dejar espacio, para no partir palabras ni separar la puntuación. */
    internal val ETIQUETAS_EN_LINEA = Regex("</?(b|strong|i|em|u|span|a|mark|small|sup|sub|abbr|code)\\b[^>]*>", RegexOption.IGNORE_CASE)

    fun decodificarEntidades(texto: String): String {
        if (!texto.contains('&')) return texto
        return Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);").replace(texto) { m ->
            val e = m.groupValues[1]
            when {
                e.startsWith("#x") || e.startsWith("#X") -> e.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                e.startsWith("#") -> e.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                else -> ENTIDADES[e]
            } ?: m.value
        }
    }

    private val ENTIDADES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "aacute" to "á", "eacute" to "é", "iacute" to "í", "oacute" to "ó", "uacute" to "ú", "ntilde" to "ñ",
        "Aacute" to "Á", "Eacute" to "É", "Iacute" to "Í", "Oacute" to "Ó", "Uacute" to "Ú", "Ntilde" to "Ñ",
        "uuml" to "ü", "Uuml" to "Ü", "iexcl" to "¡", "iquest" to "¿", "laquo" to "«", "raquo" to "»",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“",
        "middot" to "·", "deg" to "°", "copy" to "©", "reg" to "®", "euro" to "€"
    )
}

/** El texto legible de una página web (sin menús, scripts ni estilos), para dárselo a la IA. */
object TextoWeb {

    data class Pagina(val titulo: String, val texto: String)

    fun desdeHtml(html: String): Pagina {
        val titulo = Regex("<title\\b[^>]*>(.*?)</title>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(html)?.groupValues?.get(1)?.let(BusquedaWeb::limpiarHtml).orEmpty()
        var cuerpo = html
        // Fuera lo que no es contenido
        for (etiqueta in listOf("script", "style", "noscript", "svg", "nav", "header", "footer", "aside", "form", "iframe", "template")) {
            cuerpo = cuerpo.replace(Regex("<$etiqueta\\b.*?</$etiqueta>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
        }
        cuerpo = cuerpo.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), " ")
        // Los bloques pasan a párrafos; las líneas, a saltos de línea
        cuerpo = cuerpo.replace(Regex("</?(p|div|section|article|h[1-6]|li|ul|ol|table|tr|blockquote|pre|dd|dt)\\b[^>]*>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<br\\s*/?>|</td>|</th>", RegexOption.IGNORE_CASE), "\n")
            .replace(BusquedaWeb.ETIQUETAS_EN_LINEA, "")
            .replace(Regex("<[^>]+>"), " ")
        val texto = BusquedaWeb.decodificarEntidades(cuerpo)
            .lines().map { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }
            .joinToString("\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        return Pagina(titulo, texto)
    }
}
