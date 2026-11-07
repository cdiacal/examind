package com.cdiacal.examind.nucleo

/** Un trozo del temario, del tamaño adecuado para pedirle preguntas a la IA. [pagina] es donde empieza (desde 1). */
data class Fragmento(val pagina: Int, val texto: String)

/**
 * Limpia el texto sacado de un PDF y lo parte en fragmentos. Recibe el texto de cada página por orden
 * (la primera es la página 1).
 */
object Troceador {

    /** Por debajo de esta media de letras por página consideramos que el PDF es escaneado (imágenes, sin texto). */
    const val MINIMO_LETRAS_POR_PAGINA = 80

    /** Tamaño de fragmento por defecto: cabe holgado en el contexto de un modelo pequeño en el móvil. */
    const val TAMANO_POR_DEFECTO = 2_500

    /** true si el PDF parece tener texto real (no solo imágenes escaneadas). */
    fun tieneTexto(paginas: List<String>): Boolean {
        if (paginas.isEmpty()) return false
        val letras = paginas.sumOf { pagina -> pagina.count { it.isLetter() } }
        return letras / paginas.size >= MINIMO_LETRAS_POR_PAGINA
    }

    /**
     * Quita lo que no es contenido: palabras partidas con guion a final de línea, números de página sueltos y las
     * líneas que se repiten en muchas páginas (cabeceras y pies). Devuelve una lista del mismo tamaño.
     */
    fun limpiarPaginas(paginas: List<String>): List<String> {
        val lineasPorPagina = paginas.map { pagina ->
            pagina.replace("\r", "")
                .replace(Regex("(\\p{L})-\\n(\\p{Ll})"), "$1$2")
                .lines()
                .map { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }
        }
        val repetidas = lineasRepetidas(lineasPorPagina)
        return lineasPorPagina.map { lineas ->
            lineas.filterNot { linea ->
                linea.isNotEmpty() && (esNumeroDePagina(linea) || normalizarTexto(linea) in repetidas)
            }.joinToString("\n").replace(Regex("\\n{3,}"), "\n\n").trim()
        }
    }

    /** Parte las páginas (ya limpias) en fragmentos de como mucho [maximo] caracteres, sin cortar frases si se puede. */
    fun trocear(paginas: List<String>, maximo: Int = TAMANO_POR_DEFECTO): List<Fragmento> {
        require(maximo >= 200) { "El tamaño mínimo de fragmento es 200 caracteres" }
        val fragmentos = mutableListOf<Fragmento>()
        val actual = StringBuilder()
        var paginaActual = 0

        fun cerrar() {
            val texto = actual.toString().trim()
            if (texto.isNotEmpty()) fragmentos += Fragmento(paginaActual, texto)
            actual.clear()
        }

        paginas.forEachIndexed { indice, pagina ->
            for (parrafo in parrafos(pagina)) {
                for (pieza in partirLargo(parrafo, maximo)) {
                    if (actual.isNotEmpty() && actual.length + 2 + pieza.length > maximo) cerrar()
                    if (actual.isEmpty()) paginaActual = indice + 1 else actual.append("\n\n")
                    actual.append(pieza)
                }
            }
        }
        cerrar()
        return fragmentos
    }

    private fun parrafos(pagina: String): List<String> =
        pagina.split(Regex("\\n\\s*\\n"))
            .map { it.replace(Regex("\\s*\\n\\s*"), " ").trim() }
            .filter { it.isNotEmpty() }

    /** Un párrafo más largo que [maximo] se parte por frases; una frase más larga, por palabras. */
    private fun partirLargo(parrafo: String, maximo: Int): List<String> {
        if (parrafo.length <= maximo) return listOf(parrafo)
        val frases = parrafo.split(Regex("(?<=[.!?;:])\\s+"))
        val piezas = mutableListOf<String>()
        val actual = StringBuilder()
        for (frase in frases.flatMap { partirPorPalabras(it, maximo) }) {
            if (actual.isNotEmpty() && actual.length + 1 + frase.length > maximo) {
                piezas += actual.toString(); actual.clear()
            }
            if (actual.isNotEmpty()) actual.append(' ')
            actual.append(frase)
        }
        if (actual.isNotEmpty()) piezas += actual.toString()
        return piezas
    }

    private fun partirPorPalabras(frase: String, maximo: Int): List<String> {
        if (frase.length <= maximo) return listOf(frase)
        val piezas = mutableListOf<String>()
        val actual = StringBuilder()
        for (palabra in frase.split(' ')) {
            if (actual.isNotEmpty() && actual.length + 1 + palabra.length > maximo) {
                piezas += actual.toString(); actual.clear()
            }
            if (actual.isNotEmpty()) actual.append(' ')
            actual.append(palabra.take(maximo))
        }
        if (actual.isNotEmpty()) piezas += actual.toString()
        return piezas
    }

    private fun esNumeroDePagina(linea: String): Boolean =
        linea.matches(Regex("(?i)(p[aá]g(ina)?\\.?\\s*)?\\d{1,4}(\\s*(/|de)\\s*\\d{1,4})?|[-–]\\s*\\d{1,4}\\s*[-–]"))

    /** Líneas (normalizadas) que aparecen en al menos la mitad de las páginas, si hay 3 o más páginas. */
    private fun lineasRepetidas(lineasPorPagina: List<List<String>>): Set<String> {
        if (lineasPorPagina.size < 3) return emptySet()
        val cuenta = mutableMapOf<String, Int>()
        lineasPorPagina.forEach { lineas ->
            lineas.map(::normalizarTexto).filter { it.isNotEmpty() }.toSet().forEach { cuenta[it] = (cuenta[it] ?: 0) + 1 }
        }
        val umbral = (lineasPorPagina.size + 1) / 2
        return cuenta.filterValues { it >= umbral }.keys
    }
}
