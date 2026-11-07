package com.cdiacal.examind.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.examind.datos.BaseDatos
import com.cdiacal.examind.datos.DatosEjemplo
import com.cdiacal.examind.datos.TemarioConCuentas
import com.cdiacal.examind.datos.TestEntidad
import com.cdiacal.examind.datos.aResultado
import com.cdiacal.examind.nucleo.ResumenInicio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoInicio(
    val resumen: ResumenInicio,
    val temarios: List<TemarioConCuentas>,
    val ultimosTests: List<TestEntidad>,
    val nombresTemario: Map<Long, String>
)

class InicioViewModel(app: Application) : AndroidViewModel(app) {
    private val bd = BaseDatos.obtener(app)

    /** null mientras se lee la base de datos por primera vez. */
    val estado: StateFlow<EstadoInicio?> = combine(
        bd.temarios().observarConCuentas(),
        bd.preguntas().observarTotal(),
        bd.tests().observar()
    ) { temarios, totalPreguntas, tests ->
        EstadoInicio(
            resumen = ResumenInicio.calcular(temarios.size, totalPreguntas, tests.map { it.aResultado() }),
            temarios = temarios,
            ultimosTests = tests.take(5),
            nombresTemario = temarios.associate { it.id to it.nombre }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Carga el temario de ejemplo si no está ya. Llama a [alTerminar] con lo creado (null si ya existía). */
    fun cargarEjemplo(alTerminar: (DatosEjemplo.Creado?) -> Unit) {
        viewModelScope.launch {
            val creado = if (bd.temarios().contarLlamados(DatosEjemplo.NOMBRE) > 0) null
            else DatosEjemplo.cargar(bd, System.currentTimeMillis())
            alTerminar(creado)
        }
    }

    fun deshacerEjemplo(creado: DatosEjemplo.Creado) {
        viewModelScope.launch { DatosEjemplo.deshacer(bd, creado) }
    }
}
