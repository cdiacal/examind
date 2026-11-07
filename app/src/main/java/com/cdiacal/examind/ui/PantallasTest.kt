package com.cdiacal.examind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.examind.estilo.BotonPrincipal
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.CabeceraHero
import com.cdiacal.examind.estilo.CabeceraSimple
import com.cdiacal.examind.estilo.CifraHero
import com.cdiacal.examind.estilo.DialogoTarjeta
import com.cdiacal.examind.estilo.EstadoOpcion
import com.cdiacal.examind.estilo.EstadoVacio
import com.cdiacal.examind.estilo.FilaPestanas
import com.cdiacal.examind.estilo.MiniDatoHero
import com.cdiacal.examind.estilo.SegmentButton
import com.cdiacal.examind.estilo.TarjetaOpcion
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.TituloHero
import com.cdiacal.examind.estilo.colorAviso
import com.cdiacal.examind.estilo.colorGasto
import com.cdiacal.examind.nucleo.Calificacion
import com.cdiacal.examind.nucleo.Correccion
import com.cdiacal.examind.nucleo.Formato
import com.cdiacal.examind.nucleo.Origen
import com.cdiacal.examind.nucleo.OrigenTest
import com.cdiacal.examind.nucleo.PreguntaEnTest
import com.cdiacal.examind.nucleo.SesionTest

/** Número de preguntas que se ofrecen; «Todas» se guarda como un número muy alto. */
private val NUMEROS = listOf(10, 20, 30, TODAS)
private const val TODAS = 9_999

/** Todo el flujo de un test: configurar, contestar y ver el resultado. */
@Composable
fun FlujoTest(vm: TestViewModel, onSalir: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoTest.Configurando -> PantallaConfigTest(vm, onSalir)
        is EstadoTest.EnCurso -> PantallaTest(vm, e.sesion)
        is EstadoTest.Terminado -> PantallaResultado(vm, e, onSalir)
    }
}

// ════════ CONFIGURAR ════════

@Composable
private fun PantallaConfigTest(vm: TestViewModel, onSalir: () -> Unit) {
    val temarios by vm.temarios.collectAsStateWithLifecycle()
    val ultima by vm.ultima.collectAsStateWithLifecycle()
    val candidatas by vm.candidatas.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    BackHandler(onBack = onSalir)

    val lista = temarios
    LaunchedEffect(lista) { if (lista != null) vm.preparar(lista) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val temario = lista?.firstOrNull { it.id == ultima.temarioId }
        CabeceraSimple(titulo = "Nuevo test", subtitulo = temario?.nombre, onVolver = onSalir)
        if (lista == null) return@Column
        if (lista.isEmpty() || temario == null) {
            TarjetaPanel { EstadoVacio("Aún no tienes temarios.\nVuelve al inicio y toca + para añadir uno.") }
            return@Column
        }

        val config = ultima.config
        if (lista.size > 1) {
            TarjetaPanel {
                Etiqueta("Temario")
                FilaPestanas(lista.map { it.nombre }, lista.indexOf(temario), { vm.elegirTemario(lista[it].id) })
            }
        }

        TarjetaPanel {
            Etiqueta("De dónde salen las preguntas")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(OrigenTest.TEMARIO to "Temario", OrigenTest.ONLINE to "Online", OrigenTest.MEZCLA to "Mezcla").forEach { (origen, texto) ->
                    val n = contarDisponibles(candidatas, origen, false)
                    SegmentButton("$texto ($n)", config.origen == origen, Modifier.weight(1f)) {
                        vm.cambiarConfig(config.copy(origen = origen)); vm.olvidarError()
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Solo las que fallaste", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Las que fallaste o dejaste en blanco la última vez: ${contarDisponibles(candidatas, config.origen, true)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(config.soloFalladas, { vm.cambiarConfig(config.copy(soloFalladas = it)); vm.olvidarError() })
            }
        }

        val disponibles = contarDisponibles(candidatas, config.origen, config.soloFalladas)
        TarjetaPanel {
            Etiqueta("Número de preguntas")
            FilaPestanas(
                NUMEROS.map { if (it == TODAS) "Todas" else it.toString() },
                NUMEROS.indexOf(config.numPreguntas).coerceAtLeast(0),
                { vm.cambiarConfig(config.copy(numPreguntas = NUMEROS[it])) }
            )
            if (disponibles < config.numPreguntas && config.numPreguntas != TODAS) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Solo hay ${Formato.cantidad(disponibles, "pregunta")} con estas opciones: el test tendrá esas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TarjetaPanel {
            Etiqueta("Corrección")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentButton("Al momento", ultima.correccion == Correccion.AL_MOMENTO, Modifier.weight(1f)) { vm.cambiarCorreccion(Correccion.AL_MOMENTO) }
                SegmentButton("Al final", ultima.correccion == Correccion.AL_FINAL, Modifier.weight(1f)) { vm.cambiarCorreccion(Correccion.AL_FINAL) }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (ultima.correccion == Correccion.AL_MOMENTO) "Ves si aciertas al contestar cada pregunta, con su explicación."
                else "Como en un examen: puedes cambiar respuestas y ves la nota al terminar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colorGasto()) }
        val numFinal = minOf(disponibles, config.numPreguntas)
        BotonPrincipal(
            if (numFinal > 0) "Empezar · ${Formato.cantidad(numFinal, "pregunta")}" else "Empezar",
            modifier = Modifier.fillMaxWidth(),
            habilitado = numFinal > 0,
            onClick = vm::empezar
        )
        if (numFinal == 0) {
            Text(
                if (config.soloFalladas) "No tienes preguntas falladas con estas opciones. Quita «Solo las que fallaste» o elige otro origen."
                else "No hay preguntas con este origen. Elige otro o añade preguntas al temario.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
}

// ════════ CONTESTAR ════════

@Composable
private fun PantallaTest(vm: TestViewModel, sesion: SesionTest) {
    var preguntarSalir by rememberSaveable { mutableStateOf(false) }
    var preguntarTerminar by rememberSaveable { mutableStateOf(false) }
    BackHandler { preguntarSalir = true }

    val p = sesion.pregunta
    val corregida = sesion.corregida()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            CabeceraSimple(
                titulo = "Pregunta ${sesion.actual + 1} de ${sesion.total}",
                subtitulo = if (sesion.correccion == Correccion.AL_MOMENTO) "Llevas ${Formato.cantidad(sesion.aciertosHastaAhora, "acierto")}"
                else "Contestadas: ${sesion.respondidas} de ${sesion.total}",
                onVolver = { preguntarSalir = true }
            ) {
                TextButton(onClick = { if (sesion.respondidas < sesion.total && sesion.correccion == Correccion.AL_FINAL) preguntarTerminar = true else vm.terminar() }) {
                    Text("Terminar", color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = (sesion.actual + 1f) / sesion.total,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(p.enunciado, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            p.opciones.forEachIndexed { i, texto ->
                val elegida = sesion.elegidaActual == i
                val estadoOpcion = when {
                    !corregida -> if (elegida) EstadoOpcion.ELEGIDA else EstadoOpcion.NORMAL
                    i == p.correcta -> EstadoOpcion.CORRECTA
                    elegida -> EstadoOpcion.INCORRECTA
                    else -> EstadoOpcion.ATENUADA
                }
                TarjetaOpcion('A' + i, texto, estadoOpcion, onClick = if (corregida) null else ({ vm.responder(i) }))
            }
            if (corregida) {
                Spacer(Modifier.height(6.dp))
                TarjetaPanel { Explicacion(p) }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sesion.correccion == Correccion.AL_FINAL) {
                BotonSecundario("Anterior", Modifier.weight(1f), habilitado = !sesion.esPrimera, onClick = vm::anterior)
            }
            val sinContestar = sesion.elegidaActual == null
            val texto = when {
                sesion.esUltima -> "Terminar"
                sinContestar && sesion.correccion == Correccion.AL_MOMENTO -> "Dejar en blanco"
                else -> "Siguiente"
            }
            BotonPrincipal(texto, Modifier.weight(1f)) {
                when {
                    !sesion.esUltima -> vm.siguiente()
                    sesion.correccion == Correccion.AL_FINAL && sesion.respondidas < sesion.total -> preguntarTerminar = true
                    else -> vm.terminar()
                }
            }
        }
    }

    if (preguntarSalir) {
        DialogoTarjeta(
            titulo = "¿Salir del test?",
            onCerrar = { preguntarSalir = false },
            textoConfirmar = "Salir",
            onConfirmar = { preguntarSalir = false; vm.abandonar() },
            textoCancelar = "Seguir"
        ) {
            Text("Este test no se guardará.", style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (preguntarTerminar) {
        val quedan = sesion.total - sesion.respondidas
        DialogoTarjeta(
            titulo = "¿Terminar el test?",
            onCerrar = { preguntarTerminar = false },
            textoConfirmar = "Terminar",
            onConfirmar = { preguntarTerminar = false; vm.terminar() },
            textoCancelar = "Seguir"
        ) {
            Text(
                "Te ${if (quedan == 1) "queda 1 pregunta" else "quedan $quedan preguntas"} sin contestar. Contarán como en blanco.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** La explicación de la respuesta y de dónde sale la pregunta. */
@Composable
private fun Explicacion(p: PreguntaEnTest) {
    if (p.explicacion.isNotBlank()) {
        Text(p.explicacion, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
    }
    LineaFuente(p)
}

/** «Fuente: tu temario, página 3» o el título del examen online con su enlace. */
@Composable
fun LineaFuente(p: PreguntaEnTest) {
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    val fuente = p.fuente
    if (p.origen == Origen.ONLINE && fuente != null) {
        val uri = LocalUriHandler.current
        Text("Fuente: ${fuente.titulo}", style = MaterialTheme.typography.bodySmall, color = gris)
        Text(
            fuente.url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clickable { runCatching { uri.openUri(fuente.url) } }
                .padding(vertical = 6.dp)
        )
        Text(
            "Consultada el ${Formato.fechaCorta(fuente.consultadaEl, System.currentTimeMillis())}. Comprueba la respuesta si el examen es antiguo.",
            style = MaterialTheme.typography.bodySmall,
            color = gris
        )
    } else {
        Text(
            if (p.pagina != null) "Fuente: tu temario, página ${p.pagina}" else "Fuente: tu temario",
            style = MaterialTheme.typography.bodySmall,
            color = gris
        )
    }
}

// ════════ RESULTADO ════════

@Composable
private fun PantallaResultado(vm: TestViewModel, e: EstadoTest.Terminado, onSalir: () -> Unit) {
    BackHandler(onBack = onSalir)
    val r = e.fin.resultado
    val calificacion = Calificacion.de(r.nota)
    val falladas = r.fallos + r.enBlanco

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CabeceraHero(onVolver = onSalir) {
            TituloHero("Resultado", Formato.duracion(e.fin.duracionSegundos))
            Spacer(Modifier.height(14.dp))
            CifraHero("Nota", "${Formato.nota(r.nota)} · ${calificacion.texto}")
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                MiniDatoHero("Aciertos", "+${r.aciertos}", Modifier.weight(1f))
                MiniDatoHero("Fallos", if (r.fallos > 0) "−${r.fallos}" else "0", Modifier.weight(1f))
                MiniDatoHero("En blanco", r.enBlanco.toString(), Modifier.weight(1f))
                MiniDatoHero("Porcentaje", Formato.porcentaje(r.proporcionAciertos), Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (falladas > 0) {
                BotonPrincipal("Repetir las que fallaste · $falladas", Modifier.fillMaxWidth(), onClick = vm::repetirFalladas)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonSecundario("Nuevo test", Modifier.weight(1f), onClick = vm::abandonar)
                BotonSecundario("Volver al inicio", Modifier.weight(1f), onClick = onSalir)
            }

            Text("Revisión", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            e.sesion.preguntas.forEachIndexed { i, p ->
                TarjetaPanel { Revision(i, p, e.sesion.elegidas[i]) }
            }
            Text(
                "El test se ha guardado en tu historial.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Revision(indice: Int, p: PreguntaEnTest, elegida: Int?) {
    Text("${indice + 1}. ${p.enunciado}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    if (elegida == null) {
        Text("— En blanco", style = MaterialTheme.typography.labelMedium, color = colorAviso(), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
    } else if (elegida != p.correcta) {
        TarjetaOpcion('A' + elegida, p.opciones[elegida], EstadoOpcion.INCORRECTA)
        Spacer(Modifier.height(8.dp))
    }
    TarjetaOpcion('A' + p.correcta, p.opciones[p.correcta], EstadoOpcion.CORRECTA)
    Spacer(Modifier.height(10.dp))
    Row { Spacer(Modifier.width(2.dp)); Column { Explicacion(p) } }
}
