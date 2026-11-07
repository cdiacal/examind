package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Test

class NombreTemarioTest {
    @Test fun desdeArchivo() {
        assertEquals("Tema 3 la célula", NombreTemario.desdeArchivo("tema_3__la_célula.PDF"))
        assertEquals("Apuntes", NombreTemario.desdeArchivo("/storage/emulated/0/Download/apuntes.pdf"))
        assertEquals("Apuntes tema 2", NombreTemario.desdeArchivo("apuntes_tema_2.docx"))
        assertEquals("Temario sin nombre", NombreTemario.desdeArchivo(null))
        assertEquals("Temario sin nombre", NombreTemario.desdeArchivo(".pdf"))
    }

    @Test fun unico() {
        assertEquals("Biología", NombreTemario.unico("Biología", listOf("Química")))
        assertEquals("Biología (2)", NombreTemario.unico("Biología", listOf("biologia")))
        assertEquals("Biología (3)", NombreTemario.unico("Biología", listOf("Biología", "Biología (2)")))
    }
}
