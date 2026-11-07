package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LectorDocxTest {
    private fun docx(cuerpo: String, nombre: String = "word/document.xml"): ByteArrayInputStream {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$cuerpo</w:body></w:document>"""
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write("<Types/>".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry(nombre)); zip.write(xml.toByteArray(Charsets.UTF_8)); zip.closeEntry()
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private fun p(vararg trozos: String) = "<w:p>" + trozos.joinToString("") { "<w:r><w:t xml:space=\"preserve\">$it</w:t></w:r>" } + "</w:p>"

    @Test fun leeParrafosYUneLosTrozosDeUnaLinea() {
        val d = LectorDocx.leer(docx(p("La célula ", "es la unidad", " básica.") + p("Segundo párrafo.")))
        assertEquals(listOf("La célula es la unidad básica.\n\nSegundo párrafo."), d.paginas)
        assertFalse(d.conPaginas)
    }

    @Test fun separaPorSaltosDePaginaManualesYDeWord() {
        val cuerpo = p("Página uno.") +
            "<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>" +
            p("Página dos.") +
            "<w:p><w:r><w:lastRenderedPageBreak/><w:t>Página tres.</w:t></w:r></w:p>"
        val d = LectorDocx.leer(docx(cuerpo))
        assertTrue(d.conPaginas)
        assertEquals(listOf("Página uno.", "Página dos.", "Página tres."), d.paginas)
    }

    @Test fun unSaltoAlPrincipioNoCreaUnaPaginaVacia() {
        val d = LectorDocx.leer(docx("<w:p><w:r><w:lastRenderedPageBreak/><w:t>Hola.</w:t></w:r></w:p>"))
        assertEquals(listOf("Hola."), d.paginas)
        assertFalse(d.conPaginas)
    }

    @Test fun tabuladoresSaltosDeLineaYTablas() {
        val tabla = "<w:tbl><w:tr><w:tc>${p("Celda A")}</w:tc><w:tc>${p("Celda B")}</w:tc></w:tr></w:tbl>"
        val cuerpo = "<w:p><w:r><w:t>Uno</w:t><w:tab/><w:t>dos</w:t><w:br/><w:t>tres</w:t></w:r></w:p>" + tabla
        val texto = LectorDocx.leer(docx(cuerpo)).paginas.single()
        assertEquals("Uno dos\ntres\n\nCelda A\n\nCelda B", texto)
    }

    @Test fun noLeeTextoBorradoNiCamposOcultos() {
        val cuerpo = "<w:p><w:r><w:t>Visible</w:t></w:r><w:del><w:r><w:delText> borrado</w:delText></w:r></w:del>" +
            "<w:r><w:instrText> PAGE </w:instrText></w:r></w:p>"
        assertEquals("Visible", LectorDocx.leer(docx(cuerpo)).paginas.single())
    }

    @Test fun rechazaLoQueNoEsUnDocx() {
        val e1 = assertThrows(LectorDocx.DocxNoValido::class.java) { LectorDocx.leer(ByteArrayInputStream("hola".toByteArray())) }
        assertTrue(e1.message!!.contains(".docx"))
        assertThrows(LectorDocx.DocxNoValido::class.java) { LectorDocx.leer(docx(p("x"), nombre = "otra/cosa.xml")) }
    }

    @Test fun rechazaXmlRoto() {
        val roto = ByteArrayOutputStream().also { b ->
            ZipOutputStream(b).use { z -> z.putNextEntry(ZipEntry("word/document.xml")); z.write("<w:document><sin cerrar".toByteArray()); z.closeEntry() }
        }
        assertThrows(LectorDocx.DocxNoValido::class.java) { LectorDocx.leer(ByteArrayInputStream(roto.toByteArray())) }
    }
}
