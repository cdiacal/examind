package com.cdiacal.examind.datos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cdiacal.examind.nucleo.LectorDocx
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

/** Tipos de archivo que se aceptan en el selector (el .doc antiguo se acepta para poder explicar qué hacer con él). */
val TIPOS_TEMARIO = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/msword"
)

/**
 * Un temario ya copiado dentro de la app y con su texto leído página a página. [conPaginas] es false si el documento
 * no tiene páginas (un Word sin marcas de página): entonces todo va en una y las preguntas no llevan página.
 */
data class DocumentoLeido(
    val archivo: String,
    val nombreOriginal: String?,
    val huella: String,
    val paginas: List<String>,
    val conPaginas: Boolean,
    val tipo: String
)

/** El documento no se pudo leer; [mensaje] dice qué pasó y qué hacer. */
class DocumentoNoLegible(mensaje: String) : Exception(mensaje)

/**
 * Copia el temario elegido (PDF o Word .docx) a la carpeta privada de la app, así sigue disponible aunque se borre el
 * original, y saca el texto de cada página: los PDF con PdfBox, los Word con nucleo.LectorDocx. Reconoce el tipo por
 * los primeros bytes del archivo, no por el nombre.
 */
class LectorTemario(private val context: Context) {

    private val carpeta: File get() = File(context.filesDir, "temarios").apply { mkdirs() }

    suspend fun leer(uri: Uri, alAvanzar: (pagina: Int, total: Int) -> Unit): DocumentoLeido = withContext(Dispatchers.IO) {
        val nombreOriginal = nombreDe(uri)
        val temporal = File(carpeta, UUID.randomUUID().toString() + ".tmp")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            val entrada = context.contentResolver.openInputStream(uri)
                ?: throw DocumentoNoLegible("No se pudo abrir el archivo. Prueba a elegirlo otra vez.")
            entrada.use { input ->
                temporal.outputStream().use { output ->
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
            val cabecera = temporal.inputStream().use { val b = ByteArray(8); val n = it.read(b); b.copyOf(maxOf(n, 0)) }

            when {
                empiezaPor(cabecera, "%PDF") -> {
                    val destino = File(carpeta, temporal.nameWithoutExtension + ".pdf").also { temporal.renameTo(it) }
                    try {
                        DocumentoLeido(destino.name, nombreOriginal, huella, leerPdf(destino, alAvanzar), conPaginas = true, tipo = "PDF")
                    } catch (e: Throwable) { destino.delete(); throw e }
                }
                empiezaPor(cabecera, "PK") -> {
                    val destino = File(carpeta, temporal.nameWithoutExtension + ".docx").also { temporal.renameTo(it) }
                    try {
                        alAvanzar(0, 0)
                        val doc = try {
                            destino.inputStream().buffered().use { LectorDocx.leer(it) }
                        } catch (e: LectorDocx.DocxNoValido) {
                            throw DocumentoNoLegible(e.message ?: "No se pudo leer el documento de Word.")
                        }
                        DocumentoLeido(destino.name, nombreOriginal, huella, doc.paginas, doc.conPaginas, tipo = "Word")
                    } catch (e: Throwable) { destino.delete(); throw e }
                }
                cabecera.size >= 4 && cabecera[0] == 0xD0.toByte() && cabecera[1] == 0xCF.toByte() && cabecera[2] == 0x11.toByte() && cabecera[3] == 0xE0.toByte() ->
                    throw DocumentoNoLegible("Es un documento de Word antiguo (.doc), que no se puede leer. Ábrelo en Word o Google Docs, guárdalo como .docx o PDF y vuelve a añadirlo.")
                else -> throw DocumentoNoLegible("El archivo no es un PDF ni un documento de Word (.docx). Elige uno de esos dos tipos.")
            }
        } finally {
            temporal.delete()
        }
    }

    private suspend fun leerPdf(archivo: File, alAvanzar: (Int, Int) -> Unit): List<String> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context.applicationContext)
        try {
            PDDocument.load(archivo).use { documento ->
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
            throw DocumentoNoLegible("El PDF tiene contraseña. Quítasela (o expórtalo sin protección) y vuelve a añadirlo.")
        } catch (e: java.io.IOException) {
            throw DocumentoNoLegible("El archivo no es un PDF válido o está dañado. Prueba con otra copia.")
        }
    }

    private fun empiezaPor(bytes: ByteArray, texto: String): Boolean {
        val esperado = texto.toByteArray(Charsets.US_ASCII)
        return bytes.size >= esperado.size && esperado.indices.all { bytes[it] == esperado[it] }
    }

    /** Borra la copia de un temario que al final no se guardó (o cuyo temario se ha eliminado). */
    fun borrar(archivo: String) {
        if (archivo.isNotEmpty()) File(carpeta, archivo).delete()
    }

    private fun nombreDe(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
