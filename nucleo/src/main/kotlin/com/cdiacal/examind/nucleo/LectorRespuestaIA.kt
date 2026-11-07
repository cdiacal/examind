package com.cdiacal.examind.nucleo

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Lee las preguntas que devuelve el modelo de IA. Se le pide una lista JSON así:
 *
 * ```
 * [{"pregunta": "...", "opciones": ["...", "...", "...", "..."], "correcta": 0, "explicacion": "...", "pagina": 3}]
 * ```
 *
 * Los modelos pequeños no siempre obedecen, así que es tolerante: acepta texto o ```json alrededor, un objeto
 * `{"preguntas": [...]}`, la correcta como número (0 o 1 en adelante) o como letra, opciones con «A) » delante y
 * respuestas cortadas a medias (se aprovechan las preguntas completas). Lo que no es válido se descarta y se cuenta.
 */
object LectorRespuestaIA {

    data class Resultado(val preguntas: List<PreguntaNueva>, val descartadas: Int) {
        val vacio: Boolean get() = preguntas.isEmpty()
    }

    /**
     * [numOpciones]: cuántas opciones debe tener cada pregunta. [paginaPorDefecto]: la del fragmento, si la IA no la da.
     * [base1]: true si en el prompt se pidió la correcta empezando en 1 en vez de en 0.
     */
    fun leer(
        texto: String,
        numOpciones: Int = 4,
        origen: Origen = Origen.TEMARIO,
        paginaPorDefecto: Int? = null,
        fuente: Fuente? = null,
        base1: Boolean = false
    ): Resultado {
        val objetos = extraerObjetos(texto)
        val preguntas = mutableListOf<PreguntaNueva>()
        var descartadas = 0
        for (crudo in objetos) {
            val pregunta = try {
                convertir(JSONObject(crudo), numOpciones, origen, paginaPorDefecto, fuente, base1)
            } catch (e: JSONException) {
                null
            }
            if (pregunta != null) preguntas += pregunta else descartadas++
        }
        return Resultado(preguntas, descartadas)
    }

    private fun convertir(
        obj: JSONObject,
        numOpciones: Int,
        origen: Origen,
        paginaPorDefecto: Int?,
        fuente: Fuente?,
        base1: Boolean
    ): PreguntaNueva? {
        val enunciado = primerTexto(obj, "pregunta", "enunciado", "question")?.trim().orEmpty()
        if (enunciado.length < 5) return null

        val arrayOpciones = primerArray(obj, "opciones", "options", "respuestas") ?: return null
        val opciones = (0 until arrayOpciones.length()).map { limpiarOpcion(arrayOpciones.optString(it)) }
        if (opciones.size != numOpciones || opciones.any { it.isEmpty() }) return null
        if (opciones.map(::normalizarTexto).toSet().size != opciones.size) return null

        val correcta = indiceCorrecta(obj, opciones, base1) ?: return null
        if (correcta !in opciones.indices) return null

        val explicacion = primerTexto(obj, "explicacion", "explicación", "explanation")?.trim().orEmpty()
        val pagina = obj.opt("pagina")?.let { (it as? Number)?.toInt() ?: it.toString().trim().toIntOrNull() }
            ?.takeIf { it > 0 } ?: paginaPorDefecto

        return PreguntaNueva(enunciado, opciones, correcta, explicacion, origen, pagina, fuente)
    }

    private fun indiceCorrecta(obj: JSONObject, opciones: List<String>, base1: Boolean): Int? {
        val valor = obj.opt("correcta") ?: obj.opt("respuesta_correcta") ?: obj.opt("answer") ?: return null
        if (valor is Number) return valor.toInt() - if (base1) 1 else 0
        val texto = valor.toString().trim()
        texto.toIntOrNull()?.let { return it - if (base1) 1 else 0 }
        // Una letra sola, o «B)», «b.», «Opción C»
        Regex("(?i)^(?:opci[oó]n\\s+)?([a-zA-Z])\\s*[).:]?$").find(texto)?.let {
            return it.groupValues[1].lowercase()[0] - 'a'
        }
        // El texto de la opción buena
        val normal = normalizarTexto(limpiarOpcion(texto))
        return opciones.indexOfFirst { normalizarTexto(it) == normal }.takeIf { it >= 0 }
    }

    /** Quita «A) », «b. », «1) » o «- » del principio de una opción. */
    fun limpiarOpcion(opcion: String): String =
        opcion.trim().replace(Regex("^(?:[a-hA-H]|[1-8])\\s*[).:]\\s+|^[-•]\\s+"), "").trim()

    private fun primerTexto(obj: JSONObject, vararg claves: String): String? =
        claves.firstNotNullOfOrNull { clave -> obj.opt(clave)?.takeIf { it !is JSONArray && it !is JSONObject }?.toString() }

    private fun primerArray(obj: JSONObject, vararg claves: String): JSONArray? =
        claves.firstNotNullOfOrNull { obj.optJSONArray(it) }

    /**
     * Busca los objetos `{...}` que son preguntas, aunque la lista esté cortada o envuelta en texto. Si encuentra
     * un objeto contenedor (`{"preguntas": [...]}`), entra en él.
     */
    internal fun extraerObjetos(texto: String): List<String> {
        val objetos = mutableListOf<String>()
        var profundidad = 0
        var inicio = -1
        var enCadena = false
        var escapado = false
        for ((i, c) in texto.withIndex()) {
            if (enCadena) {
                when {
                    escapado -> escapado = false
                    c == '\\' -> escapado = true
                    c == '"' -> enCadena = false
                }
                continue
            }
            when (c) {
                '"' -> if (profundidad > 0) enCadena = true
                '{' -> {
                    if (profundidad == 0) inicio = i
                    profundidad++
                }
                '}' -> if (profundidad > 0) {
                    profundidad--
                    if (profundidad == 0 && inicio >= 0) {
                        objetos += texto.substring(inicio, i + 1)
                        inicio = -1
                    }
                }
            }
        }
        // Un objeto contenedor sin "pregunta" propia: se miran los objetos de dentro
        return objetos.flatMap { crudo ->
            val interior = crudo.substring(1, crudo.length - 1)
            val esContenedor = !Regex("\"(pregunta|enunciado|question)\"\\s*:").containsMatchIn(
                interior.substringBefore('[', interior)
            ) && interior.contains('{')
            if (esContenedor) extraerObjetos(interior) else listOf(crudo)
        }.ifEmpty {
            // Lista cortada antes del primer cierre: se reintenta con lo que haya dentro del contenedor abierto
            if (inicio >= 0 && inicio + 1 < texto.length) extraerObjetos(texto.substring(inicio + 1)) else emptyList()
        }
    }
}
