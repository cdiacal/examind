package com.cdiacal.examind.nucleo

/** Un modelo de IA que se descarga una vez y se ejecuta en el móvil con LiteRT-LM. */
data class ModeloDescargable(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val archivo: String,
    val url: String,
    val bytes: Long,
    /** Memoria RAM nominal que necesita el móvil (la que anuncia el fabricante). */
    val ramMinimaGb: Int,
    /** Tamaño máximo de contexto (entrada + salida) con el que se abre el modelo. */
    val maxTokens: Int,
    /** Página donde se puede descargar a mano (por ejemplo, si pide aceptar la licencia). */
    val pagina: String
)

/**
 * Los modelos que ofrece la app. Son los que la app de ejemplo de Google (AI Edge Gallery) publica para LiteRT-LM,
 * fijados a una versión concreta para que el archivo no cambie sin avisar.
 */
object CatalogoModelos {

    val COMPLETO = ModeloDescargable(
        id = "gemma-4-e2b",
        nombre = "Gemma 4 E2B (completo)",
        descripcion = "Las mejores preguntas. Necesita un móvil reciente con 8 GB de RAM o más.",
        archivo = "gemma-4-E2B-it.litertlm",
        url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm?download=true",
        bytes = 2_590_000_000L,
        ramMinimaGb = 8,
        maxTokens = 8192,
        pagina = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm"
    )

    val LIGERO = ModeloDescargable(
        id = "qwen2.5-1.5b",
        nombre = "Qwen 2.5 1.5B (ligero)",
        descripcion = "Para móviles con 6 GB de RAM. Preguntas más sencillas y con más errores: revísalas antes de guardarlas.",
        archivo = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/5e6e7a58d03777898b31330a15d2e0a5a9107de3/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
        bytes = 1_600_000_000L,
        ramMinimaGb = 6,
        maxTokens = 4096,
        pagina = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct"
    )

    val TODOS = listOf(COMPLETO, LIGERO)

    fun porId(id: String?): ModeloDescargable? = TODOS.firstOrNull { it.id == id }

    /** Busca a qué modelo corresponde un archivo elegido a mano, por su nombre. */
    fun porArchivo(nombre: String?): ModeloDescargable? = TODOS.firstOrNull { it.archivo.equals(nombre, ignoreCase = true) }

    /**
     * Android informa de algo menos RAM que la anunciada (parte la reserva el sistema): un móvil de 8 GB dice unos
     * 7,3 GB. Se redondea hacia arriba a la talla comercial más cercana.
     */
    fun ramNominalGb(bytesTotales: Long): Int {
        val gb = bytesTotales / 1_000_000_000.0
        val tallas = listOf(2, 3, 4, 6, 8, 12, 16, 24)
        return tallas.firstOrNull { gb <= it * 1.0 } ?: tallas.last()
    }

    /** El modelo recomendado para la RAM nominal del móvil, o null si no llega a ninguno. */
    fun recomendado(ramGb: Int): ModeloDescargable? = TODOS.firstOrNull { ramGb >= it.ramMinimaGb }
}
