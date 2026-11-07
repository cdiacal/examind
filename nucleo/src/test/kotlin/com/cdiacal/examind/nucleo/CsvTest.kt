package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class CsvTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    @Test fun escapaComasComillasYSaltos() {
        assertEquals("simple", Csv.campo("simple"))
        assertEquals("\"a, b\"", Csv.campo("a, b"))
        assertEquals("\"dice \"\"hola\"\"\"", Csv.campo("dice \"hola\""))
        assertEquals("\"dos\nlíneas\"", Csv.campo("dos\nlíneas"))
    }

    @Test fun exportarPreguntasYVolverALeer() {
        val fuente = Fuente("Examen, junio 2024", "https://ejemplo.edu/e.pdf", 1_718_000_000_000)
        val filas = listOf(
            Csv.FilaPregunta("Biología", PreguntaNueva("¿Qué es \"ATP\"?", listOf("Energía", "Proteína", "Lípido", "Azúcar"), 0, "Moneda, de energía", Origen.TEMARIO, 3)),
            Csv.FilaPregunta("Biología", PreguntaNueva("Pregunta online", listOf("a", "b", "c", "d"), 3, "", Origen.ONLINE, null, fuente))
        )
        val csv = Csv.exportarPreguntas(filas, madrid)
        assertTrue(csv.startsWith("﻿"))

        val leido = Csv.leer(csv)
        assertEquals(3, leido.size)
        assertEquals(Csv.CABECERA_PREGUNTAS, leido[0])
        assertEquals(listOf("Biología", "Temario", "¿Qué es \"ATP\"?", "Energía", "Proteína", "Lípido", "Azúcar", "A", "Moneda, de energía", "3", "", "", ""), leido[1])
        assertEquals("D", leido[2][7])
        assertEquals("Online", leido[2][1])
        assertEquals("Examen, junio 2024", leido[2][10])
        assertEquals("2024-06-10 08:13", leido[2][12])
    }

    @Test fun exportarTests() {
        val r = Puntuacion.calcular(listOf(true, true, true, false))
        val csv = Csv.exportarTests(listOf(Csv.FilaTest(1_718_000_000_000, "Biología", "Temario", r, 300)), madrid)
        val leido = Csv.leer(csv)
        assertEquals(listOf("2024-06-10 08:13", "Biología", "Temario", "4", "3", "1", "0", "7,5", "300"), leido[1])
    }
}
