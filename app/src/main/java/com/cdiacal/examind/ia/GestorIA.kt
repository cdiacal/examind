package com.cdiacal.examind.ia

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.cdiacal.examind.nucleo.CatalogoModelos
import com.cdiacal.examind.nucleo.ModeloDescargable
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/** Si Gemini Nano se puede usar en este móvil. */
sealed interface EstadoNano {
    data object Disponible : EstadoNano
    /** Lo puede descargar el sistema (unos cientos de MB, lo gestiona Android). */
    data object Descargable : EstadoNano
    data class Descargando(val bytes: Long, val total: Long) : EstadoNano
    data class NoDisponible(val motivo: String) : EstadoNano
}

/** Cómo va un modelo descargable. */
sealed interface EstadoModelo {
    data object NoDescargado : EstadoModelo
    data class Descargando(val bytes: Long, val total: Long) : EstadoModelo
    /** En pausa hasta que haya wifi (la descarga se pidió solo con wifi). */
    data class EsperandoWifi(val bytes: Long, val total: Long) : EstadoModelo
    data object Descargado : EstadoModelo
    data class Fallo(val mensaje: String) : EstadoModelo
}

/** Qué motor usar para generar preguntas. */
sealed interface EleccionMotor {
    data object GeminiNano : EleccionMotor
    data class Modelo(val modelo: ModeloDescargable) : EleccionMotor
}

/** El motor no se puede abrir; [mensaje] dice qué pasó y qué hacer. */
class MotorNoDisponible(mensaje: String) : Exception(mensaje)

/**
 * Todo lo de la IA en el móvil: si hay Gemini Nano, la descarga e importación de los modelos .litertlm, qué motor
 * se ha elegido y abrirlo para generar.
 */
class GestorIA(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("ia_prefs", Context.MODE_PRIVATE)
    private val descargas = app.getSystemService(DownloadManager::class.java)

    /** Carpeta de la app en el almacenamiento externo: no necesita permisos y se borra al desinstalar. */
    private val carpeta: File get() = File(app.getExternalFilesDir(null) ?: app.filesDir, "modelos").apply { mkdirs() }

    fun archivo(m: ModeloDescargable): File = File(carpeta, m.archivo)

    /** RAM nominal del móvil en GB (la que anuncia el fabricante). */
    val ramGb: Int by lazy {
        val info = ActivityManager.MemoryInfo()
        app.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        CatalogoModelos.ramNominalGb(info.totalMem)
    }

    // ════════ GEMINI NANO ════════

    suspend fun estadoNano(): EstadoNano = try {
        when (Generation.getClient().checkStatus()) {
            FeatureStatus.AVAILABLE -> EstadoNano.Disponible
            FeatureStatus.DOWNLOADABLE -> EstadoNano.Descargable
            FeatureStatus.DOWNLOADING -> EstadoNano.Descargando(0, 0)
            else -> EstadoNano.NoDisponible("Este móvil no tiene Gemini Nano para apps. Usa uno de los modelos descargables.")
        }
    } catch (e: Throwable) {
        EstadoNano.NoDisponible("Este móvil no tiene Gemini Nano para apps (${e.javaClass.simpleName}). Usa uno de los modelos descargables.")
    }

    /** Pide al sistema que descargue Gemini Nano y va contando. */
    fun descargarNano(): Flow<EstadoNano> {
        var total = 0L
        return Generation.getClient().download().map { s ->
            when (s) {
                is DownloadStatus.DownloadStarted -> { total = s.bytesToDownload; EstadoNano.Descargando(0, total) }
                is DownloadStatus.DownloadProgress -> EstadoNano.Descargando(s.totalBytesDownloaded, total)
                is DownloadStatus.DownloadCompleted -> EstadoNano.Disponible
                is DownloadStatus.DownloadFailed -> EstadoNano.NoDisponible("No se pudo descargar Gemini Nano: ${s.e.message}. Prueba más tarde con wifi.")
                else -> EstadoNano.Descargando(0, total)
            }
        }
    }

    // ════════ MODELOS DESCARGABLES ════════

    private fun claveDescarga(m: ModeloDescargable) = "descarga_${m.id}"

    /** Empieza a descargar el modelo con el gestor de descargas de Android (sigue aunque se cierre la app). */
    fun descargar(m: ModeloDescargable, soloWifi: Boolean = true) {
        cancelar(m)
        val peticion = DownloadManager.Request(m.url.toUri())
            .setTitle("Examind: ${m.nombre}")
            .setDescription("Modelo de IA para crear preguntas")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(app, null, "modelos/${m.archivo}.parcial")
            .setAllowedOverMetered(!soloWifi)
            .setAllowedOverRoaming(false)
        prefs.edit().putLong(claveDescarga(m), descargas.enqueue(peticion)).apply()
    }

    fun cancelar(m: ModeloDescargable) {
        val id = prefs.getLong(claveDescarga(m), -1)
        if (id >= 0) descargas.remove(id)
        prefs.edit().remove(claveDescarga(m)).apply()
        File(carpeta, m.archivo + ".parcial").delete()
    }

    fun borrar(m: ModeloDescargable) {
        cancelar(m)
        archivo(m).delete()
        if (modeloElegidoId() == m.id) prefs.edit().remove("motor").apply()
    }

    /** Mira cómo va: si ha terminado, pasa el archivo a su nombre definitivo. */
    fun estado(m: ModeloDescargable): EstadoModelo {
        val id = prefs.getLong(claveDescarga(m), -1)
        if (id < 0) return if (archivo(m).length() > 0) EstadoModelo.Descargado else EstadoModelo.NoDescargado
        descargas.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) {
                prefs.edit().remove(claveDescarga(m)).apply()
                return EstadoModelo.NoDescargado
            }
            val estado = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytes = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { it > 0 } ?: m.bytes
            val motivo = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return when (estado) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val parcial = File(carpeta, m.archivo + ".parcial")
                    if (parcial.exists()) parcial.renameTo(archivo(m))
                    prefs.edit().remove(claveDescarga(m)).apply()
                    EstadoModelo.Descargado
                }
                DownloadManager.STATUS_FAILED -> {
                    prefs.edit().remove(claveDescarga(m)).apply()
                    descargas.remove(id)
                    EstadoModelo.Fallo(mensajeFallo(motivo, m))
                }
                DownloadManager.STATUS_PAUSED ->
                    if (motivo == DownloadManager.PAUSED_QUEUED_FOR_WIFI) EstadoModelo.EsperandoWifi(bytes, total)
                    else EstadoModelo.Descargando(bytes, total)
                else -> EstadoModelo.Descargando(bytes, total)
            }
        }
        return EstadoModelo.NoDescargado
    }

    private fun mensajeFallo(motivo: Int, m: ModeloDescargable): String = when (motivo) {
        401, 403 -> "La página del modelo pide iniciar sesión o aceptar su licencia. Ábrela en el navegador (${m.pagina}), descárgalo y elígelo con «Ya lo tengo descargado»."
        404 -> "El modelo ya no está en esa dirección. Descárgalo desde ${m.pagina} y elígelo con «Ya lo tengo descargado»."
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "No hay espacio suficiente: hacen falta unos ${String.format(java.util.Locale("es", "ES"), "%.1f", m.bytes / 1e9)} GB libres. Libera espacio y vuelve a intentarlo."
        DownloadManager.ERROR_CANNOT_RESUME, DownloadManager.ERROR_HTTP_DATA_ERROR -> "Se cortó la descarga. Vuelve a intentarlo con una conexión estable."
        else -> "No se pudo descargar el modelo (código $motivo). Vuelve a intentarlo más tarde."
    }

    /**
     * Copia un modelo .litertlm descargado a mano (por ejemplo, desde el navegador). Tiene que ser uno de los del
     * catálogo, reconocido por el nombre del archivo.
     */
    suspend fun importar(uri: Uri): ModeloDescargable = withContext(Dispatchers.IO) {
        val nombre = runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        val modelo = CatalogoModelos.porArchivo(nombre)
            ?: throw MotorNoDisponible("«${nombre ?: "Ese archivo"}» no es uno de los modelos de Examind. Elige ${CatalogoModelos.TODOS.joinToString(" o ") { it.archivo }}.")
        val destino = archivo(modelo)
        val temporal = File(carpeta, modelo.archivo + ".parcial")
        try {
            app.contentResolver.openInputStream(uri)?.use { entrada -> temporal.outputStream().use { entrada.copyTo(it, 1 shl 20) } }
                ?: throw MotorNoDisponible("No se pudo abrir el archivo. Prueba a elegirlo otra vez.")
            if (!temporal.renameTo(destino)) throw MotorNoDisponible("No se pudo guardar el modelo. Comprueba que hay espacio libre.")
        } catch (e: java.io.IOException) {
            temporal.delete()
            throw MotorNoDisponible("No se pudo copiar el modelo: ${e.message}. Comprueba que hay espacio libre.")
        }
        modelo
    }

    // ════════ ELEGIR Y ABRIR ════════

    private fun modeloElegidoId(): String? = prefs.getString("motor", null)

    /** Lo que eligió el usuario; si no eligió, Gemini Nano si está, o el primer modelo descargado. */
    suspend fun eleccion(): EleccionMotor? {
        val guardada = modeloElegidoId()
        if (guardada == "nano" && estadoNano() == EstadoNano.Disponible) return EleccionMotor.GeminiNano
        CatalogoModelos.porId(guardada)?.takeIf { estado(it) == EstadoModelo.Descargado }?.let { return EleccionMotor.Modelo(it) }
        if (estadoNano() == EstadoNano.Disponible) return EleccionMotor.GeminiNano
        return CatalogoModelos.TODOS.firstOrNull { estado(it) == EstadoModelo.Descargado }?.let { EleccionMotor.Modelo(it) }
    }

    fun elegir(eleccion: EleccionMotor) {
        prefs.edit().putString("motor", if (eleccion is EleccionMotor.Modelo) eleccion.modelo.id else "nano").apply()
    }

    /** Abre el motor elegido para generar. Lanza [MotorNoDisponible] con un mensaje claro si no hay ninguno. */
    suspend fun abrir(): MotorAbierto {
        val e = eleccion()
            ?: throw MotorNoDisponible("Aún no hay IA en el móvil. Ve a Ajustes → IA del móvil y descarga un modelo.")
        return try {
            when (e) {
                EleccionMotor.GeminiNano -> MotorGeminiNano.abrir()
                is EleccionMotor.Modelo -> MotorLiteRt.abrir(app, archivo(e.modelo), e.modelo.nombre, e.modelo.maxTokens)
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            throw MotorNoDisponible(
                "No se pudo cargar la IA (${t.message ?: t.javaClass.simpleName}). " +
                    "Cierra otras apps para liberar memoria o prueba con el modelo ligero."
            )
        }
    }
}
