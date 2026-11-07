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

    suspend fun generar(
        fragmentos: List<Fragmento>,
        huellasGuardadas: Set<String>,
        alAvanzar: (Avance) -> Unit = {}
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
                motor.generar(PromptPreguntas.desdeTemario(fragmento.texto, fragmento.pagina, porFragmento))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                ultimoError = e
                null
            }
            if (respuesta == null) {
                fallidos++
            } else {
                val leido = LectorRespuestaIA.leer(respuesta, origen = Origen.TEMARIO)
                descartadas += leido.descartadas
                if (leido.vacio) fallidos++
                // La página es siempre la del fragmento (0 = sin página): el modelo no la sabe y podría inventarla
                val conPagina = leido.preguntas.map { it.copy(pagina = fragmento.pagina.takeIf { p -> p > 0 }) }
                val separacion = separarDuplicadas(conPagina, huellas)
                duplicadas += separacion.duplicadas.size
                separacion.nuevas.forEach { huellas += it.huella }
                nuevas += separacion.nuevas
            }
        }
        alAvanzar(Avance(fragmentos.size, fragmentos.size, nuevas.size))
        return ResultadoGeneracion(nuevas, descartadas, duplicadas, fallidos, ultimoError)
    }
}
