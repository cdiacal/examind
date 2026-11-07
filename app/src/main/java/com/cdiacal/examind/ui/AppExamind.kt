package com.cdiacal.examind.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdiacal.examind.datos.TemarioConCuentas
import com.cdiacal.examind.datos.TestEntidad
import com.cdiacal.examind.estilo.BotonPrincipal
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.CabeceraHero
import com.cdiacal.examind.estilo.CabeceraSimple
import com.cdiacal.examind.estilo.CifraHero
import com.cdiacal.examind.estilo.EstadoVacio
import com.cdiacal.examind.estilo.FilaLista
import com.cdiacal.examind.estilo.FilaPestanas
import com.cdiacal.examind.estilo.GaleriaEstilo
import com.cdiacal.examind.estilo.IconoInicial
import com.cdiacal.examind.estilo.MiniDatoHero
import com.cdiacal.examind.estilo.ModoTema
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.TemaState
import com.cdiacal.examind.estilo.TituloHero
import com.cdiacal.examind.estilo.colorGasto
import com.cdiacal.examind.estilo.colorIngreso
import com.cdiacal.examind.nucleo.Calificacion
import com.cdiacal.examind.nucleo.Formato
import kotlinx.coroutines.launch

/** Pantallas de la app. */
private enum class Pantalla { INICIO, TEST, ANADIR, AJUSTES, GALERIA }

@Composable
fun AppExamind() {
    var pantalla by rememberSaveable { mutableStateOf(Pantalla.INICIO) }
    val avisos = remember { SnackbarHostState() }
    val inicio: InicioViewModel = viewModel()
    val test: TestViewModel = viewModel()
    val importar: ImportarViewModel = viewModel()
    val alcance = rememberCoroutineScope()

    // Selector de archivos del sistema, solo PDF
    val elegirPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importar.leer(uri)
            pantalla = Pantalla.ANADIR
        }
    }
    val abrirSelector = { elegirPdf.launch(arrayOf("application/pdf")) }

    // El flujo del test gestiona su propio «atrás» (pregunta antes de salir de un test a medias)
    BackHandler(enabled = pantalla == Pantalla.AJUSTES || pantalla == Pantalla.GALERIA) {
        pantalla = if (pantalla == Pantalla.GALERIA) Pantalla.AJUSTES else Pantalla.INICIO
    }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        containerColor = MaterialTheme.colorScheme.background
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when (pantalla) {
                Pantalla.INICIO -> PantallaInicio(
                    inicio, avisos,
                    onAjustes = { pantalla = Pantalla.AJUSTES },
                    onHacerTest = { test.abandonar(); pantalla = Pantalla.TEST },
                    onAnadir = abrirSelector
                )
                Pantalla.ANADIR -> PantallaAnadirTemario(
                    importar,
                    onElegirOtro = abrirSelector,
                    onGuardado = { guardado ->
                        pantalla = Pantalla.INICIO
                        alcance.launch {
                            avisos.currentSnackbarData?.dismiss()
                            val r = avisos.showSnackbar("Temario «${guardado.nombre}» guardado.", actionLabel = "Deshacer")
                            if (r == SnackbarResult.ActionPerformed) importar.deshacer(guardado)
                        }
                    },
                    onSalir = { pantalla = Pantalla.INICIO }
                )
                Pantalla.TEST -> FlujoTest(test, onSalir = { test.abandonar(); pantalla = Pantalla.INICIO })
                Pantalla.AJUSTES -> PantallaAjustes(
                    inicio, avisos,
                    onVolver = { pantalla = Pantalla.INICIO },
                    onGaleria = { pantalla = Pantalla.GALERIA }
                )
                Pantalla.GALERIA -> Column(Modifier.fillMaxSize()) {
                    CabeceraSimple(
                        titulo = "Galería de estilo",
                        subtitulo = "Todos los componentes",
                        onVolver = { pantalla = Pantalla.AJUSTES },
                        modifier = Modifier.padding(16.dp)
                    )
                    GaleriaEstilo(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PantallaInicio(
    vm: InicioViewModel,
    avisos: SnackbarHostState,
    onAjustes: () -> Unit,
    onHacerTest: () -> Unit,
    onAnadir: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val resumen = estado?.resumen
            CabeceraHero(
                mostrarBarraSuperior = true,
                acciones = {
                    IconButton(onClick = onAjustes) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
                    }
                }
            ) {
                TituloHero("Examind", "Tests a partir de tus temarios")
                Spacer(Modifier.height(14.dp))
                CifraHero(
                    "Aciertos en tus últimos tests",
                    resumen?.mediaAciertos?.let { Formato.porcentaje(it) } ?: "—"
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    MiniDatoHero("Temarios", Formato.miles((resumen?.temarios ?: 0).toLong()), Modifier.weight(1f))
                    MiniDatoHero("Preguntas", Formato.miles((resumen?.preguntas ?: 0).toLong()), Modifier.weight(1f))
                    MiniDatoHero("Tests hechos", Formato.miles((resumen?.tests ?: 0).toLong()), Modifier.weight(1f))
                }
            }

            val e = estado
            if (e == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val hayPreguntas = e.resumen.preguntas > 0
                BotonPrincipal(
                    "Hacer un test",
                    modifier = Modifier.fillMaxWidth(),
                    habilitado = hayPreguntas,
                    onClick = onHacerTest
                )
                if (!hayPreguntas) {
                    Text(
                        "Para hacer un test necesitas preguntas: añade un temario con el botón +.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TarjetaPanel {
                    Text("Tus temarios", style = MaterialTheme.typography.titleMedium)
                    if (e.temarios.isEmpty()) {
                        EstadoVacio("Aún no tienes temarios.\nToca el botón + para añadir un PDF y crear tus primeras preguntas.")
                    } else {
                        e.temarios.forEach { FilaTemario(it) }
                    }
                }

                TarjetaPanel {
                    Text("Últimos tests", style = MaterialTheme.typography.titleMedium)
                    if (e.ultimosTests.isEmpty()) {
                        EstadoVacio("Cuando hagas tu primer test, verás aquí la nota.")
                    } else {
                        val ahora = remember(e) { System.currentTimeMillis() }
                        e.ultimosTests.forEach { test ->
                            FilaTest(test, e.nombresTemario[test.temarioId] ?: "Temario borrado", ahora)
                        }
                    }
                }
                Spacer(Modifier.height(64.dp)) // que el botón + no tape la última fila
            }
        }

        FloatingActionButton(
            onClick = onAnadir,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir temario")
        }
    }
}

@Composable
private fun FilaTemario(t: TemarioConCuentas) {
    val total = t.preguntasTemario + t.preguntasOnline
    FilaLista(
        titulo = t.nombre,
        importe = Formato.cantidad(total, "pregunta"),
        colorImporte = MaterialTheme.colorScheme.onSurfaceVariant,
        subtitulo = "Del temario: ${Formato.miles(t.preguntasTemario.toLong())} · Online: ${Formato.miles(t.preguntasOnline.toLong())}",
        icono = { IconoInicial(t.nombre, t.id) }
    )
}

@Composable
private fun FilaTest(test: TestEntidad, nombreTemario: String, ahora: Long) {
    val calificacion = Calificacion.de(test.nota)
    val total = test.aciertos + test.fallos + test.enBlanco
    FilaLista(
        titulo = nombreTemario,
        importe = "${Formato.nota(test.nota)} · ${calificacion.texto}",
        colorImporte = if (calificacion.aprobado) colorIngreso() else colorGasto(),
        subtitulo = "${Formato.fechaCorta(test.fecha, ahora)} · ${Formato.cantidad(test.aciertos, "acierto")} de ${Formato.miles(total.toLong())}",
        icono = { IconoInicial(nombreTemario, test.temarioId ?: 0) }
    )
}

@Composable
private fun PantallaAjustes(vm: InicioViewModel, avisos: SnackbarHostState, onVolver: () -> Unit, onGaleria: () -> Unit) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val modos = listOf(ModoTema.SISTEMA, ModoTema.CLARO, ModoTema.OSCURO)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CabeceraSimple(titulo = "Ajustes", onVolver = onVolver)
        TarjetaPanel {
            Text("Tema", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FilaPestanas(
                pestañas = listOf("Según el sistema", "Claro", "Oscuro"),
                seleccionada = modos.indexOf(TemaState.modo),
                onSeleccionar = { TemaState.establecer(context, modos[it]) }
            )
        }
        TarjetaPanel {
            Text("Temario de ejemplo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Añade 12 preguntas de biología y dos tests hechos para ver cómo queda la app. Las fuentes online del ejemplo son ficticias.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            BotonSecundario("Añadir el ejemplo", onClick = {
                vm.cargarEjemplo { creado ->
                    alcance.launch {
                        avisos.currentSnackbarData?.dismiss()
                        if (creado == null) {
                            avisos.showSnackbar("El temario de ejemplo ya está añadido.")
                        } else {
                            val r = avisos.showSnackbar("Temario de ejemplo añadido.", actionLabel = "Deshacer")
                            if (r == SnackbarResult.ActionPerformed) vm.deshacerEjemplo(creado)
                        }
                    }
                }
            })
        }
        TarjetaPanel {
            Text("Estilo", style = MaterialTheme.typography.titleMedium)
            Text(
                "Todos los componentes de la app en una sola pantalla. Sirve para comprobar el modo claro y el oscuro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            BotonSecundario("Ver galería de estilo", onClick = onGaleria)
        }
    }
}
