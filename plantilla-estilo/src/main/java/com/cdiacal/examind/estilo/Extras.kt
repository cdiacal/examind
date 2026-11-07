package com.cdiacal.examind.estilo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Componentes del estilo de Ceca que están en pantallas concretas de la app y aquí se dejan sueltos y genéricos.

/** Botón de un selector de dos o tres opciones (Gasto / Ingreso): la elegida va rellena, las demás con borde. */
@Composable
fun SegmentButton(texto: String, seleccionado: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val relleno = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    if (seleccionado) {
        Button(onClick = onClick, modifier = modifier, contentPadding = relleno) {
            Text(texto, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = relleno) {
            Text(texto, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Tarjeta destacada con el dato protagonista: fondo del color de contenedor, esquinas de 20 dp, una cifra grande y,
 * debajo, un detalle (por ejemplo la variación, en verde o rojo). Admite contenido extra debajo.
 */
@Composable
fun TarjetaDestacada(
    titulo: String,
    cifra: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    colorDetalle: Color = Color.Unspecified,
    contenido: @Composable ColumnScope.() -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                cifra,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (detalle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    detalle,
                    color = if (colorDetalle == Color.Unspecified) MaterialTheme.colorScheme.onPrimaryContainer else colorDetalle,
                    fontWeight = FontWeight.SemiBold
                )
            }
            contenido()
        }
    }
}

/**
 * Una fila de lista: icono redondeado a la izquierda, título y subtítulo, y a la derecha el importe con su color
 * semántico; una línea fina debajo. Es el patrón de la lista de movimientos.
 */
@Composable
fun FilaLista(
    titulo: String,
    importe: String,
    colorImporte: Color,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    icono: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icono()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Text(importe, style = MaterialTheme.typography.bodyMedium, color = colorImporte)
    }
    HorizontalDivider()
}

/** Mensaje de lista vacía: centrado, en gris, diciendo qué hacer a continuación (no solo "no hay nada"). */
@Composable
fun EstadoVacio(texto: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Barra de progreso con color semántico: verde por debajo del 80 %, ámbar desde el 80 % y rojo al llegar al límite.
 * [progreso] es una proporción (1,0 = 100 %). Con [mensajeExceso] avisa al pasarse.
 */
@Composable
fun BarraProgresoSemantica(
    progreso: Float,
    izquierda: String,
    derecha: String,
    modifier: Modifier = Modifier,
    mensajeExceso: String? = null
) {
    val color = when {
        progreso >= 1f -> colorGasto()
        progreso >= 0.8f -> colorAviso()
        else -> colorIngreso()
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(izquierda, style = MaterialTheme.typography.bodyMedium)
            Text(derecha, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progreso.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        if (progreso >= 1f && mensajeExceso != null) {
            Spacer(Modifier.height(2.dp))
            Text(mensajeExceso, style = MaterialTheme.typography.bodySmall, color = colorGasto())
        }
    }
}

/** Diálogo estándar: tarjeta de 20 dp con título, contenido y, abajo a la derecha, Cancelar y la acción principal. */
@Composable
fun DialogoTarjeta(
    titulo: String,
    onCerrar: () -> Unit,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    habilitado: Boolean = true,
    textoCancelar: String = "Cancelar",
    contenido: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onCerrar) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                contenido()
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onCerrar) { Text(textoCancelar) }
                    Spacer(Modifier.width(8.dp))
                    BotonPrincipal(texto = textoConfirmar, habilitado = habilitado, onClick = onConfirmar)
                }
            }
        }
    }
}

// ════════ SELECCIÓN MÚLTIPLE: mantener pulsado para empezar ════════

/**
 * Fila de lista que se puede seleccionar: mantener pulsado empieza la selección; con la selección en marcha, tocar
 * marca o desmarca; sin ella, tocar abre el elemento. La fila seleccionada se tiñe del color de contenedor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.filaSeleccionable(
    seleccionada: Boolean,
    modoSeleccion: Boolean,
    onAbrir: () -> Unit,
    onAlternar: () -> Unit
): Modifier {
    val haptico = LocalHapticFeedback.current
    return this
        .background(if (seleccionada) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
        .combinedClickable(
            onClick = { if (modoSeleccion) onAlternar() else onAbrir() },
            onLongClick = {
                haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                onAlternar()
            }
        )
}

/** La casilla que aparece al principio de cada fila mientras hay una selección en marcha. */
@Composable
fun CasillaSeleccion(seleccionada: Boolean) {
    Checkbox(checked = seleccionada, onCheckedChange = null, modifier = Modifier.padding(end = 10.dp))
}

/** Barra inferior con lo seleccionado y sus acciones (todos, eliminar, cancelar). Sustituye al botón flotante. */
@Composable
fun BarraSeleccion(
    cantidad: Int,
    detalle: String,
    onTodos: () -> Unit,
    onEliminar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (cantidad == 1) "1 seleccionado" else "$cantidad seleccionados", fontWeight = FontWeight.SemiBold)
                if (detalle.isNotBlank()) {
                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            TextButton(onClick = onTodos) { Text("Todos") }
            TextButton(onClick = onEliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onCancelar) { Text("✕") }
        }
    }
}

// ════════ TECLADO NUMÉRICO PROPIO ════════

/** Una tecla: toque = pulsar con vibración suave; mantener = acción larga (por ejemplo, borrar todo). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tecla(etiqueta: String, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val haptico = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = {
                    haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let { accion ->
                    {
                        haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                        accion()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(etiqueta, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Teclado numérico en pantalla: dígitos, coma, "+" para sumar, borrar (mantener borra todo) y, en la última fila, el
 * botón de la acción principal (Guardar). Junto con [teclear] e [importeParaMostrar] escribe importes sin el teclado del móvil.
 */
@Composable
fun TecladoNumerico(
    onTecla: (Char) -> Unit,
    onBorrarTodo: () -> Unit,
    habilitado: Boolean,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier,
    textoAccion: String = "Guardar"
) {
    val filas = listOf(
        listOf('7', '8', '9', TECLA_BORRAR),
        listOf('4', '5', '6', '+'),
        listOf('1', '2', '3', ',')
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { tecla ->
                    Tecla(
                        etiqueta = if (tecla == TECLA_BORRAR) "⌫" else tecla.toString(),
                        modifier = Modifier.weight(1f),
                        onLongClick = if (tecla == TECLA_BORRAR) onBorrarTodo else null,
                        onClick = { onTecla(tecla) }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Tecla("0", Modifier.weight(1f), onClick = { onTecla('0') })
            BotonPrincipal(texto = textoAccion, habilitado = habilitado, modifier = Modifier.weight(3f).height(46.dp), onClick = onAccion)
        }
    }
}

// ════════ RESPUESTA DE UN TEST ════════

/** Cómo se pinta una opción de respuesta. */
enum class EstadoOpcion {
    /** Sin elegir. */
    NORMAL,
    /** Elegida, todavía sin corregir. */
    ELEGIDA,
    /** La buena, ya corregida. */
    CORRECTA,
    /** La elegida, y era mala. */
    INCORRECTA,
    /** Ni elegida ni buena, después de corregir. */
    ATENUADA
}

/**
 * Opción de respuesta de un test: letra en un icono redondeado, texto y, al corregir, la marca con texto
 * («✓ Correcta», «✗ Tu respuesta»): el color nunca es la única pista. Alto mínimo de 56 dp para tocarla bien.
 */
@Composable
fun TarjetaOpcion(
    letra: Char,
    texto: String,
    estado: EstadoOpcion,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val forma = RoundedCornerShape(12.dp)
    val colorEstado = when (estado) {
        EstadoOpcion.CORRECTA -> colorIngreso()
        EstadoOpcion.INCORRECTA -> colorGasto()
        EstadoOpcion.ELEGIDA -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val grosor = if (estado == EstadoOpcion.NORMAL || estado == EstadoOpcion.ATENUADA) 1.dp else 2.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .alpha(if (estado == EstadoOpcion.ATENUADA) 0.6f else 1f)
            .clip(forma)
            .background(
                if (estado == EstadoOpcion.ELEGIDA) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(grosor, colorEstado, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    when (estado) {
                        EstadoOpcion.CORRECTA, EstadoOpcion.INCORRECTA, EstadoOpcion.ELEGIDA -> colorEstado
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letra.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = when (estado) {
                    EstadoOpcion.NORMAL, EstadoOpcion.ATENUADA -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        val marca = when (estado) {
            EstadoOpcion.CORRECTA -> "✓ Correcta"
            EstadoOpcion.INCORRECTA -> "✗ Tu respuesta"
            else -> null
        }
        if (marca != null) {
            Spacer(Modifier.width(8.dp))
            Text(marca, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = colorEstado)
        }
    }
}
