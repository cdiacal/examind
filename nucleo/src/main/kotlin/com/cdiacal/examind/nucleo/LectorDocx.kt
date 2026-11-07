package com.cdiacal.examind.nucleo

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Lee el texto de un documento de Word (.docx). Un .docx es un zip con el texto en `word/document.xml`: se toma
 * cada párrafo (también los de las tablas) y se separan las páginas por los saltos de página que guarda Word
 * (los manuales y los que anota al guardar, `lastRenderedPageBreak`). Código puro, sin Android.
 */
object LectorDocx {

    /** Tope de tamaño del XML descomprimido, para no quedarse sin memoria con archivos raros o maliciosos. */
    private const val MAXIMO_BYTES = 60L * 1024 * 1024

    private const val W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    /** [paginas]: el texto de cada página. [conPaginas]: false si Word no dejó marcas de página (todo va en una). */
    data class Documento(val paginas: List<String>, val conPaginas: Boolean)

    class DocxNoValido(mensaje: String) : Exception(mensaje)

    fun leer(entrada: InputStream): Documento {
        val zip = ZipInputStream(entrada)
        try {
            while (true) {
                val entrada = zip.nextEntry ?: break
                if (entrada.name == "word/document.xml") return leerXml(Limitado(zip, MAXIMO_BYTES))
            }
        } catch (e: ZipException) {
            throw DocxNoValido("El archivo no es un documento de Word válido o está dañado. Prueba a guardarlo otra vez como .docx o PDF.")
        }
        throw DocxNoValido("El archivo no es un documento de Word (.docx). Guárdalo como .docx o PDF y vuelve a añadirlo.")
    }

    private fun leerXml(xml: InputStream): Documento {
        val fabrica = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            // Sin entidades externas (seguridad); en algunos sistemas estas opciones no existen y se ignoran
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val lector = Lector()
        try {
            fabrica.newSAXParser().parse(xml, lector)
        } catch (e: SAXException) {
            throw DocxNoValido("No se pudo leer el texto del documento de Word. Prueba a guardarlo otra vez como .docx o PDF.")
        } catch (e: DemasiadoGrande) {
            throw DocxNoValido("El documento de Word es demasiado grande. Divídelo en partes o guárdalo como PDF.")
        } catch (e: java.io.IOException) {
            throw DocxNoValido("No se pudo leer el documento de Word. Prueba a guardarlo otra vez como .docx o PDF.")
        }
        return lector.documento()
    }

    private class Lector : DefaultHandler() {
        private val paginas = mutableListOf<String>()
        private val pagina = StringBuilder()
        private var enTexto = false
        private var huboSalto = false

        override fun startElement(uri: String?, local: String, qName: String?, atributos: Attributes) {
            if (uri != W) return
            when (local) {
                "t" -> enTexto = true
                "tab" -> pagina.append(' ')
                "br" -> if (atributos.getValue(W, "type") == "page") nuevaPagina() else pagina.append('\n')
                "lastRenderedPageBreak" -> nuevaPagina()
                "cr" -> pagina.append('\n')
                "noBreakHyphen" -> pagina.append('-')
            }
        }

        override fun endElement(uri: String?, local: String, qName: String?) {
            if (uri != W) return
            when (local) {
                "t" -> enTexto = false
                "p" -> pagina.append("\n\n")
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (enTexto) pagina.append(ch, start, length)
        }

        private fun nuevaPagina() {
            // Word a veces anota un salto al principio: una página vacía no cuenta
            if (pagina.isBlank()) return
            huboSalto = true
            paginas += pagina.toString()
            pagina.clear()
        }

        fun documento(): Documento {
            if (pagina.isNotBlank() || paginas.isEmpty()) paginas += pagina.toString()
            val limpias = paginas.map { it.replace(Regex("\n{3,}"), "\n\n").trim() }
            return Documento(limpias, conPaginas = huboSalto)
        }
    }

    private class DemasiadoGrande : java.io.IOException()

    /** Corta la lectura si se pasa de [maximo] bytes. */
    private class Limitado(private val dentro: InputStream, private val maximo: Long) : InputStream() {
        private var leidos = 0L
        private fun contar(n: Int): Int {
            if (n > 0) { leidos += n; if (leidos > maximo) throw DemasiadoGrande() }
            return n
        }
        override fun read(): Int = dentro.read().also { if (it >= 0) contar(1) }
        override fun read(b: ByteArray, off: Int, len: Int): Int = contar(dentro.read(b, off, len))
        override fun close() {}
    }
}
