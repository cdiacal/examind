package com.cdiacal.examind.estilo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Degradado de la cabecera, según el modo actual. */
@Composable
fun degradadoHero(): Brush {
    val oscuro = LocalEsModoOscuro.current
    return Brush.linearGradient(
        if (oscuro) listOf(HeroOscuroInicio, HeroOscuroFin)
        else listOf(HeroClaroInicio, HeroClaroFin)
    )
}

/**
 * Cabecera principal con degradado azul marino y esquinas inferiores redondeadas.
 * Se usa en la pantalla de inicio, donde va el dato más importante.
 */
@Composable
fun CabeceraHero(
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    mostrarBarraSuperior: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(degradadoHero())
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 26.dp)
    ) {
        if (onVolver != null || mostrarBarraSuperior) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (onVolver != null) {
                    IconButton(onClick = onVolver, modifier = Modifier.offset(x = (-10).dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(content = acciones)
            }
        }
        contenido()
    }
}

/** Título y subtítulo en blanco, para usar dentro de [CabeceraHero]. */
@Composable
fun TituloHero(titulo: String, subtitulo: String? = null) {
    Column {
        Text(titulo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (subtitulo != null) {
            Text(subtitulo, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        }
    }
}

/** Cifra destacada sobre la cabecera (el dato protagonista de la pantalla). */
@Composable
fun CifraHero(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        Text(
            valor,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp
        )
    }
}

/**
 * Dato pequeño en la cabecera (Ingresos / Gastos / Ahorro / Inversión).
 * Fuerza una sola línea: con cuatro columnas el espacio es justo, y es mejor
 * recortar con puntos suspensivos que romper la cifra en dos líneas.
 */
@Composable
fun MiniDatoHero(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 4.dp)) {
        Text(
            etiqueta,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.5f.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            valor,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Cabecera de pantalla secundaria: barra con degradado, botón de volver,
 * título, subtítulo opcional y acciones a la derecha.
 */
@Composable
fun CabeceraSimple(
    titulo: String,
    subtitulo: String? = null,
    onVolver: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(degradadoHero())
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onVolver != null) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
        } else {
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (subtitulo != null) {
                Text(subtitulo, color = Color.White.copy(alpha = 0.82f), fontSize = 11.5f.sp)
            }
        }
        Row(content = acciones)
        Spacer(Modifier.width(4.dp))
    }
}

/** Icono redondeado de categoría/cuenta/activo, con fondo suave según el modo. */
@Composable
fun IconoCategoria(
    emoji: String,
    id: Long,
    modifier: Modifier = Modifier,
    tamano: Dp = 36.dp
) {
    val oscuro = LocalEsModoOscuro.current
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.3f))
            .background(colorIconoPara(id, oscuro)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (tamano.value * 0.4f).sp)
    }
}

/** Igual que [IconoCategoria] pero con la inicial de un nombre (para cuentas). */
@Composable
fun IconoInicial(
    texto: String,
    id: Long,
    modifier: Modifier = Modifier,
    tamano: Dp = 36.dp
) {
    val oscuro = LocalEsModoOscuro.current
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.3f))
            .background(colorIconoPara(id, oscuro)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto.take(1).uppercase(),
            fontSize = (tamano.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Botón principal de acción, sólido en azul marino. */
@Composable
fun BotonPrincipal(
    texto: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .alpha(if (habilitado) 1f else 0.45f)
            .clip(forma)
            .background(degradadoHero())
            .clickable(
                enabled = habilitado,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
    }
}

/** Botón secundario, con borde y sin relleno. */
@Composable
fun BotonSecundario(
    texto: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .alpha(if (habilitado) 1f else 0.45f)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, forma)
            .clickable(
                enabled = habilitado,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Pestañas de estilo "segmentos": la activa se rellena, las demás quedan planas.
 * Sustituye a las carpetas del estilo anterior — más limpio y no corta el texto.
 */
@Composable
fun FilaPestanas(
    pestañas: List<String>,
    seleccionada: Int,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pestañas.forEachIndexed { indice, titulo ->
            val activa = indice == seleccionada
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (activa) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSeleccionar(indice) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    titulo,
                    fontSize = 12.sp,
                    fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (activa) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/** Tarjeta contenedora estándar (paneles de la pantalla). */
@Composable
fun TarjetaPanel(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = contenido
    )
}

// ════════ COLORES DE ESTADO SEGÚN EL MODO ════════
// Se usan en lugar de las constantes directas para que el verde/rojo/ámbar
// tengan suficiente contraste tanto en claro como en oscuro.

@Composable
fun colorIngreso(): Color = if (LocalEsModoOscuro.current) VerdeIngresoOscuro else VerdeIngreso

@Composable
fun colorGasto(): Color = if (LocalEsModoOscuro.current) RojoGastoOscuro else RojoGasto

@Composable
fun colorAviso(): Color = if (LocalEsModoOscuro.current) AmbarAvisoOscuro else AmbarAviso
