package com.cdiacal.examind.nucleo

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Exportar a CSV (RFC 4180: comas, comillas dobles si hace falta). Se abre bien en Excel, Numbers y Hojas de cálculo. */
object Csv {

    /** Pone el campo entre comillas si lleva coma, comillas o salto de línea, y dobla las comillas de dentro. */
    fun campo(valor: String): String =
        if (valor.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + valor.replace("\"", "\"\"") + "\"" else valor

    fun fila(valores: List<String>): String = valores.joinToString(",") { campo(it) }

    /** Lee un CSV (con comillas, comas y saltos de línea dentro de los campos). Lo usan las pruebas y la importación. */
    fun leer(texto: String): List<List<String>> {
        val filas = mutableListOf<List<String>>()
        var fila = mutableListOf<String>()
        val campo = StringBuilder()
        var entreComillas = false
        var i = 0
        val t = texto.removePrefix("﻿")
        while (i < t.length) {
            val c = t[i]
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < t.length && t[i + 1] == '"') { campo.append('"'); i++ } else entreComillas = false
                } else campo.append(c)
            } else when (c) {
                '"' -> entreComillas = true
                ',' -> { fila += campo.toString(); campo.clear() }
                '\r' -> {}
                '\n' -> { fila += campo.toString(); campo.clear(); filas += fila; fila = mutableListOf() }
                else -> campo.append(c)
            }
            i++
        }
        if (campo.isNotEmpty() || fila.isNotEmpty()) { fila += campo.toString(); filas += fila }
        return filas
    }

    private val formatoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun fecha(millis: Long, zona: ZoneId): String = formatoFecha.format(Instant.ofEpochMilli(millis).atZone(zona))

    /** Una pregunta con el nombre de su temario, lista para exportar. */
    data class FilaPregunta(val temario: String, val pregunta: PreguntaNueva)

    val CABECERA_PREGUNTAS = listOf(
        "Temario", "Origen", "Pregunta", "Opción A", "Opción B", "Opción C", "Opción D", "Correcta",
        "Explicación", "Página", "Fuente", "Enlace", "Consultada"
    )

    /** Todas las preguntas, una por fila. Empieza con BOM para que Excel reconozca las tildes. */
    fun exportarPreguntas(filas: List<FilaPregunta>, zona: ZoneId = ZoneId.systemDefault()): String = buildString {
        append('﻿')
        append(fila(CABECERA_PREGUNTAS)).append("\r\n")
        for ((temario, p) in filas) {
            val opciones = (0 until 4).map { p.opciones.getOrNull(it).orEmpty() }
            val valores = listOf(
                temario,
                if (p.origen == Origen.TEMARIO) "Temario" else "Online",
                p.enunciado
            ) + opciones + listOf(
                ('A' + p.correcta).toString(),
                p.explicacion,
                p.pagina?.toString().orEmpty(),
                p.fuente?.titulo.orEmpty(),
                p.fuente?.url.orEmpty(),
                p.fuente?.let { fecha(it.consultadaEl, zona) }.orEmpty()
            )
            append(fila(valores)).append("\r\n")
        }
    }

    /** Un test hecho, para exportar el historial. */
    data class FilaTest(val fecha: Long, val temario: String, val origen: String, val resultado: ResultadoTest, val duracionSegundos: Long)

    val CABECERA_TESTS = listOf("Fecha", "Temario", "Origen", "Preguntas", "Aciertos", "Fallos", "En blanco", "Nota", "Duración (s)")

    fun exportarTests(filas: List<FilaTest>, zona: ZoneId = ZoneId.systemDefault()): String = buildString {
        append('﻿')
        append(fila(CABECERA_TESTS)).append("\r\n")
        for (t in filas) {
            val r = t.resultado
            append(fila(listOf(
                fecha(t.fecha, zona), t.temario, t.origen, r.total.toString(), r.aciertos.toString(),
                r.fallos.toString(), r.enBlanco.toString(), Formato.nota(r.nota), t.duracionSegundos.toString()
            ))).append("\r\n")
        }
    }
}
