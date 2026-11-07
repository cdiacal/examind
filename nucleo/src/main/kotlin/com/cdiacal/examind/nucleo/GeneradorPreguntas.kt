package com.cdiacal.examind.nucleo

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Un modelo de lenguaje que, dado un texto de entrada, devuelve texto. Lo implementa cada motor (Gemini Nano, LiteRT-LM). */
fun interface MotorTexto {
    suspend fun generar(prompt: String): String
}

/** Las instrucciones que se le dan al modelo. Cortas y muy concretas: los modelos del móvil son pequeños. */
object PromptPreguntas {

    fun desdeTemario(texto: String, pagina: Int, cuantas: Int): String = """
Eres profesor de universidad y preparas un examen tipo test en español.
Lee el TEXTO y escribe $cuantas preguntas sobre sus ideas más importantes.

Reglas:
- Cada pregunta tiene exactamente 4 opciones y solo una es correcta.
- Todo debe poder responderse con el TEXTO. No inventes datos.
- Las opciones incorrectas deben ser creíbles, del mismo tema y de longitud parecida a la correcta.
- No uses «todas las anteriores» ni «ninguna de las anteriores».
- "correcta" es la posición de la opción correcta, empezando en 0.
- "explicacion" es una frase que justifica la respuesta con el TEXTO.

Responde solo con una lista JSON, sin nada antes ni después, con este formato:
[{"pregunta": "...", "opciones": ["...", "...", "...", "..."], "correcta": 0, "explicacion": "..."}]

${if (pagina > 0) "TEXTO (página $pagina):" else "TEXTO:"}
""${'"'}
${texto.trim()}
""${'"'}
""".trimStart()
}

/** Instrucciones para sacar preguntas de una página web o un PDF encontrado en internet. */
fun PromptPreguntas.desdeWeb(texto: String, tituloPagina: String, tema: String, cuantas: Int): String = """
Preparas un examen tipo test sobre «${tema.trim()}». El TEXTO viene de la página «${tituloPagina.trim()}».

- Si el TEXTO tiene preguntas tipo test (un examen o un banco de preguntas), cópialas tal cual, hasta $cuantas.
- Si no tiene preguntas tipo test, escribe $cuantas preguntas nuevas en español sobre lo que explica el TEXTO.
- Si el TEXTO no trata del tema, responde solo [].

Reglas:
- Cada pregunta tiene exactamente 4 opciones y solo una es correcta.
- "correcta" es la posición de la opción correcta, empezando en 0.
- "copiada" es true si la pregunta está en el TEXTO y false si la has escrito tú.
- "solucion_en_texto" es true si el TEXTO dice cuál es la respuesta correcta y false si la has deducido tú.
- "explicacion" es una frase que justifica la respuesta.

Responde solo con una lista JSON, sin nada antes ni después, con este formato:
[{"pregunta": "...", "opciones": ["...", "...", "...", "..."], "correcta": 0, "copiada": true, "solucion_en_texto": true, "explicacion": "..."}]

TEXTO:
""${'"'}
${texto.trim()}
""${'"'}
""".trimStart()

/** Cómo va la generación: fragmentos hechos de [total] y preguntas nuevas encontradas hasta ahora. */
data class Avance(val hechos: Int, val total: Int, val nuevas: Int)

data class ResultadoGeneracion(
    val preguntas: List<PreguntaNueva>,
    /** Preguntas que la IA devolvió mal formadas o que ya existían. */
    val descartadas: Int,
    val duplicadas: Int,
    /** Fragmentos en los que el motor falló (error o respuesta sin preguntas). */
    val fragmentosFallidos: Int,
    /** El último error del motor, si lo hubo, para explicarlo. */
    val ultimoError: Throwable?
)

/**
 * Recorre los fragmentos, le pide preguntas al [motor] para cada uno y junta las válidas sin duplicados (ni con las
 * ya guardadas, [huellasGuardadas], ni entre sí). Si un fragmento falla, sigue con el siguiente.
 */
class GeneradorPreguntas(private val motor: MotorTexto, private val porFragmento: Int = 3) {

    /** Preguntas del temario: llevan la página del fragmento (si la tiene). */
    suspend fun generar(
        fragmentos: List<Fragmento>,
        huellasGuardadas: Set<String>,
        alAvanzar: (Avance) -> Unit = {}
    ): ResultadoGeneracion = recorrer(fragmentos, huellasGuardadas, alAvanzar) { fragmento, respuesta ->
        // La página es siempre la del fragmento (0 = sin página): el modelo no la sabe y podría inventarla
        LectorRespuestaIA.leer(respuesta, origen = Origen.TEMARIO).let { r ->
            r.copy(preguntas = r.preguntas.map { it.copy(pagina = fragmento.pagina.takeIf { p -> p > 0 }) })
        }
    }

    /** Preguntas de una página encontrada en internet: llevan su [fuente] y ninguna página del temario. */
    suspend fun generarDesdeWeb(
        fragmentos: List<Fragmento>,
        huellasGuardadas: Set<String>,
        fuente: Fuente,
        tema: String,
        alAvanzar: (Avance) -> Unit = {}
    ): ResultadoGeneracion = recorrer(
        fragmentos, huellasGuardadas, alAvanzar,
        prompt = { f -> PromptPreguntas.desdeWeb(f.texto, fuente.titulo, tema, porFragmento + 2) }
    ) { _, respuesta ->
        LectorRespuestaIA.leer(respuesta, origen = Origen.ONLINE, fuente = fuente)
    }

    private suspend fun recorrer(
        fragmentos: List<Fragmento>,
        huellasGuardadas: Set<String>,
        alAvanzar: (Avance) -> Unit,
        prompt: (Fragmento) -> String = { f -> PromptPreguntas.desdeTemario(f.texto, f.pagina, porFragmento) },
        leer: (Fragmento, String) -> LectorRespuestaIA.Resultado
    ): ResultadoGeneracion {
        val huellas = huellasGuardadas.toMutableSet()
        val nuevas = mutableListOf<PreguntaNueva>()
        var descartadas = 0
        var duplicadas = 0
        var fallidos = 0
        var ultimoError: Throwable? = null

        fragmentos.forEachIndexed { i, fragmento ->
            currentCoroutineContext().ensureActive()
            alAvanzar(Avance(i, fragmentos.size, nuevas.size))
            val respuesta = try {
                motor.generar(prompt(fragmento))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ultimoError = e
                null
            }
            if (respuesta == null) {
                fallidos++
            } else {
                val leido = leer(fragmento, respuesta)
                descartadas += leido.descartadas
                if (leido.vacio) fallidos++
                val separacion = separarDuplicadas(leido.preguntas, huellas)
                duplicadas += separacion.duplicadas.size
                separacion.nuevas.forEach { huellas += it.huella }
                nuevas += separacion.nuevas
            }
        }
        alAvanzar(Avance(fragmentos.size, fragmentos.size, nuevas.size))
        return ResultadoGeneracion(nuevas, descartadas, duplicadas, fallidos, ultimoError)
    }
}
