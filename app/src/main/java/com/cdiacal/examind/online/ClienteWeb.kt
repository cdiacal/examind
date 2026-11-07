package com.cdiacal.examind.online

import android.content.Context
import com.cdiacal.examind.nucleo.BusquedaWeb
import com.cdiacal.examind.nucleo.ResultadoWeb
import com.cdiacal.examind.nucleo.TextoWeb
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

/** No se pudo buscar o descargar; [mensaje] dice qué pasó y qué hacer. */
class FalloWeb(mensaje: String) : Exception(mensaje)

/** Lo que se ha leído de una dirección: su título y el texto por páginas (una sola si es una web). */
data class PaginaLeida(val url: String, val titulo: String, val paginas: List<String>, val esPdf: Boolean)

/**
 * Buscar exámenes y descargar páginas, sin claves ni cuentas: usa la versión HTML de DuckDuckGo y, si falla, la de
 * Mojeek. Cada descarga tiene un tope de tamaño y de tiempo.
 */
class ClienteWeb(private val context: Context) {

    private val agente = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    /** Hace las búsquedas del tema y junta los resultados. Lanza [FalloWeb] si no ha funcionado ningún buscador. */
    suspend fun buscar(tema: String): List<ResultadoWeb> = withContext(Dispatchers.IO) {
        val listas = mutableListOf<List<ResultadoWeb>>()
        var ultimoFallo: Exception? = null
        for (consulta in BusquedaWeb.consultas(tema)) {
            ensureActive()
            val resultados = try {
                buscarEnDuckDuckGo(consulta).ifEmpty { buscarEnMojeek(consulta) }
            } catch (e: IOException) {
                ultimoFallo = e
                try { buscarEnMojeek(consulta) } catch (e2: IOException) { ultimoFallo = e2; emptyList() }
            }
            listas += resultados
        }
        val unidos = BusquedaWeb.unir(listas)
        if (unidos.isEmpty() && ultimoFallo != null) {
            throw FalloWeb("No se pudo buscar ahora (${ultimoFallo.message ?: "sin conexión"}). Comprueba la conexión o pega un enlace a mano.")
        }
        unidos
    }

    private fun buscarEnDuckDuckGo(consulta: String): List<ResultadoWeb> {
        val cuerpo = "q=" + URLEncoder.encode(consulta, "UTF-8") + "&b=&kl=es-es"
        val html = pedir("https://html.duckduckgo.com/html/", maxBytes = 2_000_000, post = cuerpo).texto()
        return BusquedaWeb.deDuckDuckGo(html)
    }

    private fun buscarEnMojeek(consulta: String): List<ResultadoWeb> {
        val html = pedir("https://www.mojeek.com/search?q=" + URLEncoder.encode(consulta, "UTF-8"), maxBytes = 2_000_000).texto()
        return BusquedaWeb.deMojeek(html)
    }

    /** Descarga una página o un PDF y saca su texto. */
    suspend fun leer(url: String, tituloSiNoHay: String): PaginaLeida = withContext(Dispatchers.IO) {
        // Android no deja conexiones sin cifrar: se prueba la misma dirección con https
        val segura = url.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        val r = try {
            pedir(segura, maxBytes = 25_000_000)
        } catch (e: IOException) {
            throw FalloWeb("No se pudo descargar ${BusquedaWeb.dominio(url)} (${e.message ?: "sin conexión"}).")
        }
        val esPdf = r.tipo.contains("pdf") || (r.bytes.size > 4 && String(r.bytes, 0, 4, Charsets.US_ASCII) == "%PDF")
        if (esPdf) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val paginas = try {
                PDDocument.load(r.bytes).use { doc ->
                    val lector = PDFTextStripper().apply { sortByPosition = true }
                    (1..doc.numberOfPages).map { n -> lector.startPage = n; lector.endPage = n; lector.getText(doc) }
                }
            } catch (e: IOException) {
                throw FalloWeb("El PDF de ${BusquedaWeb.dominio(url)} no se pudo leer (puede tener contraseña o estar dañado).")
            }
            PaginaLeida(r.urlFinal, tituloSiNoHay, paginas, esPdf = true)
        } else if (r.tipo.contains("html") || r.tipo.contains("text") || r.tipo.isEmpty()) {
            val pagina = TextoWeb.desdeHtml(r.texto())
            PaginaLeida(r.urlFinal, pagina.titulo.ifBlank { tituloSiNoHay }, listOf(pagina.texto), esPdf = false)
        } else {
            throw FalloWeb("${BusquedaWeb.dominio(url)} no es una página web ni un PDF (${r.tipo}).")
        }
    }

    private class Respuesta(val urlFinal: String, val tipo: String, val charset: Charset, val bytes: ByteArray) {
        fun texto() = String(bytes, charset)
    }

    /** Petición HTTP con tope de tamaño, de tiempo y de redirecciones (también de http a https). */
    private fun pedir(direccion: String, maxBytes: Int, post: String? = null): Respuesta {
        var url = URL(direccion)
        repeat(5) {
            val c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", agente)
                setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.7")
                if (post != null) {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    outputStream.use { it.write(post.toByteArray()) }
                }
            }
            try {
                val codigo = c.responseCode
                if (codigo in 300..399) {
                    val destino = c.getHeaderField("Location") ?: throw IOException("redirección sin destino")
                    url = URL(URL(url, destino).toString().replaceFirst(Regex("^http://"), "https://"))
                    return@repeat
                }
                if (codigo !in 200..299) throw IOException("el servidor respondió $codigo")
                val largo = c.contentLengthLong
                if (largo > maxBytes) throw IOException("el archivo es demasiado grande")
                val salida = ByteArrayOutputStream()
                c.inputStream.use { entrada ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        salida.write(buffer, 0, n)
                        if (salida.size() > maxBytes) throw IOException("el archivo es demasiado grande")
                    }
                }
                val tipo = c.contentType.orEmpty().lowercase()
                val charset = Regex("charset=([\\w-]+)").find(tipo)?.groupValues?.get(1)
                    ?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
                return Respuesta(url.toString(), tipo, charset, salida.toByteArray())
            } finally {
                c.disconnect()
            }
        }
        throw IOException("demasiadas redirecciones")
    }
}
