package com.cdiacal.examind.datos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Un PDF ya copiado dentro de la app y con su texto leído página a página. */
data class PdfLeido(
    val archivo: String,
    val nombreOriginal: String?,
    val huella: String,
    val paginas: List<String>
)

/** El PDF no se pudo leer; [mensaje] dice qué pasó y qué hacer. */
class PdfNoLegible(mensaje: String) : Exception(mensaje)

/**
 * Copia el PDF elegido a la carpeta privada de la app (así sigue disponible aunque se borre el original) y saca el
 * texto de cada página con PdfBox. La limpieza y el troceado son código puro, en nucleo.Troceador.
 */
class LectorPdf(private val context: Context) {

    private val carpeta: File get() = File(context.filesDir, "temarios").apply { mkdirs() }

    fun archivo(nombre: String): File = File(carpeta, nombre)

    suspend fun leer(uri: Uri, alAvanzar: (pagina: Int, total: Int) -> Unit): PdfLeido = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context.applicationContext)
        val nombreOriginal = nombreDe(uri)
        val destino = File(carpeta, UUID.randomUUID().toString() + ".pdf")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            val entrada = context.contentResolver.openInputStream(uri)
                ?: throw PdfNoLegible("No se pudo abrir el archivo. Prueba a elegirlo otra vez.")
            entrada.use { input ->
                destino.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        digest.update(buffer, 0, n)
                        output.write(buffer, 0, n)
                    }
                }
            }
            val huella = digest.digest().joinToString("") { "%02x".format(it) }
            val paginas = try {
                PDDocument.load(destino).use { documento ->
                    val total = documento.numberOfPages
                    val lector = PDFTextStripper().apply { sortByPosition = true }
                    (1..total).map { pagina ->
                        ensureActive()
                        alAvanzar(pagina, total)
                        lector.startPage = pagina
                        lector.endPage = pagina
                        lector.getText(documento)
                    }
                }
            } catch (e: InvalidPasswordException) {
                throw PdfNoLegible("El PDF tiene contraseña. Quítasela (o expórtalo sin protección) y vuelve a añadirlo.")
            } catch (e: java.io.IOException) {
                throw PdfNoLegible("El archivo no es un PDF válido o está dañado. Prueba con otra copia.")
            }
            PdfLeido(destino.name, nombreOriginal, huella, paginas)
        } catch (e: Throwable) {
            destino.delete()
            throw e
        }
    }

    /** Borra la copia de un PDF que al final no se guardó (o cuyo temario se ha eliminado). */
    fun borrar(archivo: String) {
        if (archivo.isNotEmpty()) File(carpeta, archivo).delete()
    }

    private fun nombreDe(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
