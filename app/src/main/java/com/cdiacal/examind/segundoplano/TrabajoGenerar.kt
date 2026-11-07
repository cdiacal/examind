package com.cdiacal.examind.segundoplano

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.room.withTransaction
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
import com.cdiacal.examind.nucleo.Formato
import com.cdiacal.examind.nucleo.Fragmento
import com.cdiacal.examind.nucleo.GeneradorPreguntas

/**
 * Crea preguntas con la IA en segundo plano, como servicio en primer plano (con notificación) para que Android no
 * lo pare aunque se salga de la app o se apague la pantalla. Cada fragmento terminado se guarda enseguida como
 * borrador: si se cancela o el sistema lo para, no se pierde lo hecho.
 */
class TrabajoGenerar(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val temarioId = inputData.getLong(TEMARIO, -1)
        val cuantos = inputData.getInt(CUANTOS, 5)
        val bd = BaseDatos.obtener(app)
        val temario = bd.temarios().porId(temarioId) ?: return Result.success()
        val cancelar = WorkManager.getInstance(app).createCancelPendingIntent(id)
        Avisos.crearCanales(app)

        fun notificacion(texto: String, hechos: Int, total: Int) =
            Avisos.progreso(app, temarioId, "Creando preguntas: ${temario.nombre}", texto, hechos, total, cancelar)

        // Pasa a servicio en primer plano. Si Android no lo deja (la app ya no está delante), sigue igualmente.
        try {
            setForeground(foreground(notificacion("Cargando la IA del móvil…", 0, 0)))
        } catch (e: IllegalStateException) {
        }

        val fragmentos = bd.temarios().fragmentosSinUsar(temarioId, cuantos)
        if (fragmentos.isEmpty()) {
            return Result.success(workDataOf(NUEVAS to 0))
        }
        val motor = try {
            GestorIA(app).abrir()
        } catch (e: MotorNoDisponible) {
            Avisos.terminado(app, temarioId, "No se han podido crear preguntas", e.message.orEmpty())
            return Result.failure(workDataOf(ERROR to e.message))
        }

        var nuevas = 0
        var fallidos = 0
        var ultimoError: Throwable? = null
        try {
            val huellas = (bd.preguntas().huellas(temarioId) + bd.borradores().huellas(temarioId)).toMutableSet()
            fragmentos.forEachIndexed { i, f ->
                val texto = "Fragmento ${i + 1} de ${fragmentos.size} con ${motor.nombre} · ${Formato.cantidad(nuevas, "pregunta nueva", "preguntas nuevas")}"
                setProgress(workDataOf(HECHOS to i, TOTAL to fragmentos.size, NUEVAS to nuevas, MOTOR to motor.nombre, TEXTO to texto))
                Avisos.actualizarProgreso(app, notificacion(texto, i, fragmentos.size))

                val r = GeneradorPreguntas(motor).generar(listOf(Fragmento(f.pagina, f.texto)), huellas)
                if (r.preguntas.isNotEmpty()) {
                    val ahora = System.currentTimeMillis()
                    bd.withTransaction {
                        bd.borradores().insertar(r.preguntas.mapIndexed { j, p -> p.aBorrador(temarioId, f.id, ahora + j) })
                        bd.temarios().marcarUsado(f.id)
                    }
                    huellas += r.preguntas.map { it.huella }
                    nuevas += r.preguntas.size
                } else {
                    // Sin preguntas válidas, el fragmento queda sin usar para intentarlo otra vez (quizá con otro modelo)
                    fallidos++
                    ultimoError = r.ultimoError ?: ultimoError
                }
            }
        } finally {
            motor.close()
        }

        if (nuevas > 0) {
            Avisos.terminado(
                app, temarioId,
                "${Formato.cantidad(nuevas, "pregunta lista", "preguntas listas")} para revisar",
                "«${temario.nombre}»: toca para revisarlas y guardar las que quieras." +
                    if (fallidos > 0) " En ${Formato.cantidad(fallidos, "fragmento")} la IA no dio preguntas válidas." else ""
            )
        } else {
            Avisos.terminado(
                app, temarioId, "La IA no ha dado preguntas válidas",
                "«${temario.nombre}»: " + (ultimoError?.let { "${it.message ?: it.javaClass.simpleName}. " } ?: "") +
                    "Vuelve a intentarlo o prueba con otro modelo en Ajustes."
            )
        }
        return Result.success(workDataOf(NUEVAS to nuevas, FALLIDOS to fallidos))
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        foreground(Avisos.progreso(applicationContext, inputData.getLong(TEMARIO, -1), "Creando preguntas", "Preparando…", 0, 0,
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)))

    private fun foreground(n: android.app.Notification): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= 34) ForegroundInfo(Avisos.ID_PROGRESO, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else ForegroundInfo(Avisos.ID_PROGRESO, n)

    companion object {
        /** Un único trabajo de generación a la vez (la IA ocupa mucha memoria); los siguientes esperan en cola. */
        const val NOMBRE = "generar-preguntas"
        const val TEMARIO = "temarioId"
        const val CUANTOS = "cuantos"
        const val HECHOS = "hechos"
        const val TOTAL = "total"
        const val NUEVAS = "nuevas"
        const val FALLIDOS = "fallidos"
        const val MOTOR = "motor"
        /** Texto ya preparado para enseñar el progreso. */
        const val TEXTO = "texto"
        const val ERROR = "error"

        fun etiqueta(temarioId: Long) = "temario-$temarioId"

        fun lanzar(context: Context, temarioId: Long, cuantos: Int) {
            val peticion = OneTimeWorkRequestBuilder<TrabajoGenerar>()
                .setInputData(Data.Builder().putLong(TEMARIO, temarioId).putInt(CUANTOS, cuantos).build())
                .addTag(etiqueta(temarioId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
        }

        fun cancelar(context: Context, temarioId: Long) {
            WorkManager.getInstance(context).cancelAllWorkByTag(etiqueta(temarioId))
        }
    }
}
