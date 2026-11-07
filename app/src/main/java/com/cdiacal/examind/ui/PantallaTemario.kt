package com.cdiacal.examind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.examind.datos.PreguntaEntidad
import com.cdiacal.examind.estilo.BarraSeleccion
import com.cdiacal.examind.estilo.BotonPrincipal
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.CabeceraSimple
import com.cdiacal.examind.estilo.CasillaSeleccion
import com.cdiacal.examind.estilo.DialogoTarjeta
import com.cdiacal.examind.estilo.EstadoVacio
import com.cdiacal.examind.estilo.FilaPestanas
import com.cdiacal.examind.estilo.TarjetaDestacada
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.colorGasto
import com.cdiacal.examind.estilo.colorIngreso
import com.cdiacal.examind.estilo.filaSeleccionable
import com.cdiacal.examind.nucleo.Formato
import com.cdiacal.examind.nucleo.PreguntaNueva
import kotlinx.coroutines.launch

/** Cuántos fragmentos se usan en cada «Crear preguntas». «Todos» se guarda como un número muy alto. */
private val CUANTOS = listOf(5, 10, 20, 9_999)

/** Un temario: sus preguntas, crear más con la IA, hacer un test con él y eliminarlo. */
@Composable
fun PantallaTemario(
    vm: TemarioViewModel,
    temarioId: Long,
    avisos: SnackbarHostState,
    onVolver: () -> Unit,
    onHacerTest: (Long) -> Unit
) {
    LaunchedEffect(temarioId) { vm.abrir(temarioId) }
    val temario by vm.temario.collectAsStateWithLifecycle()
    val delTemario by vm.delTemario.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()
    val generacion by vm.generacion.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()

    val revisando = generacion as? EstadoGeneracion.Revisando
    if (revisando != null) {
        PantallaRevision(vm, revisando) { guardadas, n ->
            alcance.launch {
                avisos.currentSnackbarData?.dismiss()
                val r = avisos.showSnackbar("${Formato.cantidad(n, "pregunta guardada", "preguntas guardadas")}.", actionLabel = "Deshacer")
                if (r == SnackbarResult.ActionPerformed) vm.deshacerGuardado(guardadas)
            }
        }
        return
    }

    // Mientras la IA trabaja, que no se apague la pantalla (si se va la app, se para)
    val ocupado = generacion is EstadoGeneracion.Generando || generacion is EstadoGeneracion.CargandoMotor
    val vista = LocalView.current
    DisposableEffect(ocupado) {
        vista.keepScreenOn = ocupado
        onDispose { vista.keepScreenOn = false }
    }

    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var seleccion by remember(temarioId) { mutableStateOf(setOf<Long>()) }
    var abierta by remember(temarioId) { mutableStateOf<Long?>(null) }
    var preguntarBorrarTemario by remember { mutableStateOf(false) }
    var preguntarBorrarPreguntas by remember { mutableStateOf(false) }
    val lista = if (pestana == 0) delTemario else online

    BackHandler {
        when {
            seleccion.isNotEmpty() -> seleccion = emptySet()
            else -> onVolver()
        }
    }

    val t = temario
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CabeceraSimple(
                titulo = t?.nombre ?: "Temario",
                subtitulo = t?.let { if (it.paginas > 0) Formato.cantidad(it.paginas, "página") else null },
                onVolver = onVolver
            ) {
                IconButton(onClick = { preguntarBorrarTemario = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar temario", tint = Color.White)
                }
            }
            if (t == null) return@Column

            val total = t.preguntasTemario + t.preguntasOnline
            TarjetaDestacada(
                titulo = "Preguntas",
                cifra = Formato.miles(total.toLong()),
                detalle = "Del temario: ${Formato.miles(t.preguntasTemario.toLong())} · Online: ${Formato.miles(t.preguntasOnline.toLong())}"
            )
            if (total > 0) BotonPrincipal("Hacer un test con este temario", Modifier.fillMaxWidth()) { onHacerTest(t.id) }

            TarjetaCrear(vm, generacion, t.fragmentosSinUsar)

            FilaPestanas(
                listOf("Del temario (${delTemario.size})", "Online (${online.size})"),
                pestana,
                { pestana = it; seleccion = emptySet() }
            )
            TarjetaPanel {
                if (lista.isEmpty()) {
                    EstadoVacio(
                        if (pestana == 0) "Aún no hay preguntas de este temario.\nToca «Crear preguntas» para que la IA las escriba."
                        else "Aún no hay preguntas de exámenes online.\nLa búsqueda de exámenes llega en la próxima versión."
                    )
                } else {
                    lista.forEach { p ->
                        FilaPregunta(
                            p,
                            seleccionada = p.id in seleccion,
                            modoSeleccion = seleccion.isNotEmpty(),
                            abierta = abierta == p.id,
                            onAbrir = { abierta = if (abierta == p.id) null else p.id },
                            onAlternar = { seleccion = if (p.id in seleccion) seleccion - p.id else seleccion + p.id }
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (seleccion.isNotEmpty()) 72.dp else 0.dp))
        }

        if (seleccion.isNotEmpty()) {
            BarraSeleccion(
                cantidad = seleccion.size,
                detalle = if (pestana == 0) "Del temario" else "Online",
                onTodos = { seleccion = lista.map { it.id }.toSet() },
                onEliminar = { preguntarBorrarPreguntas = true },
                onCancelar = { seleccion = emptySet() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }

    if (preguntarBorrarPreguntas) {
        val n = seleccion.size
        DialogoTarjeta(
            titulo = "¿Eliminar ${Formato.cantidad(n, "pregunta")}?",
            onCerrar = { preguntarBorrarPreguntas = false },
            textoConfirmar = "Eliminar",
            onConfirmar = {
                preguntarBorrarPreguntas = false
                val borrar = lista.filter { it.id in seleccion }
                seleccion = emptySet()
                vm.borrarPreguntas(borrar) { borradas ->
                    alcance.launch {
                        avisos.currentSnackbarData?.dismiss()
                        val r = avisos.showSnackbar("${Formato.cantidad(borradas.preguntas.size, "pregunta eliminada", "preguntas eliminadas")}.", actionLabel = "Deshacer")
                        if (r == SnackbarResult.ActionPerformed) vm.restaurarPreguntas(borradas)
                    }
                }
            }
        ) {
            Text("Dejarán de salir en los tests. Podrás deshacerlo justo después.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (preguntarBorrarTemario && t != null) {
        DialogoTarjeta(
            titulo = "¿Eliminar «${t.nombre}»?",
            onCerrar = { preguntarBorrarTemario = false },
            textoConfirmar = "Eliminar",
            onConfirmar = {
                preguntarBorrarTemario = false
                vm.borrarTemario {
                    onVolver()
                    alcance.launch { avisos.currentSnackbarData?.dismiss(); avisos.showSnackbar("Temario eliminado.") }
                }
            }
        ) {
            Text(
                "Se borrarán sus ${Formato.cantidad(t.preguntasTemario + t.preguntasOnline, "pregunta")} y la copia del PDF. Los tests que ya hiciste se quedan en el historial. No se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TarjetaCrear(vm: TemarioViewModel, generacion: EstadoGeneracion, sinUsar: Int) {
    var cuantos by rememberSaveable { mutableIntStateOf(5) }
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    val barra = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
    TarjetaPanel {
        Text("Crear preguntas con la IA", style = MaterialTheme.typography.titleMedium)
        when (generacion) {
            EstadoGeneracion.CargandoMotor -> {
                Text("Cargando la IA del móvil… puede tardar unos segundos.", style = MaterialTheme.typography.bodySmall, color = gris)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(barra, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(8.dp))
                BotonSecundario("Cancelar", onClick = vm::cancelarGeneracion)
            }
            is EstadoGeneracion.Generando -> {
                val a = generacion.avance
                Text(
                    "Fragmento ${minOf(a.hechos + 1, a.total)} de ${a.total} con ${generacion.motor} · ${Formato.cantidad(a.nuevas, "pregunta nueva", "preguntas nuevas")}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(if (a.total > 0) a.hechos.toFloat() / a.total else 0f, barra, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("No salgas de la app mientras tanto: si se cierra, se para.", style = MaterialTheme.typography.bodySmall, color = gris)
                Spacer(Modifier.height(8.dp))
                BotonSecundario("Cancelar", onClick = vm::cancelarGeneracion)
            }
            else -> {
                if (generacion is EstadoGeneracion.Error) {
                    Text(generacion.mensaje, style = MaterialTheme.typography.bodySmall, color = colorGasto())
                    Spacer(Modifier.height(8.dp))
                }
                if (sinUsar == 0) {
                    Text("Ya se han usado todos los fragmentos del temario.", style = MaterialTheme.typography.bodySmall, color = gris)
                } else {
                    Text(
                        "Quedan ${Formato.cantidad(sinUsar, "fragmento")} sin usar. La IA escribe unas 3 preguntas por fragmento y tú eliges cuáles guardar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = gris
                    )
                    Spacer(Modifier.height(8.dp))
                    FilaPestanas(
                        CUANTOS.map { if (it > 1000) "Todos" else "$it fragmentos" },
                        CUANTOS.indexOf(cuantos).coerceAtLeast(0),
                        { cuantos = CUANTOS[it] }
                    )
                    Spacer(Modifier.height(8.dp))
                    BotonPrincipal("Crear preguntas", onClick = { vm.generar(cuantos) })
                }
            }
        }
    }
}

@Composable
private fun FilaPregunta(
    p: PreguntaEntidad,
    seleccionada: Boolean,
    modoSeleccion: Boolean,
    abierta: Boolean,
    onAbrir: () -> Unit,
    onAlternar: () -> Unit
) {
    Column(Modifier.fillMaxWidth().filaSeleccionable(seleccionada, modoSeleccion, onAbrir, onAlternar).padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (modoSeleccion) CasillaSeleccion(seleccionada)
            Column(Modifier.weight(1f)) {
                Text(p.enunciado, style = MaterialTheme.typography.bodyMedium, maxLines = if (abierta) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "✓ ${p.opciones.getOrNull(p.correcta).orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorIngreso(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (abierta) {
            Spacer(Modifier.height(8.dp))
            DetallePregunta(p.opciones, p.correcta, p.explicacion)
            Text(
                when {
                    p.fuenteUrl != null -> "Fuente: ${p.fuenteTitulo} · ${p.fuenteUrl}"
                    p.pagina != null -> "Fuente: tu temario, página ${p.pagina}"
                    else -> "Fuente: tu temario"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

/** Las cuatro opciones con la correcta marcada (con texto, no solo color) y la explicación. */
@Composable
private fun DetallePregunta(opciones: List<String>, correcta: Int, explicacion: String) {
    opciones.forEachIndexed { i, o ->
        val buena = i == correcta
        Text(
            "${'A' + i}) $o" + if (buena) "  ✓ Correcta" else "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (buena) FontWeight.SemiBold else FontWeight.Normal,
            color = if (buena) colorIngreso() else MaterialTheme.colorScheme.onSurface
        )
    }
    if (explicacion.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(explicacion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(4.dp))
}

/** Revisar lo que ha escrito la IA antes de guardarlo: todas marcadas, se desmarcan las que no convenzan. */
@Composable
private fun PantallaRevision(vm: TemarioViewModel, r: EstadoGeneracion.Revisando, onGuardado: (PreguntasGuardadas, Int) -> Unit) {
    var preguntarDescartar by remember { mutableStateOf(false) }
    BackHandler { preguntarDescartar = true }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CabeceraSimple(titulo = "Revisa las preguntas", subtitulo = r.resumen, onVolver = { preguntarDescartar = true })
            Text(
                "La IA puede equivocarse: comprueba la respuesta marcada con ✓ y desmarca las que no te convenzan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                TextButton(onClick = { vm.seleccionarTodas(true) }) { Text("Marcar todas") }
                TextButton(onClick = { vm.seleccionarTodas(false) }) { Text("Desmarcar todas") }
            }
            r.preguntas.forEachIndexed { i, p -> TarjetaRevision(p, i in r.seleccion) { vm.alternar(i) } }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonSecundario("Descartar", Modifier.weight(1f), onClick = { preguntarDescartar = true })
            BotonPrincipal(
                if (r.seleccion.isEmpty()) "Guardar" else "Guardar ${r.seleccion.size}",
                Modifier.weight(1f),
                habilitado = r.seleccion.isNotEmpty()
            ) { vm.guardarRevisadas(onGuardado) }
        }
    }

    if (preguntarDescartar) {
        DialogoTarjeta(
            titulo = "¿Descartar estas preguntas?",
            onCerrar = { preguntarDescartar = false },
            textoConfirmar = "Descartar",
            onConfirmar = { preguntarDescartar = false; vm.descartar() },
            textoCancelar = "Seguir revisando"
        ) {
            Text("No se guardará ninguna. Los fragmentos quedan sin usar para crearlas otra vez.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TarjetaRevision(p: PreguntaNueva, marcada: Boolean, onAlternar: () -> Unit) {
    TarjetaPanel(Modifier.clickable(onClick = onAlternar)) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked = marcada, onCheckedChange = { onAlternar() })
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                Text(p.enunciado, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                DetallePregunta(p.opciones, p.correcta, p.explicacion)
                p.pagina?.let { Text("Página $it del temario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
