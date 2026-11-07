package com.cdiacal.examind.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cdiacal.examind.datos.BaseDatos
import com.cdiacal.examind.datos.FragmentoEntidad
import com.cdiacal.examind.datos.DocumentoNoLegible
import com.cdiacal.examind.datos.LectorTemario
import com.cdiacal.examind.datos.TemarioEntidad
import com.cdiacal.examind.nucleo.Fragmento
import com.cdiacal.examind.nucleo.NombreTemario
import com.cdiacal.examind.nucleo.Troceador
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoImportar {
    data object Inactivo : EstadoImportar
    data class Leyendo(val pagina: Int, val total: Int) : EstadoImportar
    data class VistaPrevia(
        val archivo: String,
        val huella: String,
        val nombre: String,
        /** Páginas del documento; 0 si es un Word sin marcas de página. */
        val paginas: Int,
        val tipo: String,
        val conTexto: Boolean,
        val fragmentos: List<Fragmento>
    ) : EstadoImportar
    data class Error(val mensaje: String) : EstadoImportar
}

/** Lo que se acaba de guardar, para poder deshacerlo. */
data class TemarioGuardado(val id: Long, val nombre: String, val archivo: String)

class ImportarViewModel(app: Application) : AndroidViewModel(app) {
    private val bd = BaseDatos.obtener(app)
    private val lector = LectorTemario(app)

    private val _estado = MutableStateFlow<EstadoImportar>(EstadoImportar.Inactivo)
    val estado: StateFlow<EstadoImportar> = _estado.asStateFlow()
    private var lectura: Job? = null

    fun leer(uri: Uri) {
        lectura?.cancel()
        _estado.value = EstadoImportar.Leyendo(0, 0)
        lectura = viewModelScope.launch {
            _estado.value = try {
                val pdf = lector.leer(uri) { pagina, total -> _estado.value = EstadoImportar.Leyendo(pagina, total) }
                val existente = bd.temarios().porHuellaArchivo(pdf.huella)
                if (existente != null) {
                    lector.borrar(pdf.archivo)
                    EstadoImportar.Error("Este documento ya está añadido como «${existente.nombre}». No hace falta añadirlo otra vez.")
                } else {
                    val limpias = Troceador.limpiarPaginas(pdf.paginas)
                    val nombre = NombreTemario.unico(NombreTemario.desdeArchivo(pdf.nombreOriginal), bd.temarios().nombres())
                    EstadoImportar.VistaPrevia(
                        archivo = pdf.archivo,
                        huella = pdf.huella,
                        nombre = nombre,
                        paginas = if (pdf.conPaginas) pdf.paginas.size else 0,
                        tipo = pdf.tipo,
                        conTexto = Troceador.tieneTexto(limpias),
                        // Sin páginas, los fragmentos van con página 0 y las preguntas no citarán ninguna
                        fragmentos = Troceador.trocear(limpias).map { if (pdf.conPaginas) it else it.copy(pagina = 0) }
                    )
                }
            } catch (e: DocumentoNoLegible) {
                EstadoImportar.Error(e.message ?: "No se pudo leer el documento.")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                EstadoImportar.Error("No se pudo leer el documento (${e.javaClass.simpleName}). Prueba con otro archivo.")
            }
        }
    }

    /** Guarda el temario de la vista previa con el nombre y tema elegidos. */
    fun guardar(nombre: String, tema: String, alTerminar: (TemarioGuardado) -> Unit) {
        val vista = _estado.value as? EstadoImportar.VistaPrevia ?: return
        viewModelScope.launch {
            val nombreFinal = nombre.trim().ifEmpty { vista.nombre }
            val id = bd.temarios().guardarConFragmentos(
                TemarioEntidad(
                    nombre = nombreFinal,
                    tema = tema.trim().ifEmpty { nombreFinal },
                    archivo = vista.archivo,
                    paginas = vista.paginas,
                    creadoEl = System.currentTimeMillis(),
                    huellaArchivo = vista.huella
                ),
                vista.fragmentos.mapIndexed { i, f -> FragmentoEntidad(temarioId = 0, pagina = f.pagina, orden = i, texto = f.texto) }
            )
            _estado.value = EstadoImportar.Inactivo
            alTerminar(TemarioGuardado(id, nombreFinal, vista.archivo))
        }
    }

    /** Deshace el guardado: borra el temario (y sus fragmentos) y la copia del documento. */
    fun deshacer(guardado: TemarioGuardado) {
        viewModelScope.launch {
            bd.temarios().porId(guardado.id)?.let { bd.temarios().borrar(it) }
            lector.borrar(guardado.archivo)
        }
    }

    /** Cierra la importación sin guardar y borra la copia del documento si la había. */
    fun cancelar() {
        lectura?.cancel()
        (_estado.value as? EstadoImportar.VistaPrevia)?.let { lector.borrar(it.archivo) }
        _estado.value = EstadoImportar.Inactivo
    }
}
