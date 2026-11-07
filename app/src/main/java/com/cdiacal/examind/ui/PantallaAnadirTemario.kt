package com.cdiacal.examind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cdiacal.examind.estilo.BotonPrincipal
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.CabeceraSimple
import com.cdiacal.examind.estilo.TarjetaDestacada
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.colorAviso
import com.cdiacal.examind.estilo.colorGasto
import com.cdiacal.examind.nucleo.Formato

/**
 * Añadir un temario: lee el PDF elegido, enseña una vista previa (páginas, texto detectado, nombre) y lo guarda.
 * [onElegirOtro] vuelve a abrir el selector de archivos; [onSalir] vuelve sin guardar.
 */
@Composable
fun PantallaAnadirTemario(
    vm: ImportarViewModel,
    onElegirOtro: () -> Unit,
    onGuardado: (TemarioGuardado) -> Unit,
    onSalir: () -> Unit
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    BackHandler { vm.cancelar(); onSalir() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CabeceraSimple(titulo = "Añadir temario", subtitulo = "Desde un PDF o un Word (.docx)", onVolver = { vm.cancelar(); onSalir() })

        when (val e = estado) {
            EstadoImportar.Inactivo -> TarjetaPanel {
                Text("Elige el temario en PDF o en Word (.docx) para empezar.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                BotonPrincipal("Elegir archivo", onClick = onElegirOtro)
            }

            is EstadoImportar.Leyendo -> TarjetaPanel {
                Text(
                    if (e.total == 0) "Leyendo el documento…" else "Leyendo la página ${e.pagina} de ${e.total}…",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                val barra = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                if (e.total == 0) LinearProgressIndicator(barra, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                else LinearProgressIndicator(e.pagina.toFloat() / e.total, barra, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }

            is EstadoImportar.Error -> TarjetaPanel {
                Text(e.mensaje, style = MaterialTheme.typography.bodyMedium, color = colorGasto())
                Spacer(Modifier.height(12.dp))
                BotonPrincipal("Elegir otro archivo", onClick = onElegirOtro)
            }

            is EstadoImportar.VistaPrevia -> VistaPrevia(vm, e, onElegirOtro, onGuardado)
        }
    }
}

@Composable
private fun VistaPrevia(
    vm: ImportarViewModel,
    e: EstadoImportar.VistaPrevia,
    onElegirOtro: () -> Unit,
    onGuardado: (TemarioGuardado) -> Unit
) {
    var nombre by rememberSaveable(e.archivo) { mutableStateOf(e.nombre) }
    var tema by rememberSaveable(e.archivo) { mutableStateOf(e.nombre) }
    val caracteres = e.fragmentos.sumOf { it.texto.length }

    TarjetaDestacada(
        titulo = if (e.paginas > 0) "Páginas del ${e.tipo}" else "Documento de ${e.tipo}",
        cifra = if (e.paginas > 0) Formato.miles(e.paginas.toLong()) else Formato.cantidad(e.fragmentos.size, "fragmento"),
        detalle = if (e.conTexto) "${Formato.cantidad(e.fragmentos.size, "fragmento")} de texto para crear preguntas · ${Formato.miles(caracteres.toLong())} caracteres"
        else "Sin texto que se pueda leer",
        colorDetalle = if (e.conTexto) MaterialTheme.colorScheme.onPrimaryContainer else colorAviso()
    )

    if (!e.conTexto) {
        TarjetaPanel {
            Text(
                if (e.tipo == "PDF") "⚠ Este PDF parece escaneado" else "⚠ Este documento casi no tiene texto",
                style = MaterialTheme.typography.titleMedium,
                color = colorAviso()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (e.tipo == "PDF") "Sus páginas son imágenes y no tienen texto que se pueda leer. Prueba con un PDF en el que se pueda seleccionar el texto (por ejemplo, exportado desde Word o Google Docs)."
                else "No hay texto suficiente para crear preguntas. Si el contenido está en imágenes, no se puede leer.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            BotonPrincipal("Elegir otro archivo", onClick = { vm.cancelar(); onElegirOtro() })
        }
        return
    }

    TarjetaPanel {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del temario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = tema,
            onValueChange = { tema = it },
            label = { Text("Tema para buscar exámenes online") },
            supportingText = { Text("Por ejemplo: «Biología celular, primero de Biología».") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (e.paginas == 0) {
        Text(
            "Este Word no guarda dónde empieza cada página, así que las preguntas no llevarán número de página.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    TarjetaPanel {
        Text("Así se lee el texto", style = MaterialTheme.typography.titleMedium)
        Text(
            (if (e.fragmentos.first().pagina > 0) "Primer fragmento, página ${e.fragmentos.first().pagina}." else "Primer fragmento.") +
                " Si ves letras raras, el documento puede tener un formato que no se lee bien.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(e.fragmentos.first().texto.take(600) + if (e.fragmentos.first().texto.length > 600) "…" else "", style = MaterialTheme.typography.bodySmall)
    }

    BotonPrincipal("Guardar temario", Modifier.fillMaxWidth(), habilitado = nombre.isNotBlank()) {
        vm.guardar(nombre, tema, onGuardado)
    }
    BotonSecundario("Elegir otro archivo", Modifier.fillMaxWidth(), onClick = { vm.cancelar(); onElegirOtro() })
    Text(
        "Después podrás crear las preguntas con la IA del móvil desde el temario.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
