package com.cdiacal.examind.ia

import android.content.Context
import com.cdiacal.examind.nucleo.MotorTexto
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Un motor ya preparado para generar. Hay que cerrarlo al terminar para liberar la memoria. */
interface MotorAbierto : MotorTexto, AutoCloseable {
    val nombre: String
}

/** Temperatura baja: queremos preguntas fieles al texto, no creativas. */
private const val TEMPERATURA = 0.4f
private const val TOP_K = 40

/** Gemini Nano, el modelo que trae Android en algunos móviles (lo gestiona el sistema, vía ML Kit). */
class MotorGeminiNano private constructor(private val modelo: GenerativeModel) : MotorAbierto {
    override val nombre = "Gemini Nano"

    override suspend fun generar(prompt: String): String {
        val peticion = generateContentRequest(TextPart(prompt)) {
            temperature = TEMPERATURA
            topK = TOP_K
            maxOutputTokens = 1024
        }
        val texto = StringBuilder()
        modelo.generateContentStream(peticion).collect { respuesta ->
            texto.append(respuesta.candidates.firstOrNull()?.text.orEmpty())
        }
        return texto.toString()
    }

    override fun close() {}

    companion object {
        /** Prepara el modelo (ya descargado por el sistema). */
        suspend fun abrir(): MotorGeminiNano {
            val modelo = Generation.getClient()
            modelo.warmup()
            return MotorGeminiNano(modelo)
        }
    }
}

/** Un modelo descargado (archivo .litertlm) que se ejecuta con LiteRT-LM, en la GPU si se puede y si no en la CPU. */
class MotorLiteRt private constructor(private val motor: Engine, override val nombre: String) : MotorAbierto {

    override suspend fun generar(prompt: String): String = withContext(Dispatchers.Default) {
        val config = ConversationConfig(samplerConfig = SamplerConfig(topK = TOP_K, topP = 0.95, temperature = TEMPERATURA.toDouble()))
        // Una conversación nueva por fragmento: así no se acumula contexto entre uno y otro
        motor.createConversation(config).use { conversacion -> conversacion.sendMessage(prompt).toString() }
    }

    override fun close() = motor.close()

    companion object {
        /** Carga el modelo. Puede tardar unos segundos; se hace fuera del hilo principal. */
        suspend fun abrir(context: Context, archivo: File, nombre: String, maxTokens: Int): MotorLiteRt = withContext(Dispatchers.Default) {
            fun intentar(backend: Backend): Engine {
                val motor = Engine(
                    EngineConfig(
                        modelPath = archivo.absolutePath,
                        backend = backend,
                        maxNumTokens = maxTokens,
                        cacheDir = context.cacheDir.absolutePath
                    )
                )
                try {
                    motor.initialize()
                } catch (e: Throwable) {
                    runCatching { motor.close() }
                    throw e
                }
                return motor
            }
            val motor = try {
                intentar(Backend.GPU())
            } catch (e: Throwable) {
                // Algunos móviles no tienen GPU compatible: la CPU es más lenta, pero funciona
                intentar(Backend.CPU())
            }
            MotorLiteRt(motor, nombre)
        }
    }
}
