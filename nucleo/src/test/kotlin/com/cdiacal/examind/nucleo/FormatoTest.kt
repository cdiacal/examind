package com.cdiacal.examind.nucleo

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoTest {
    @Test fun nota() {
        assertEquals("7,5", Formato.nota(7.5))
        assertEquals("10", Formato.nota(10.0))
        assertEquals("0", Formato.nota(0.0))
        assertEquals("6,7", Formato.nota(6.666))
        assertEquals("5", Formato.nota(4.96))
    }

    @Test fun porcentajeYMiles() {
        assertEquals("75 %", Formato.porcentaje(0.75))
        assertEquals("67 %", Formato.porcentaje(2.0 / 3))
        assertEquals("1.234", Formato.miles(1234))
        assertEquals("1.234.567", Formato.miles(1234567))
        assertEquals("-1.000", Formato.miles(-1000))
        assertEquals("999", Formato.miles(999))
    }

    @Test fun cantidadConPlural() {
        assertEquals("1 acierto", Formato.cantidad(1, "acierto"))
        assertEquals("0 aciertos", Formato.cantidad(0, "acierto"))
        assertEquals("3 preguntas en blanco", Formato.cantidad(3, "pregunta en blanco", "preguntas en blanco"))
    }

    @Test fun duracion() {
        assertEquals("45 s", Formato.duracion(45))
        assertEquals("12 min 5 s", Formato.duracion(725))
        assertEquals("2 min", Formato.duracion(120))
        assertEquals("1 h 3 min", Formato.duracion(3780))
    }
}
