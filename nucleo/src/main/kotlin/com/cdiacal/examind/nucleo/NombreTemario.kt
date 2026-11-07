package com.cdiacal.examind.nucleo

/** Utilidades para el nombre de un temario nuevo. */
object NombreTemario {

    /**
     * Nombre legible a partir del nombre del archivo: sin «.pdf», guiones bajos como espacios, sin espacios dobles
     * y con la primera letra en mayúscula. «tema_3__la_célula.pdf» → «Tema 3 la célula».
     */
    fun desdeArchivo(archivo: String?): String {
        val base = archivo.orEmpty().substringAfterLast('/').replace(Regex("(?i)\\.pdf$"), "")
        val limpio = base.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
        if (limpio.isEmpty()) return "Temario sin nombre"
        return limpio.replaceFirstChar { it.uppercase() }.take(80)
    }

    /**
     * Si ya hay un temario con ese nombre, añade « (2)», « (3)»… para distinguirlos.
     */
    fun unico(nombre: String, existentes: Collection<String>): String {
        val usados = existentes.map { normalizarTexto(it) }.toSet()
        if (normalizarTexto(nombre) !in usados) return nombre
        var n = 2
        while (normalizarTexto("$nombre ($n)") in usados) n++
        return "$nombre ($n)"
    }
}
