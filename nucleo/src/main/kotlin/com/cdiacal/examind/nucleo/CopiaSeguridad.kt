package com.cdiacal.examind.nucleo

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Formato de la copia de seguridad: un JSON con versión, independiente de la base de datos para que una copia
 * vieja se pueda seguir leyendo aunque cambien las tablas. No incluye los PDF (pesan mucho), sí su texto troceado.
 */
object CopiaSeguridad {

    const val VERSION = 1
    private const val TIPO = "examind-copia"

    data class Temario(val id: Long, val nombre: String, val tema: String, val paginas: Int, val creadoEl: Long)
    data class Fragmento(val temarioId: Long, val pagina: Int, val orden: Int, val texto: String)
    data class Pregunta(val id: Long, val temarioId: Long, val creadaEl: Long, val datos: PreguntaNueva)
    data class Test(
        val id: Long, val temarioId: Long?, val fecha: Long, val origen: String,
        val aciertos: Int, val fallos: Int, val enBlanco: Int, val nota: Double, val duracionSegundos: Long
    )
    data class Respuesta(val testId: Long, val preguntaId: Long, val orden: Int, val elegida: Int?, val acertada: Boolean?)

    data class Copia(
        val creadaEl: Long,
        val temarios: List<Temario>,
        val fragmentos: List<Fragmento>,
        val preguntas: List<Pregunta>,
        val tests: List<Test>,
        val respuestas: List<Respuesta>
    )

    /** La copia no se pudo leer; [mensaje] dice qué pasó y qué hacer. */
    class CopiaNoValida(mensaje: String) : Exception(mensaje)

    fun aJson(copia: Copia): String = JSONObject().apply {
        put("tipo", TIPO)
        put("version", VERSION)
        put("creadaEl", copia.creadaEl)
        put("temarios", JSONArray(copia.temarios.map { t ->
            JSONObject().put("id", t.id).put("nombre", t.nombre).put("tema", t.tema).put("paginas", t.paginas).put("creadoEl", t.creadoEl)
        }))
        put("fragmentos", JSONArray(copia.fragmentos.map { f ->
            JSONObject().put("temarioId", f.temarioId).put("pagina", f.pagina).put("orden", f.orden).put("texto", f.texto)
        }))
        put("preguntas", JSONArray(copia.preguntas.map { p ->
            val d = p.datos
            JSONObject().put("id", p.id).put("temarioId", p.temarioId).put("creadaEl", p.creadaEl)
                .put("origen", d.origen.name).put("enunciado", d.enunciado).put("opciones", JSONArray(d.opciones))
                .put("correcta", d.correcta).put("explicacion", d.explicacion)
                .putOpt("pagina", d.pagina)
                .putOpt("fuente", d.fuente?.let { JSONObject().put("titulo", it.titulo).put("url", it.url).put("consultadaEl", it.consultadaEl) })
        }))
        put("tests", JSONArray(copia.tests.map { t ->
            JSONObject().put("id", t.id).putOpt("temarioId", t.temarioId).put("fecha", t.fecha).put("origen", t.origen)
                .put("aciertos", t.aciertos).put("fallos", t.fallos).put("enBlanco", t.enBlanco).put("nota", t.nota)
                .put("duracionSegundos", t.duracionSegundos)
        }))
        put("respuestas", JSONArray(copia.respuestas.map { r ->
            JSONObject().put("testId", r.testId).put("preguntaId", r.preguntaId).put("orden", r.orden)
                .putOpt("elegida", r.elegida).putOpt("acertada", r.acertada)
        }))
    }.toString()

    fun desdeJson(texto: String): Copia {
        val raiz = try {
            JSONObject(texto.removePrefix("﻿"))
        } catch (e: JSONException) {
            throw CopiaNoValida("El archivo no es una copia de Examind. Elige el archivo .json que guardaste desde Ajustes.")
        }
        if (raiz.optString("tipo") != TIPO) {
            throw CopiaNoValida("El archivo no es una copia de Examind. Elige el archivo .json que guardaste desde Ajustes.")
        }
        val version = raiz.optInt("version", 0)
        if (version > VERSION) {
            throw CopiaNoValida("La copia es de una versión más nueva de Examind. Actualiza la app y vuelve a intentarlo.")
        }
        try {
            return Copia(
                creadaEl = raiz.optLong("creadaEl"),
                temarios = raiz.lista("temarios").map { o ->
                    Temario(o.getLong("id"), o.getString("nombre"), o.optString("tema", o.getString("nombre")), o.optInt("paginas"), o.optLong("creadoEl"))
                },
                fragmentos = raiz.lista("fragmentos").map { o ->
                    Fragmento(o.getLong("temarioId"), o.getInt("pagina"), o.optInt("orden"), o.getString("texto"))
                },
                preguntas = raiz.lista("preguntas").map { o ->
                    val opciones = o.getJSONArray("opciones").let { a -> (0 until a.length()).map { a.getString(it) } }
                    val fuente = o.optJSONObject("fuente")?.let { Fuente(it.getString("titulo"), it.getString("url"), it.optLong("consultadaEl")) }
                    Pregunta(
                        o.getLong("id"), o.getLong("temarioId"), o.optLong("creadaEl"),
                        PreguntaNueva(
                            enunciado = o.getString("enunciado"),
                            opciones = opciones,
                            correcta = o.getInt("correcta"),
                            explicacion = o.optString("explicacion"),
                            origen = Origen.valueOf(o.optString("origen", Origen.TEMARIO.name)),
                            pagina = if (o.has("pagina")) o.getInt("pagina") else null,
                            fuente = fuente
                        )
                    )
                },
                tests = raiz.lista("tests").map { o ->
                    Test(
                        o.getLong("id"), if (o.has("temarioId")) o.getLong("temarioId") else null, o.getLong("fecha"),
                        o.optString("origen"), o.getInt("aciertos"), o.getInt("fallos"), o.getInt("enBlanco"),
                        o.getDouble("nota"), o.optLong("duracionSegundos")
                    )
                },
                respuestas = raiz.lista("respuestas").map { o ->
                    Respuesta(
                        o.getLong("testId"), o.getLong("preguntaId"), o.optInt("orden"),
                        if (o.has("elegida")) o.getInt("elegida") else null,
                        if (o.has("acertada")) o.getBoolean("acertada") else null
                    )
                }
            )
        } catch (e: Exception) {
            throw CopiaNoValida("La copia está incompleta o dañada y no se ha restaurado nada. Prueba con otra copia.")
        }
    }

    private fun JSONObject.lista(clave: String): List<JSONObject> {
        val array = optJSONArray(clave) ?: return emptyList()
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
}
