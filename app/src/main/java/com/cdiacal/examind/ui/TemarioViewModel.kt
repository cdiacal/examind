package com.cdiacal.examind.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.cdiacal.examind.datos.BaseDatos
import com.cdiacal.examind.datos.LectorPdf
import com.cdiacal.examind.datos.PreguntaEntidad
import com.cdiacal.examind.datos.RespuestaEntidad
import com.cdiacal.examind.datos.TemarioConCuentas
import com.cdiacal.examind.datos.aEntidad
import com.cdiacal.examind.ia.GestorIA
import com.cdiacal.examind.ia.MotorNoDisponible
import com.cdiacal.examind.nucleo.Avance
import com.cdiacal.examind.nucleo.Formato
import com.cdiacal.examind.nucleo.Fragmento
import com.cdiacal.examind.nucleo.GeneradorPreguntas
import com.cdiacal.examind.nucleo.PreguntaNueva
import com.cdiacal.examind.nucleo.ResultadoGeneracion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Crear preguntas con la IA: preparando el motor, generando, revisando lo generado o un error. */
sealed interface EstadoGeneracion {
    data object Inactivo : EstadoGeneracion
    data object CargandoMotor : EstadoGeneracion
    data class Generando(val motor: String, val avance: Avance) : EstadoGeneracion
    data class Revisando(
        val preguntas: List<PreguntaNueva>,
        val seleccion: Set<Int>,
        val fragmentoIds: List<Long>,
        val resumen: String
    ) : EstadoGeneracion
    data class Error(val mensaje: String) : EstadoGeneracion
}

/** Lo que se acaba de guardar, para poder deshacerlo. */
data class PreguntasGuardadas(val ids: List<Long>, val fragmentoIds: List<Long>)

/** Lo que se acaba de borrar, para poder deshacerlo. */
data class PreguntasBorradas(val preguntas: List<PreguntaEntidad>, val respuestas: List<RespuestaEntidad>)

@OptIn(ExperimentalCoroutinesApi::class)
class TemarioViewModel(app: Application) : AndroidViewModel(app) {
    private val bd = BaseDatos.obtener(app)
    private val gestor = GestorIA(app)
    private val lector = LectorPdf(app)

    private val temarioId = MutableStateFlow<Long?>(null)

    val temario: StateFlow<TemarioConCuentas?> = combine(bd.temarios().observarConCuentas(), temarioId) { lista, id ->
        lista.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun observar(origen: String): Flow<List<PreguntaEntidad>> =
        temarioId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else bd.preguntas().observar(id, origen) }

    val delTemario = observar("TEMARIO").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val online = observar("ONLINE").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _generacion = MutableStateFlow<EstadoGeneracion>(EstadoGeneracion.Inactivo)
    val generacion: StateFlow<EstadoGeneracion> = _generacion.asStateFlow()
    private var trabajo: Job? = null

    fun abrir(id: Long) {
        if (temarioId.value != id) {
            trabajo?.cancel()
            _generacion.value = EstadoGeneracion.Inactivo
        }
        temarioId.value = id
    }

    /** Crea preguntas con los siguientes [cuantos] fragmentos sin usar del temario. */
    fun generar(cuantos: Int) {
        val id = temarioId.value ?: return
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            _generacion.value = EstadoGeneracion.CargandoMotor
            val fragmentos = bd.temarios().fragmentosSinUsar(id, cuantos)
            if (fragmentos.isEmpty()) {
                _generacion.value = EstadoGeneracion.Error("Ya se han usado todos los fragmentos del temario. Para tener más preguntas, busca exámenes online.")
                return@launch
            }
            val motor = try {
                gestor.abrir()
            } catch (e: MotorNoDisponible) {
                _generacion.value = EstadoGeneracion.Error(e.message ?: "No hay IA disponible.")
                return@launch
            }
            val resultado: ResultadoGeneracion = try {
                GeneradorPreguntas(motor).generar(
                    fragmentos.map { Fragmento(it.pagina, it.texto) },
                    bd.preguntas().huellas(id).toSet()
                ) { avance -> _generacion.value = EstadoGeneracion.Generando(motor.nombre, avance) }
            } finally {
                motor.close()
            }

            val p = resultado.preguntas
            _generacion.value = if (p.isEmpty()) {
                EstadoGeneracion.Error(
                    "La IA no ha devuelto preguntas válidas" +
                        (resultado.ultimoError?.let { " (${it.message ?: it.javaClass.simpleName})" } ?: "") +
                        ". Vuelve a intentarlo o prueba con otro modelo en Ajustes."
                )
            } else {
                val partes = buildList {
                    add("${Formato.cantidad(p.size, "pregunta nueva", "preguntas nuevas")} de ${Formato.cantidad(fragmentos.size, "fragmento")}")
                    if (resultado.duplicadas > 0) add("${resultado.duplicadas} repetidas no se muestran")
                    if (resultado.descartadas > 0) add("${resultado.descartadas} mal formadas descartadas")
                }
                EstadoGeneracion.Revisando(p, p.indices.toSet(), fragmentos.map { it.id }, partes.joinToString(" · "))
            }
        }
    }

    fun cancelarGeneracion() {
        trabajo?.cancel()
        _generacion.value = EstadoGeneracion.Inactivo
    }

    fun alternar(indice: Int) {
        val r = _generacion.value as? EstadoGeneracion.Revisando ?: return
        _generacion.value = r.copy(seleccion = if (indice in r.seleccion) r.seleccion - indice else r.seleccion + indice)
    }

    fun seleccionarTodas(todas: Boolean) {
        val r = _generacion.value as? EstadoGeneracion.Revisando ?: return
        _generacion.value = r.copy(seleccion = if (todas) r.preguntas.indices.toSet() else emptySet())
    }

    /** Guarda las preguntas elegidas (sin duplicados) y marca los fragmentos como usados. */
    fun guardarRevisadas(alTerminar: (PreguntasGuardadas, Int) -> Unit) {
        val r = _generacion.value as? EstadoGeneracion.Revisando ?: return
        val id = temarioId.value ?: return
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val elegidas = r.preguntas.filterIndexed { i, _ -> i in r.seleccion }
            val ids = bd.withTransaction {
                val insertadas = bd.preguntas().insertar(elegidas.mapIndexed { i, p -> p.aEntidad(id, ahora + i) }).filter { it > 0 }
                bd.temarios().marcarUsados(r.fragmentoIds)
                insertadas
            }
            _generacion.value = EstadoGeneracion.Inactivo
            alTerminar(PreguntasGuardadas(ids, r.fragmentoIds), ids.size)
        }
    }

    /** Descarta lo generado sin guardar (los fragmentos quedan sin usar para otra vez). */
    fun descartar() { _generacion.value = EstadoGeneracion.Inactivo }

    fun deshacerGuardado(g: PreguntasGuardadas) {
        viewModelScope.launch {
            bd.withTransaction {
                bd.preguntas().borrarPorIds(g.ids)
                bd.temarios().desmarcarUsados(g.fragmentoIds)
            }
        }
    }

    /** Borra preguntas guardadas; devuelve lo borrado (con sus respuestas en tests) para poder deshacerlo. */
    fun borrarPreguntas(preguntas: List<PreguntaEntidad>, alTerminar: (PreguntasBorradas) -> Unit) {
        viewModelScope.launch {
            val borradas = bd.withTransaction {
                val respuestas = bd.tests().respuestasDe(preguntas.map { it.id })
                bd.preguntas().borrar(preguntas)
                PreguntasBorradas(preguntas, respuestas)
            }
            alTerminar(borradas)
        }
    }

    /** Vuelve a poner preguntas borradas con sus mismos ids y sus respuestas, como si no se hubieran borrado. */
    fun restaurarPreguntas(b: PreguntasBorradas) {
        viewModelScope.launch {
            bd.withTransaction {
                bd.preguntas().insertarConId(b.preguntas)
                bd.tests().insertarRespuestas(b.respuestas)
            }
        }
    }

    /** Borra el temario con sus preguntas y su PDF. Los tests hechos se quedan en el historial. */
    fun borrarTemario(alTerminar: () -> Unit) {
        val id = temarioId.value ?: return
        viewModelScope.launch {
            bd.temarios().porId(id)?.let {
                bd.temarios().borrar(it)
                lector.borrar(it.archivo)
            }
            alTerminar()
        }
    }
}
