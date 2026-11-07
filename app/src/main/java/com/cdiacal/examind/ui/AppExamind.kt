package com.cdiacal.examind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cdiacal.examind.estilo.BotonSecundario
import com.cdiacal.examind.estilo.CabeceraHero
import com.cdiacal.examind.estilo.CabeceraSimple
import com.cdiacal.examind.estilo.CifraHero
import com.cdiacal.examind.estilo.EstadoVacio
import com.cdiacal.examind.estilo.FilaPestanas
import com.cdiacal.examind.estilo.GaleriaEstilo
import com.cdiacal.examind.estilo.MiniDatoHero
import com.cdiacal.examind.estilo.ModoTema
import com.cdiacal.examind.estilo.TarjetaPanel
import com.cdiacal.examind.estilo.TemaState
import com.cdiacal.examind.estilo.TituloHero

/** Pantallas de la app. De momento solo el esqueleto: inicio, ajustes y la galería de estilo. */
private enum class Pantalla { INICIO, AJUSTES, GALERIA }

@Composable
fun AppExamind() {
    var pantalla by rememberSaveable { mutableStateOf(Pantalla.INICIO) }
    BackHandler(enabled = pantalla != Pantalla.INICIO) {
        pantalla = if (pantalla == Pantalla.GALERIA) Pantalla.AJUSTES else Pantalla.INICIO
    }
    when (pantalla) {
        Pantalla.INICIO -> PantallaInicio(onAjustes = { pantalla = Pantalla.AJUSTES })
        Pantalla.AJUSTES -> PantallaAjustes(
            onVolver = { pantalla = Pantalla.INICIO },
            onGaleria = { pantalla = Pantalla.GALERIA }
        )
        Pantalla.GALERIA -> Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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

@Composable
private fun PantallaInicio(onAjustes: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
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
            CifraHero("Aciertos medios", "—")
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                MiniDatoHero("Temarios", "0", Modifier.weight(1f))
                MiniDatoHero("Preguntas", "0", Modifier.weight(1f))
                MiniDatoHero("Tests hechos", "0", Modifier.weight(1f))
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TarjetaPanel {
                Text("Tus temarios", style = MaterialTheme.typography.titleMedium)
                EstadoVacio("Aún no tienes temarios.\nAñade un PDF para crear tus primeras preguntas.")
            }
            Text(
                "Esta versión es solo el esqueleto: tema y estilo. Añadir temarios llega en el siguiente paso.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PantallaAjustes(onVolver: () -> Unit, onGaleria: () -> Unit) {
    val context = LocalContext.current
    val modos = listOf(ModoTema.SISTEMA, ModoTema.CLARO, ModoTema.OSCURO)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
