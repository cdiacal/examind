package com.cdiacal.examind.nucleo

import kotlin.math.roundToLong

/** Formatos de número en español: coma decimal y punto de miles. */
object Formato {

    /** Nota sobre 10 con un decimal, sin «,0»: 7,5 · 10 · 0. */
    fun nota(nota: Double): String {
        val decimas = (nota * 10).roundToLong()
        return if (decimas % 10 == 0L) "${decimas / 10}" else "${decimas / 10},${decimas % 10}"
    }

    /** Proporción (0..1) como porcentaje entero: «75 %». */
    fun porcentaje(proporcion: Double): String = "${miles((proporcion * 100).roundToLong())} %"

    /** Entero con punto de miles: 1.234. */
    fun miles(numero: Long): String {
        val signo = if (numero < 0) "-" else ""
        val digitos = kotlin.math.abs(numero).toString()
        return signo + digitos.reversed().chunked(3).joinToString(".").reversed()
    }

    /** «3 aciertos», «1 acierto»: número y palabra con el plural correcto. */
    fun cantidad(numero: Int, singular: String, plural: String = singular + "s"): String =
        "${miles(numero.toLong())} ${if (numero == 1) singular else plural}"

    /** Duración en segundos como «12 min 5 s», «45 s» o «1 h 3 min». */
    fun duracion(segundos: Long): String {
        val h = segundos / 3600
        val m = (segundos % 3600) / 60
        val s = segundos % 60
        return when {
            h > 0 -> "$h h $m min"
            m > 0 -> if (s > 0) "$m min $s s" else "$m min"
            else -> "$s s"
        }
    }
}
