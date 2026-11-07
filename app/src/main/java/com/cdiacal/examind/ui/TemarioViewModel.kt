package com.cdiacal.examind.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.cdiacal.examind.datos.BaseDatos
import com.cdiacal.examind.datos.LectorTemario
import com.cdiacal.examind.datos.PreguntaEntidad
import com.cdiacal.examind.datos.RespuestaEntidad
import com.cdiacal.examind.datos.TemarioConCuentas
import com.cdiacal.examind.datos.aEntidad
import com.cdiacal.examind.datos.BorradorEntidad
import com.cdiacal.examind.datos.aNueva
import com.cdiacal.examind.ia.GestorIA
import com.cdiacal.examind.segundoplano.TrabajoGenerar
import com.cdiacal.examind.segundoplano.TrabajoOnline
import com.cdiacal.examind.online.ClienteWeb
import com.cdiacal.examind.online.FalloWeb
import com.cdiacal.examind.nucleo.BusquedaWeb
import com.cdiacal.examind.nucleo.ResultadoWeb
import kotlinx.coroutines.Job
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Cómo va la creación de preguntas en segundo plano para este temario. */
sealed interface EstadoTrabajo {
    data object Inactivo : EstadoTrabajo
    /** Esperando a que termine otro temario (la IA hace uno cada vez). */
    data object EnCola : EstadoTrabajo
    data class Generando(val motor: String?, val hechos: Int, val total: Int, val nuevas: Int, val texto: String?) : EstadoTrabajo
}

/** La búsqueda de exámenes online. [usadas]: direcciones (normalizadas) de las que ya hay preguntas en el temario. */
sealed interface EstadoBusqueda {
    data object Inactivo : EstadoBusqueda
    data object Buscando : EstadoBusqueda
    data class Resultados(val lista: List<ResultadoWeb>, val seleccion: Set<String>, val usadas: Set<String>) : EstadoBusqueda
    data class Error(val mensaje: String) : EstadoBusqueda
}

/** Lo que se acaba de guardar al revisar, para poder deshacerlo (vuelven a ser borradores). */
data class PreguntasGuardadas(val ids: List<Long>, val borradores: List<BorradorEntidad>, val fragmentosDevueltos: List<Long>)

/** Lo que se acaba de borrar, para poder deshacerlo. */
data class PreguntasBorradas(val preguntas: List<PreguntaEntidad>, val respuestas: List<RespuestaEntidad>)

@OptIn(ExperimentalCoroutinesApi::class)
class TemarioViewModel(app: Application) : AndroidViewModel(app) {
    private val bd = BaseDatos.obtener(app)
    private val gestor = GestorIA(app)
    private val lector = LectorTemario(app)

    private val temarioId = MutableStateFlow<Long?>(null)

    val temario: StateFlow<TemarioConCuentas?> = combine(bd.temarios().observarConCuentas(), temarioId) { lista, id ->
        lista.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun observar(origen: String): Flow<List<PreguntaEntidad>> =
        temarioId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else bd.preguntas().observar(id, origen) }

    val delTemario = observar("TEMARIO").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val online = observar("ONLINE").stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Preguntas que ha escrito la IA y faltan por revisar. */
    val borradores: StateFlow<List<BorradorEntidad>> =
        temarioId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else bd.borradores().observar(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** El trabajo en segundo plano de este temario, si hay uno en marcha o en cola. */
    val trabajo: StateFlow<EstadoTrabajo> = temarioId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else WorkManager.getInstance(getApplication()).getWorkInfosByTagFlow(TrabajoGenerar.etiqueta(id))
    }.map { infos ->
        val enMarcha = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        when {
            enMarcha != null -> enMarcha.progress.let { d ->
                EstadoTrabajo.Generando(
                    d.getString(TrabajoGenerar.MOTOR),
                    d.getInt(TrabajoGenerar.HECHOS, 0),
                    d.getInt(TrabajoGenerar.TOTAL, 0),
                    d.getInt(TrabajoGenerar.NUEVAS, 0),
                    d.getString(TrabajoGenerar.TEXTO)
                )
            }
            infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED } -> EstadoTrabajo.EnCola
            else -> EstadoTrabajo.Inactivo
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoTrabajo.Inactivo)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _revisando = MutableStateFlow(false)
    /** true mientras se está en la pantalla de revisar los borradores. */
    val revisando: StateFlow<Boolean> = _revisando.asStateFlow()

    private val _desmarcados = MutableStateFlow(setOf<Long>())
    /** Borradores que el usuario ha desmarcado (por defecto se guardan todos). */
    val desmarcados: StateFlow<Set<Long>> = _desmarcados.asStateFlow()

    private val _busqueda = MutableStateFlow<EstadoBusqueda>(EstadoBusqueda.Inactivo)
    val busqueda: StateFlow<EstadoBusqueda> = _busqueda.asStateFlow()
    private var buscando: Job? = null

    /** Busca exámenes del [tema] en internet y lo guarda como tema del temario para la próxima vez. */
    fun buscar(tema: String) {
        val id = temarioId.value ?: return
        val t = tema.trim()
        if (t.isEmpty()) return
        buscando?.cancel()
        buscando = viewModelScope.launch {
            _busqueda.value = EstadoBusqueda.Buscando
            bd.temarios().cambiarTema(id, t)
            _busqueda.value = try {
                val lista = ClienteWeb(getApplication()).buscar(t)
                val usadas = bd.borradores().urlsUsadas(id).map(BusquedaWeb::normalizarUrl).toSet()
                if (lista.isEmpty()) EstadoBusqueda.Error("No se ha encontrado nada para «$t». Prueba con otras palabras (por ejemplo, el nombre de la asignatura) o pega un enlace.")
                else EstadoBusqueda.Resultados(
                    lista,
                    // Se proponen las 3 primeras que aún no se han usado
                    lista.filter { BusquedaWeb.normalizarUrl(it.url) !in usadas }.take(3).map { it.url }.toSet(),
                    usadas
                )
            } catch (e: FalloWeb) {
                EstadoBusqueda.Error(e.message.orEmpty())
            }
        }
    }

    fun alternarResultado(url: String) {
        val r = _busqueda.value as? EstadoBusqueda.Resultados ?: return
        _busqueda.value = r.copy(seleccion = if (url in r.seleccion) r.seleccion - url else r.seleccion + url)
    }

    /** Añade a mano un enlace a la lista (marcado). Devuelve un mensaje de error si no es una dirección válida. */
    fun anadirEnlace(texto: String): String? {
        val url = BusquedaWeb.urlValida(texto)
            ?: return "Eso no parece una dirección web. Cópiala entera desde el navegador (por ejemplo, https://…)."
        val actual = _busqueda.value as? EstadoBusqueda.Resultados
        val nuevo = ResultadoWeb(BusquedaWeb.dominio(url), url, "Añadido a mano")
        _busqueda.value = if (actual == null) EstadoBusqueda.Resultados(listOf(nuevo), setOf(url), emptySet())
        else actual.copy(lista = listOf(nuevo) + actual.lista.filter { it.url != url }, seleccion = actual.seleccion + url)
        return null
    }

    fun cerrarBusqueda() { buscando?.cancel(); _busqueda.value = EstadoBusqueda.Inactivo }

    /** Lanza en segundo plano sacar preguntas de las páginas marcadas. */
    fun sacarPreguntasOnline(tema: String) {
        val id = temarioId.value ?: return
        val r = _busqueda.value as? EstadoBusqueda.Resultados ?: return
        val elegidas = r.lista.filter { it.url in r.seleccion }
        if (elegidas.isEmpty()) return
        viewModelScope.launch {
            _error.value = null
            if (gestor.eleccion() == null) {
                _busqueda.value = EstadoBusqueda.Error("Aún no hay IA en el móvil. Ve a Ajustes → IA del móvil y descarga un modelo.")
                return@launch
            }
            TrabajoOnline.lanzar(getApplication(), id, tema.trim(), elegidas.map { it.url to it.titulo })
            _busqueda.value = EstadoBusqueda.Inactivo
        }
    }

    fun abrir(id: Long) {
        if (temarioId.value != id) {
            cerrarBusqueda()
            _revisando.value = false
            _desmarcados.value = emptySet()
            _error.value = null
        }
        temarioId.value = id
    }

    /** Lanza la creación de preguntas en segundo plano con los siguientes [cuantos] fragmentos sin usar. */
    fun generar(cuantos: Int) {
        val id = temarioId.value ?: return
        viewModelScope.launch {
            _error.value = null
            if (gestor.eleccion() == null) {
                _error.value = "Aún no hay IA en el móvil. Ve a Ajustes → IA del móvil y descarga un modelo."
                return@launch
            }
            TrabajoGenerar.lanzar(getApplication(), id, cuantos)
        }
    }

    fun cancelarGeneracion() {
        temarioId.value?.let { TrabajoGenerar.cancelar(getApplication(), it) }
    }

    fun empezarRevision() { _desmarcados.value = emptySet(); _revisando.value = true }
    fun salirRevision() { _revisando.value = false }

    fun alternar(id: Long) {
        _desmarcados.value = _desmarcados.value.let { if (id in it) it - id else it + id }
    }

    fun seleccionarTodas(todas: Boolean) {
        _desmarcados.value = if (todas) emptySet() else borradores.value.map { it.id }.toSet()
    }

    /**
     * Guarda los borradores marcados de [revisados] (los que se veían en pantalla) como preguntas, sin duplicados, y
     * quita todos ellos de pendientes. Los fragmentos de los que no se ha guardado nada vuelven a «sin usar».
     */
    fun guardarRevisadas(revisados: List<BorradorEntidad>, alTerminar: (PreguntasGuardadas, Int) -> Unit) {
        val id = temarioId.value ?: return
        val quitados = _desmarcados.value
        viewModelScope.launch {
            val elegidos = revisados.filter { it.id !in quitados }
            val fragmentosConGuardadas = elegidos.map { it.fragmentoId }.toSet()
            val devueltos = revisados.map { it.fragmentoId }.distinct().filter { it !in fragmentosConGuardadas }
            val ahora = System.currentTimeMillis()
            val ids = bd.withTransaction {
                val insertadas = bd.preguntas().insertar(elegidos.mapIndexed { i, b -> b.aNueva().aEntidad(id, ahora + i) }).filter { it > 0 }
                bd.borradores().borrar(revisados)
                bd.temarios().desmarcarUsados(devueltos)
                insertadas
            }
            _revisando.value = false
            _desmarcados.value = emptySet()
            alTerminar(PreguntasGuardadas(ids, revisados, devueltos), ids.size)
        }
    }

    /** Descarta todos los borradores que se veían: no se guarda ninguno y sus fragmentos vuelven a «sin usar». */
    fun descartar(revisados: List<BorradorEntidad>) {
        viewModelScope.launch {
            bd.withTransaction {
                bd.borradores().borrar(revisados)
                bd.temarios().desmarcarUsados(revisados.map { it.fragmentoId }.distinct())
            }
            _revisando.value = false
        }
    }

    /** Deshace el guardado: quita las preguntas guardadas y deja otra vez los borradores pendientes de revisar. */
    fun deshacerGuardado(g: PreguntasGuardadas) {
        viewModelScope.launch {
            bd.withTransaction {
                bd.preguntas().borrarPorIds(g.ids)
                bd.borradores().insertarConId(g.borradores)
                bd.temarios().marcarUsados(g.fragmentosDevueltos)
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
        TrabajoGenerar.cancelar(getApplication(), id)
        viewModelScope.launch {
            bd.temarios().porId(id)?.let {
                bd.temarios().borrar(it)
                lector.borrar(it.archivo)
            }
            alTerminar()
        }
    }
}
