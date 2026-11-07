package com.cdiacal.examind.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.examind.estilo.BotonPrincipal
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.FilaPestanas
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.colorAviso
import com.cdiacal.examind.estilo.colorGasto
import com.cdiacal.examind.estilo.colorIngreso
import com.cdiacal.examind.ia.EleccionMotor
import com.cdiacal.examind.ia.EstadoModelo
import com.cdiacal.examind.ia.EstadoNano
import com.cdiacal.examind.nucleo.CatalogoModelos
import com.cdiacal.examind.nucleo.ModeloDescargable
import java.util.Locale

private fun gb(bytes: Long) = String.format(Locale("es", "ES"), "%.1f GB", bytes / 1e9)
private fun porcentaje(bytes: Long, total: Long) = if (total > 0) (bytes * 100 / total).toInt() else 0

/** Ajustes → IA del móvil: qué hay en este móvil, descargar o importar modelos, elegir cuál usar y probarlo. */
@Composable
fun TarjetaIA(vm: IAViewModel) {
    val e by vm.estado.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        vm.vigilar()
        onDispose { vm.dejarDeVigilar() }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importar(uri)
    }
    val gris = MaterialTheme.colorScheme.onSurfaceVariant

    TarjetaPanel {
        Text("IA del móvil", style = MaterialTheme.typography.titleMedium)
        Text(
            "Crea las preguntas en el propio móvil, sin enviar tu temario a ningún servidor. Tu móvil tiene unos ${e.ramGb} GB de RAM.",
            style = MaterialTheme.typography.bodySmall,
            color = gris
        )
        Spacer(Modifier.height(12.dp))

        // ── Gemini Nano
        Text("Gemini Nano (del sistema)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        when (val nano = e.nano) {
            null -> Text("Comprobando…", style = MaterialTheme.typography.bodySmall, color = gris)
            EstadoNano.Disponible -> Text("✓ Disponible en este móvil. No ocupa espacio de la app.", style = MaterialTheme.typography.bodySmall, color = colorIngreso())
            EstadoNano.Descargable -> {
                Text("Este móvil lo puede descargar (lo gestiona Android).", style = MaterialTheme.typography.bodySmall, color = gris)
                Spacer(Modifier.height(8.dp))
                BotonSecundario("Descargar Gemini Nano", onClick = vm::descargarNano)
            }
            is EstadoNano.Descargando -> Progreso(nano.bytes, nano.total, "Descargando Gemini Nano")
            is EstadoNano.NoDisponible -> Text(nano.motivo, style = MaterialTheme.typography.bodySmall, color = gris)
        }

        CatalogoModelos.TODOS.forEach { m ->
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            FilaModelo(vm, m, e.modelos[m.id], recomendado = e.recomendado == m, ramGb = e.ramGb)
        }

        Spacer(Modifier.height(12.dp))
        BotonSecundario("Ya lo tengo descargado", onClick = { importar.launch(arrayOf("*/*")) })
        Text(
            "Si lo descargaste desde el navegador, elige el archivo .litertlm.",
            style = MaterialTheme.typography.bodySmall,
            color = gris
        )
        e.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = colorGasto())
        }

        // ── Cuál usar
        val opciones = buildList {
            if (e.nano == EstadoNano.Disponible) add(EleccionMotor.GeminiNano)
            CatalogoModelos.TODOS.filter { e.modelos[it.id] == EstadoModelo.Descargado }.forEach { add(EleccionMotor.Modelo(it)) }
        }
        if (opciones.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Usar para crear preguntas", style = MaterialTheme.typography.labelMedium, color = gris)
            Spacer(Modifier.height(8.dp))
            FilaPestanas(
                opciones.map { o -> if (o is EleccionMotor.Modelo) o.modelo.nombre.substringBefore(" (") else "Gemini Nano" },
                opciones.indexOf(e.eleccion).coerceAtLeast(0),
                { vm.elegir(opciones[it]) }
            )
            Spacer(Modifier.height(12.dp))
            BotonPrincipal(if (e.probando) "Probando…" else "Probar la IA", habilitado = !e.probando, onClick = vm::probar)
            e.resultadoPrueba?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        } else if (e.nano != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Para crear preguntas descarga uno de los modelos${e.recomendado?.let { " (te recomendamos el ${it.nombre.substringAfter("(").removeSuffix(")")})" } ?: ""}.",
                style = MaterialTheme.typography.bodySmall,
                color = colorAviso()
            )
        }
    }
}

@Composable
private fun FilaModelo(vm: IAViewModel, m: ModeloDescargable, estado: EstadoModelo?, recomendado: Boolean, ramGb: Int) {
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(m.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        if (recomendado) Text("Recomendado", style = MaterialTheme.typography.labelMedium, color = colorIngreso())
    }
    Text("${m.descripcion} Ocupa ${gb(m.bytes)}.", style = MaterialTheme.typography.bodySmall, color = gris)
    if (ramGb < m.ramMinimaGb) {
        Text(
            "Tu móvil tiene menos RAM de la que pide (${m.ramMinimaGb} GB): puede ir muy lento o cerrarse.",
            style = MaterialTheme.typography.bodySmall,
            color = colorAviso()
        )
    }
    Spacer(Modifier.height(8.dp))
    when (estado) {
        null, EstadoModelo.NoDescargado -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonSecundario("Descargar con wifi", onClick = { vm.descargar(m, soloWifi = true) })
            BotonSecundario("Con datos", onClick = { vm.descargar(m, soloWifi = false) })
        }
        is EstadoModelo.Descargando -> {
            Progreso(estado.bytes, estado.total, "Descargando")
            Spacer(Modifier.height(8.dp))
            BotonSecundario("Cancelar", onClick = { vm.cancelar(m) })
        }
        is EstadoModelo.EsperandoWifi -> {
            Progreso(estado.bytes, estado.total, "Esperando wifi")
            Text("Seguirá sola cuando te conectes a una wifi.", style = MaterialTheme.typography.bodySmall, color = gris)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonSecundario("Seguir con datos", onClick = { vm.descargar(m, soloWifi = false) })
                BotonSecundario("Cancelar", onClick = { vm.cancelar(m) })
            }
        }
        EstadoModelo.Descargado -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✓ Descargado", style = MaterialTheme.typography.bodySmall, color = colorIngreso(), modifier = Modifier.weight(1f))
            BotonSecundario("Eliminar", onClick = { vm.borrar(m) })
        }
        is EstadoModelo.Fallo -> {
            Text(estado.mensaje, style = MaterialTheme.typography.bodySmall, color = colorGasto())
            Spacer(Modifier.height(8.dp))
            BotonSecundario("Volver a intentarlo", onClick = { vm.descargar(m, soloWifi = true) })
        }
    }
}

@Composable
private fun Progreso(bytes: Long, total: Long, texto: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            if (total > 0) "$texto: ${porcentaje(bytes, total)} % · ${gb(bytes)} de ${gb(total)}" else "$texto…",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(4.dp))
        val barra = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        if (total > 0) LinearProgressIndicator(bytes.toFloat() / total, barra, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        else LinearProgressIndicator(barra, trackColor = MaterialTheme.colorScheme.surfaceVariant)
    }
}
