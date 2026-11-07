package com.cdiacal.examind.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.examind.estilo.BotonPrincipal
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.colorAviso
import com.cdiacal.examind.estilo.colorGasto
import com.cdiacal.examind.nucleo.BusquedaWeb
import com.cdiacal.examind.nucleo.Formato
import com.cdiacal.examind.nucleo.ResultadoWeb

/**
 * Buscar exámenes online del tema: busca, enseña los resultados con casillas (vista previa: título, sitio, si es PDF
 * y si ya se usó), deja añadir un enlace a mano y lanza en segundo plano sacar las preguntas de los marcados.
 */
@Composable
fun TarjetaBuscarOnline(vm: TemarioViewModel, temaGuardado: String, ocupado: Boolean) {
    val busqueda by vm.busqueda.collectAsStateWithLifecycle()
    var tema by rememberSaveable(temaGuardado) { mutableStateOf(temaGuardado) }
    var enlace by rememberSaveable { mutableStateOf("") }
    var errorEnlace by rememberSaveable { mutableStateOf<String?>(null) }
    val gris = MaterialTheme.colorScheme.onSurfaceVariant

    TarjetaPanel {
        Text("Buscar exámenes online", style = MaterialTheme.typography.titleMedium)
        Text(
            "Busca exámenes y preguntas tipo test del tema en internet. Cada pregunta guarda su fuente (página y enlace).",
            style = MaterialTheme.typography.bodySmall,
            color = gris
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = tema,
            onValueChange = { tema = it },
            label = { Text("Tema que buscar") },
            supportingText = { Text("Mejor con el nombre de la asignatura o del examen.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        BotonPrincipal(
            if (busqueda is EstadoBusqueda.Buscando) "Buscando…" else "Buscar",
            habilitado = tema.isNotBlank() && busqueda !is EstadoBusqueda.Buscando
        ) { vm.buscar(tema) }

        when (val b = busqueda) {
            EstadoBusqueda.Buscando -> {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            is EstadoBusqueda.Error -> {
                Spacer(Modifier.height(8.dp))
                Text(b.mensaje, style = MaterialTheme.typography.bodySmall, color = colorGasto())
            }
            is EstadoBusqueda.Resultados -> {
                Spacer(Modifier.height(12.dp))
                Text(
                    "${Formato.cantidad(b.lista.size, "resultado")}. Marca de cuáles sacar preguntas; la IA lee cada página (unos minutos por página).",
                    style = MaterialTheme.typography.bodySmall,
                    color = gris
                )
                Spacer(Modifier.height(4.dp))
                b.lista.forEach { r ->
                    FilaResultado(r, r.url in b.seleccion, BusquedaWeb.normalizarUrl(r.url) in b.usadas) { vm.alternarResultado(r.url) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonPrincipal(
                        if (b.seleccion.isEmpty()) "Sacar preguntas" else "Sacar preguntas de ${b.seleccion.size}",
                        Modifier.weight(1f),
                        habilitado = b.seleccion.isNotEmpty()
                    ) { vm.sacarPreguntasOnline(tema) }
                    TextButton(onClick = vm::cerrarBusqueda) { Text("Cerrar") }
                }
                if (ocupado) {
                    Text(
                        "La IA está ocupada con otra tarea: esta esperará en cola.",
                        style = MaterialTheme.typography.bodySmall,
                        color = gris
                    )
                }
                Text(
                    "Los resultados vienen de DuckDuckGo o Mojeek. Comprueba las respuestas: un examen de otra universidad puede usar otro temario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = gris
                )
            }
            EstadoBusqueda.Inactivo -> {}
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("¿Ya tienes un examen localizado?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = enlace,
            onValueChange = { enlace = it; errorEnlace = null },
            label = { Text("Pega el enlace (web o PDF)") },
            singleLine = true,
            isError = errorEnlace != null,
            supportingText = errorEnlace?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        BotonSecundario("Añadir a la lista", habilitado = enlace.isNotBlank()) {
            errorEnlace = vm.anadirEnlace(enlace)
            if (errorEnlace == null) enlace = ""
        }
    }
}

@Composable
private fun FilaResultado(r: ResultadoWeb, marcado: Boolean, usado: Boolean, onAlternar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onAlternar).padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(checked = marcado, onCheckedChange = { onAlternar() })
        Column(Modifier.weight(1f).padding(top = 10.dp)) {
            Text(r.titulo.ifBlank { r.dominio }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                r.dominio + (if (r.esPdf) " · PDF" else "") ,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (usado) Text("Ya usada en este temario", style = MaterialTheme.typography.bodySmall, color = colorAviso())
            if (r.resumen.isNotBlank()) {
                Text(r.resumen, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
