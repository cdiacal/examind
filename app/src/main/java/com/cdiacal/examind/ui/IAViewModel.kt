package com.cdiacal.examind.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.examind.ia.EleccionMotor
import com.cdiacal.examind.ia.EstadoModelo
import com.cdiacal.examind.ia.EstadoNano
import com.cdiacal.examind.ia.GestorIA
import com.cdiacal.examind.ia.MotorNoDisponible
import com.cdiacal.examind.nucleo.CatalogoModelos
import com.cdiacal.examind.nucleo.ModeloDescargable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EstadoIA(
    val ramGb: Int,
    val nano: EstadoNano? = null,
    val modelos: Map<String, EstadoModelo> = emptyMap(),
    val eleccion: EleccionMotor? = null,
    val probando: Boolean = false,
    val resultadoPrueba: String? = null,
    val error: String? = null
) {
    val recomendado: ModeloDescargable? get() = CatalogoModelos.recomendado(ramGb)
}

class IAViewModel(app: Application) : AndroidViewModel(app) {
    val gestor = GestorIA(app)

    private val _estado = MutableStateFlow(EstadoIA(ramGb = gestor.ramGb))
    val estado: StateFlow<EstadoIA> = _estado.asStateFlow()
    private var vigilancia: Job? = null

    /** Lee el estado de todo y, mientras haya descargas en marcha, lo vuelve a mirar cada segundo. */
    fun vigilar() {
        if (vigilancia?.isActive == true) return
        vigilancia = viewModelScope.launch {
            _estado.update { it.copy(nano = gestor.estadoNano()) }
            do {
                val modelos = CatalogoModelos.TODOS.associate { it.id to gestor.estado(it) }
                _estado.update { it.copy(modelos = modelos, eleccion = gestor.eleccion()) }
                val enMarcha = modelos.values.any { it is EstadoModelo.Descargando || it is EstadoModelo.EsperandoWifi }
                delay(1_000)
            } while (isActive && enMarcha)
        }
    }

    fun dejarDeVigilar() { vigilancia?.cancel() }

    fun descargarNano() {
        viewModelScope.launch {
            try {
                gestor.descargarNano().collect { e -> _estado.update { it.copy(nano = e) } }
                _estado.update { it.copy(eleccion = gestor.eleccion()) }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _estado.update { it.copy(nano = EstadoNano.NoDisponible("No se pudo descargar Gemini Nano. Prueba más tarde con wifi.")) }
            }
        }
    }

    fun descargar(m: ModeloDescargable, soloWifi: Boolean) {
        gestor.descargar(m, soloWifi)
        vigilancia?.cancel(); vigilar()
    }

    fun cancelar(m: ModeloDescargable) { gestor.cancelar(m); vigilancia?.cancel(); vigilar() }

    fun borrar(m: ModeloDescargable) { gestor.borrar(m); vigilancia?.cancel(); vigilar() }

    fun importar(uri: Uri) {
        viewModelScope.launch {
            _estado.update { it.copy(error = null) }
            try {
                val m = gestor.importar(uri)
                gestor.elegir(EleccionMotor.Modelo(m))
            } catch (e: MotorNoDisponible) {
                _estado.update { it.copy(error = e.message) }
            }
            vigilancia?.cancel(); vigilar()
        }
    }

    fun elegir(e: EleccionMotor) {
        gestor.elegir(e)
        _estado.update { it.copy(eleccion = e, resultadoPrueba = null) }
    }

    /** Carga el motor y le pide una respuesta corta, para comprobar que funciona en este móvil y cuánto tarda. */
    fun probar() {
        viewModelScope.launch {
            _estado.update { it.copy(probando = true, resultadoPrueba = null) }
            val inicio = System.currentTimeMillis()
            val texto = try {
                gestor.abrir().use { motor ->
                    val cargado = System.currentTimeMillis()
                    val respuesta = motor.generar("Responde solo con la palabra «listo», sin nada más.").trim().take(60)
                    val fin = System.currentTimeMillis()
                    "Funciona con ${motor.nombre}: respondió «$respuesta». Cargar: ${segundos(cargado - inicio)}; responder: ${segundos(fin - cargado)}."
                }
            } catch (e: MotorNoDisponible) {
                e.message
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                "La IA falló al responder (${t.message ?: t.javaClass.simpleName}). Prueba con el otro modelo."
            }
            _estado.update { it.copy(probando = false, resultadoPrueba = texto) }
        }
    }

    private fun segundos(ms: Long) = String.format(java.util.Locale("es", "ES"), "%.1f s", ms / 1000.0)
}
