package com.cdiacal.examind.estilo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de muestra con TODOS los componentes del estilo, para ver el conjunto de un vistazo en la vista previa
 * de Android Studio (claro y oscuro) o dentro de una app de ejemplo. Sirve también de chuleta: cada bloque es
 * la forma de usar ese componente.
 */
@Composable
fun GaleriaEstilo(modifier: Modifier = Modifier) {
    var pestaña by remember { mutableIntStateOf(0) }
    var tipo by remember { mutableIntStateOf(0) }
    var importe by remember { mutableStateOf("") }
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Cabecera de la pantalla de inicio: degradado azul marino, dato protagonista y tres datos pequeños
        CabeceraHero(mostrarBarraSuperior = true) {
            TituloHero("Resumen", "octubre 2026")
            Spacer(Modifier.height(14.dp))
            CifraHero("Saldo total", "1.234,56 €")
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                MiniDatoHero("Ingresos", "+1.800 €", Modifier.weight(1f))
                MiniDatoHero("Gastos", "-950 €", Modifier.weight(1f))
                MiniDatoHero("Ahorro", "850 €", Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Seccion("Cabecera de pantalla secundaria")
            CabeceraSimple(titulo = "Inversiones", subtitulo = "Acciones, fondos y cripto", onVolver = {})

            Seccion("Tarjeta destacada")
            TarjetaDestacada(titulo = "Valor de la cartera", cifra = "12.480,00 €", detalle = "+1.230,00 € (+10,9 %) sin realizar", colorDetalle = colorIngreso())

            Seccion("Pestañas y selector")
            FilaPestanas(listOf("Hoy", "Ayer", "Lun 19", "📅 Otro"), pestaña, { pestaña = it })
            Row {
                SegmentButton("Gasto", tipo == 0, Modifier.weight(1f)) { tipo = 0 }
                Spacer(Modifier.width(8.dp))
                SegmentButton("Ingreso", tipo == 1, Modifier.weight(1f)) { tipo = 1 }
            }

            Seccion("Botones")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                BotonPrincipal("Guardar", onClick = { mostrarDialogo = true })
                BotonSecundario("Cancelar", onClick = {})
                BotonPrincipal("Desactivado", habilitado = false, onClick = {})
            }

            Seccion("Panel con lista")
            TarjetaPanel {
                Text("Movimientos", style = MaterialTheme.typography.titleMedium)
                FilaLista("Mercadona", "-30,80 €", colorGasto(), subtitulo = "Comida", icono = { IconoCategoria("🛒", 1) })
                FilaLista("Nómina", "+1.800,00 €", colorIngreso(), subtitulo = "Ingresos", icono = { IconoCategoria("💼", 2) })
                FilaLista("Cuenta corriente", "250,00 €", MaterialTheme.colorScheme.onSurface, icono = { IconoInicial("Cuenta corriente", 3) })
            }

            Seccion("Barras de progreso (verde, ámbar, rojo)")
            TarjetaPanel {
                BarraProgresoSemantica(0.4f, "🍔 Comida", "120 € / 300 €")
                Spacer(Modifier.height(12.dp))
                BarraProgresoSemantica(0.86f, "🎬 Ocio", "86 € / 100 €")
                Spacer(Modifier.height(12.dp))
                BarraProgresoSemantica(1.2f, "🚗 Transporte", "120 € / 100 €", mensajeExceso = "Te has pasado 20 €")
            }

            Seccion("Colores de estado y paleta de iconos")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Muestra("Ingreso", colorIngreso())
                Muestra("Gasto", colorGasto())
                Muestra("Aviso", colorAviso())
                Muestra("Acento", MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0L..7L).forEach { IconoCategoria("★", it, tamano = 34.dp) }
            }

            Seccion("Teclado numérico")
            Text(
                importeParaMostrar(importe) + " €",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = colorGasto(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            TecladoNumerico(
                onTecla = { importe = teclear(importe, it) },
                onBorrarTodo = { importe = "" },
                habilitado = importe.isNotEmpty(),
                onAccion = {}
            )

            Seccion("Opciones de un test (sin corregir y corregidas)")
            TarjetaOpcion('A', "Mitocondria", EstadoOpcion.ELEGIDA, onClick = {})
            TarjetaOpcion('B', "Ribosoma", EstadoOpcion.NORMAL, onClick = {})
            TarjetaOpcion('A', "Mitocondria", EstadoOpcion.CORRECTA)
            TarjetaOpcion('B', "Ribosoma", EstadoOpcion.INCORRECTA)
            TarjetaOpcion('C', "Lisosoma", EstadoOpcion.ATENUADA)

            Seccion("Estado vacío")
            TarjetaPanel { EstadoVacio("Aún no hay movimientos este mes.\nToca el botón + para apuntar el primero.") }

            Seccion("Barra de selección")
            BarraSeleccion(cantidad = 3, detalle = "Suman -84,30 €", onTodos = {}, onEliminar = {}, onCancelar = {})
        }
    }

    if (mostrarDialogo) {
        DialogoTarjeta(
            titulo = "Diálogo estándar",
            onCerrar = { mostrarDialogo = false },
            textoConfirmar = "Aceptar",
            onConfirmar = { mostrarDialogo = false }
        ) {
            Text("Tarjeta de 20 dp, título grande, contenido y los dos botones abajo a la derecha.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Seccion(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Muestra(nombre: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color))
        Spacer(Modifier.height(4.dp))
        Text(nombre, fontSize = 10.5f.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Claro", showBackground = true, heightDp = 2200)
@Composable
private fun GaleriaClaraPreview() {
    EstiloTheme(modo = ModoTema.CLARO) { GaleriaEstilo() }
}

@Preview(name = "Oscuro", showBackground = true, heightDp = 2200)
@Composable
private fun GaleriaOscuraPreview() {
    EstiloTheme(modo = ModoTema.OSCURO) { GaleriaEstilo() }
}
