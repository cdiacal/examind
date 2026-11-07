package com.cdiacal.examind.segundoplano

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cdiacal.examind.datos.BaseDatos
import com.cdiacal.examind.datos.aBorrador
import com.cdiacal.examind.ia.GestorIA
import com.cdiacal.examind.ia.MotorNoDisponible
import com.cdiacal.examind.nucleo.BusquedaWeb
import com.cdiacal.examind.nucleo.Formato
import com.cdiacal.examind.nucleo.Fuente
import com.cdiacal.examind.nucleo.GeneradorPreguntas
import com.cdiacal.examind.nucleo.Troceador
import com.cdiacal.examind.online.ClienteWeb
import com.cdiacal.examind.online.FalloWeb

/**
 * Saca preguntas de páginas y PDF encontrados en internet, en segundo plano: descarga cada dirección, lee su texto
 * y la IA copia las preguntas del examen (o escribe preguntas sobre la página), guardando la fuente. Cada página
 * terminada se guarda como borrador.
 */
class TrabajoOnline(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        Avisos.crearCanales(app)
        val temarioId = inputData.getLong(TrabajoGenerar.TEMARIO, -1)
        val tema = inputData.getString(TEMA).orEmpty()
        val urls = inputData.getStringArray(URLS).orEmpty().toList()
        val titulos = inputData.getStringArray(TITULOS).orEmpty().toList()
        val bd = BaseDatos.obtener(app)
        val temario = bd.temarios().porId(temarioId) ?: return Result.success()
        if (urls.isEmpty()) return Result.success()
        val cancelar = WorkManager.getInstance(app).createCancelPendingIntent(id)

        fun notificacion(texto: String, hechos: Int, total: Int) =
            Avisos.progreso(app, temarioId, "Buscando preguntas online: ${temario.nombre}", texto, hechos, total, cancelar)

        try {
            setForeground(foreground(notificacion("Cargando la IA del móvil…", 0, 0)))
        } catch (e: IllegalStateException) {
        }

        val motor = try {
            GestorIA(app).abrir()
        } catch (e: MotorNoDisponible) {
            Avisos.terminado(app, temarioId, "No se han podido sacar preguntas", e.message.orEmpty())
            return Result.failure(workDataOf(TrabajoGenerar.ERROR to e.message))
        }

        var nuevas = 0
        val fallos = mutableListOf<String>()
        try {
            val huellas = (bd.preguntas().huellas(temarioId) + bd.borradores().huellas(temarioId)).toMutableSet()
            urls.forEachIndexed { i, url ->
                val sitio = BusquedaWeb.dominio(url)
                val texto = "Página ${i + 1} de ${urls.size}: $sitio · ${Formato.cantidad(nuevas, "pregunta nueva", "preguntas nuevas")}"
                setProgress(workDataOf(TrabajoGenerar.HECHOS to i, TrabajoGenerar.TOTAL to urls.size, TrabajoGenerar.NUEVAS to nuevas,
                    TrabajoGenerar.MOTOR to motor.nombre, TrabajoGenerar.TEXTO to texto))
                Avisos.actualizarProgreso(app, notificacion(texto, i, urls.size))

                val leida = try {
                    ClienteWeb(app).leer(url, titulos.getOrNull(i).orEmpty().ifBlank { sitio })
                } catch (e: FalloWeb) {
                    fallos += e.message.orEmpty()
                    return@forEachIndexed
                }
                val limpias = Troceador.limpiarPaginas(leida.paginas)
                if (!Troceador.tieneTexto(limpias)) {
                    fallos += "$sitio no tiene texto que se pueda leer."
                    return@forEachIndexed
                }
                // Un examen largo da para muchas preguntas: se limita para que cada página no tarde demasiado
                val fragmentos = Troceador.trocear(limpias).take(MAX_FRAGMENTOS)
                val fuente = Fuente(leida.titulo.ifBlank { sitio }.take(200), leida.url, System.currentTimeMillis())
                val r = GeneradorPreguntas(motor).generarDesdeWeb(fragmentos, huellas, fuente, tema)
                if (r.preguntas.isEmpty()) {
                    fallos += "En $sitio la IA no encontró preguntas sobre el tema."
                } else {
                    val ahora = System.currentTimeMillis()
                    bd.borradores().insertar(r.preguntas.mapIndexed { j, p -> p.aBorrador(temarioId, 0, ahora + j) })
                    huellas += r.preguntas.map { it.huella }
                    nuevas += r.preguntas.size
                }
            }
        } finally {
            motor.close()
        }

        val detalle = if (fallos.isEmpty()) "" else " " + fallos.joinToString(" ")
        if (nuevas > 0) {
            Avisos.terminado(
                app, temarioId,
                "${Formato.cantidad(nuevas, "pregunta online lista", "preguntas online listas")} para revisar",
                "«${temario.nombre}»: toca para revisarlas, con su fuente.$detalle"
            )
        } else {
            Avisos.terminado(app, temarioId, "No se han encontrado preguntas online", "«${temario.nombre}».$detalle Prueba con otras páginas o pega un enlace.")
        }
        return Result.success(workDataOf(TrabajoGenerar.NUEVAS to nuevas))
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foreground(Avisos.progreso(applicationContext, inputData.getLong(TrabajoGenerar.TEMARIO, -1), "Buscando preguntas online", "Preparando…", 0, 0,
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)))

    private fun foreground(n: android.app.Notification): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= 34) ForegroundInfo(Avisos.ID_PROGRESO, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else ForegroundInfo(Avisos.ID_PROGRESO, n)

    companion object {
        const val TEMA = "tema"
        const val URLS = "urls"
        const val TITULOS = "titulos"
        const val MAX_FRAGMENTOS = 6

        fun lanzar(context: Context, temarioId: Long, tema: String, paginas: List<Pair<String, String>>) {
            val peticion = OneTimeWorkRequestBuilder<TrabajoOnline>()
                .setInputData(
                    Data.Builder()
                        .putLong(TrabajoGenerar.TEMARIO, temarioId)
                        .putString(TEMA, tema)
                        .putStringArray(URLS, paginas.map { it.first }.toTypedArray())
                        .putStringArray(TITULOS, paginas.map { it.second }.toTypedArray())
                        .build()
                )
                .addTag(TrabajoGenerar.etiqueta(temarioId))
                .build()
            // La misma cola que crear preguntas del temario: la IA hace una cosa cada vez
            WorkManager.getInstance(context).enqueueUniqueWork(TrabajoGenerar.NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
        }
    }
}
