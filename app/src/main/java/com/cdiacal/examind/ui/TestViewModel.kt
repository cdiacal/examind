package com.cdiacal.examind.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.examind.datos.BaseDatos
import com.cdiacal.examind.datos.PreferenciasTest
import com.cdiacal.examind.datos.RespuestaEntidad
import com.cdiacal.examind.datos.TemarioConCuentas
import com.cdiacal.examind.datos.TestEntidad
import com.cdiacal.examind.datos.UltimaConfig
import com.cdiacal.examind.datos.aCandidata
import com.cdiacal.examind.datos.aNueva
import com.cdiacal.examind.nucleo.Candidata
import com.cdiacal.examind.nucleo.ConfigTest
import com.cdiacal.examind.nucleo.Correccion
import com.cdiacal.examind.nucleo.Origen
import com.cdiacal.examind.nucleo.OrigenTest
import com.cdiacal.examind.nucleo.PreguntaEnTest
import com.cdiacal.examind.nucleo.SelectorPreguntas
import com.cdiacal.examind.nucleo.SesionTest
import com.cdiacal.examind.nucleo.TestTerminado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cuántas de las [candidatas] entrarían en un test con este origen y filtro. */
fun contarDisponibles(candidatas: List<Candidata>, origen: OrigenTest, soloFalladas: Boolean): Int =
    candidatas.count { c ->
        val origenVale = when (origen) {
            OrigenTest.TEMARIO -> c.origen == Origen.TEMARIO
            OrigenTest.ONLINE -> c.origen == Origen.ONLINE
            OrigenTest.MEZCLA -> true
        }
        origenVale && (!soloFalladas || c.ultimaFallada)
    }

sealed interface EstadoTest {
    data object Configurando : EstadoTest
    data class EnCurso(val sesion: SesionTest) : EstadoTest
    data class Terminado(val sesion: SesionTest, val fin: TestTerminado, val temarioId: Long) : EstadoTest
}

class TestViewModel(app: Application) : AndroidViewModel(app) {
    private val bd = BaseDatos.obtener(app)
    private val prefs = PreferenciasTest(app)

    val temarios: StateFlow<List<TemarioConCuentas>?> =
        bd.temarios().observarConCuentas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ultima = MutableStateFlow(prefs.leer())
    /** Configuración elegida en pantalla (empieza con la del último test). */
    val ultima: StateFlow<UltimaConfig> = _ultima.asStateFlow()

    private val _candidatas = MutableStateFlow<List<Candidata>>(emptyList())
    /** Preguntas del temario elegido con sus estadísticas, para contar cuántas hay de cada tipo. */
    val candidatas: StateFlow<List<Candidata>> = _candidatas.asStateFlow()

    private val _estado = MutableStateFlow<EstadoTest>(EstadoTest.Configurando)
    val estado: StateFlow<EstadoTest> = _estado.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Vuelve a la configuración. Si el temario guardado ya no existe, elige el primero. */
    fun preparar(temariosDisponibles: List<TemarioConCuentas>) {
        _estado.value = EstadoTest.Configurando
        val actual = _ultima.value.temarioId
        if (temariosDisponibles.none { it.id == actual }) {
            elegirTemario(temariosDisponibles.firstOrNull()?.id)
        } else {
            cargarCandidatas(actual)
        }
    }

    fun elegirTemario(id: Long?) {
        _ultima.update { it.copy(temarioId = id) }
        cargarCandidatas(id)
    }

    fun cambiarConfig(config: ConfigTest) = _ultima.update { it.copy(config = config) }
    fun cambiarCorreccion(correccion: Correccion) = _ultima.update { it.copy(correccion = correccion) }
    fun olvidarError() { _error.value = null }

    private fun cargarCandidatas(temarioId: Long?) {
        viewModelScope.launch {
            _candidatas.value = if (temarioId == null) emptyList() else bd.preguntas().candidatas(temarioId).map { it.aCandidata() }
        }
    }

    fun empezar() {
        val u = _ultima.value
        val temarioId = u.temarioId ?: return
        viewModelScope.launch {
            val candidatas = bd.preguntas().candidatas(temarioId).map { it.aCandidata() }
            val ids = SelectorPreguntas.elegir(candidatas, u.config)
            if (ids.isEmpty()) {
                _error.value = if (u.config.soloFalladas) "No tienes preguntas falladas con estas opciones. Quita «Solo falladas» o elige otro origen."
                else "No hay preguntas con estas opciones. Elige otro origen o añade preguntas al temario."
                return@launch
            }
            prefs.guardar(u)
            iniciar(ids, u.correccion)
        }
    }

    /** Un test nuevo solo con las que se fallaron o se dejaron en blanco en el que acaba de terminar. */
    fun repetirFalladas() {
        val terminado = _estado.value as? EstadoTest.Terminado ?: return
        val ids = terminado.fin.respuestas.filter { it.acertada != true }.map { it.preguntaId }
        if (ids.isEmpty()) return
        viewModelScope.launch { iniciar(ids.shuffled(), terminado.sesion.correccion) }
    }

    private suspend fun iniciar(ids: List<Long>, correccion: Correccion) {
        val porId = bd.preguntas().porIds(ids).associateBy { it.id }
        val preguntas = ids.mapNotNull { id -> porId[id]?.let { PreguntaEnTest.desde(id, it.aNueva()) } }
        if (preguntas.isEmpty()) {
            _error.value = "Las preguntas de este test ya no existen. Vuelve a configurarlo."
            return
        }
        _estado.value = EstadoTest.EnCurso(SesionTest(preguntas, correccion, System.currentTimeMillis()))
    }

    private fun cambiarSesion(cambio: (SesionTest) -> SesionTest) {
        _estado.update { e -> if (e is EstadoTest.EnCurso) e.copy(sesion = cambio(e.sesion)) else e }
    }

    fun responder(opcion: Int) = cambiarSesion { it.responder(opcion) }
    fun siguiente() = cambiarSesion { it.siguiente() }
    fun anterior() = cambiarSesion { it.anterior() }

    /** Corrige el test y lo guarda en el historial. */
    fun terminar() {
        val enCurso = _estado.value as? EstadoTest.EnCurso ?: return
        val temarioId = _ultima.value.temarioId ?: return
        val ahora = System.currentTimeMillis()
        val fin = enCurso.sesion.terminar(ahora)
        val origen = when {
            enCurso.sesion.preguntas.all { it.origen == Origen.TEMARIO } -> OrigenTest.TEMARIO
            enCurso.sesion.preguntas.all { it.origen == Origen.ONLINE } -> OrigenTest.ONLINE
            else -> OrigenTest.MEZCLA
        }
        _estado.value = EstadoTest.Terminado(enCurso.sesion, fin, temarioId)
        viewModelScope.launch {
            val r = fin.resultado
            bd.tests().guardar(
                TestEntidad(
                    temarioId = temarioId, fecha = ahora, origen = origen.name, aciertos = r.aciertos, fallos = r.fallos,
                    enBlanco = r.enBlanco, nota = r.nota, duracionSegundos = fin.duracionSegundos
                ),
                fin.respuestas.map { RespuestaEntidad(testId = 0, preguntaId = it.preguntaId, orden = it.orden, elegida = it.elegidaOriginal, acertada = it.acertada) }
            )
            cargarCandidatas(temarioId)
        }
    }

    /** Sale del test sin guardarlo. */
    fun abandonar() {
        _estado.value = EstadoTest.Configurando
    }
}
