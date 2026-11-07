# Guía de estilo de Ceca

El estilo visual de la app **Ceca**, escrito para reutilizarlo en otras apps. Es una app de finanzas personales con una
idea simple: **sobria, rápida y honesta**. Azul marino de banca, mucho aire, cifras grandes, colores que significan algo
(verde = entra, rojo = sale, ámbar = cuidado) y flujos de un toque.

Todo lo de aquí está en código en `src/main/java/com/cdiacal/examind/estilo/` (`GaleriaEstilo` los enseña todos).

---

## 1. Principios

1. **Sobrio, estilo banca.** Un solo color de identidad (azul marino) con degradado en las cabeceras. Sin colores chillones.
2. **El dato protagonista, grande.** Cada pantalla tiene una cifra principal (saldo, valor de la cartera, importe) en grande y
   en blanco sobre la cabecera; lo demás es secundario.
3. **Los colores significan.** Verde = ingreso/ganancia, rojo = gasto/pérdida, ámbar = aviso. Nunca se usan solo por decoración.
4. **Rápido antes que completo.** Lo habitual cabe en un toque (atajos, valores sugeridos, el último usado ya elegido). Lo raro
   queda plegado ("Ver todas", "Añadir nota").
5. **Deshacer en vez de confirmar.** Tras guardar o borrar se ofrece "Deshacer" unos segundos; solo se pide confirmación en lo
   destructivo en bloque (borrar varios).
6. **Honesto.** Si algo es una estimación o un dato puede no estar al día, se dice en una línea gris. No se inventan datos.
7. **Claro y oscuro de verdad.** Cada color tiene su par oscuro con contraste suficiente; los de estado cambian de tono.

---

## 2. Color

### Identidad (azul marino)
| Uso | Claro | Oscuro |
|---|---|---|
| Cabecera, degradado (inicio → fin) | `#152C52` → `#1E4B87` | `#11223F` → `#183C6B` |
| Acento: botones, elementos activos | `#1E4B87` | `#5A9BF5` |
| Contenedor de acento (tarjeta destacada) | `#E3EAF5` | `#1B2C46` |

### Estado (semánticos)
| Significado | Claro | Oscuro |
|---|---|---|
| Ingreso / ganancia / "vas bien" | `#12734A` | `#2DD48E` |
| Gasto / pérdida / "te has pasado" | `#C6473A` | `#F0705F` |
| Aviso / "te acercas al límite" | `#D9932B` | `#E5A93F` |

Se piden siempre con `colorIngreso()`, `colorGasto()` y `colorAviso()`, no con las constantes: así cambian de tono solos en oscuro.

### Superficies y texto
| Rol | Claro | Oscuro |
|---|---|---|
| Fondo de pantalla | `#F5F6FA` | `#0C0E16` |
| Superficie (tarjetas, diálogos) | `#FFFFFF` | `#161A26` |
| Borde / separador / fondo de chip | `#E6E9F0` | `#232838` |
| Texto principal | `#111420` | `#EDEEF5` |
| Texto secundario | `#767C90` | `#8189A0` |

### Iconos y gráficos
- **Iconos de categoría/cuenta/activo:** 8 tonos pastel apagados, asignados por `id % 8` (siempre el mismo para el mismo elemento).
  Claros: `#E3EAF5 #E0EFE7 #F5EADF #EDE6F3 #F6E4E4 #E1EFF2 #F0EEE3 #E7EAF0`; oscuros: `#1E2A3E #1B2F28 #322A20 #2A2438 #352424 #1C2E33 #2E2C22 #242833`.
- **Gráficos:** 8 colores más saturados para distinguir series: `#1E4B87 #12734A #D9932B #C6473A #6B4E9E #2A8A99 #8A6A3D #4A5C7A`.

### Reglas
- Un único color de acento por app. Para cambiar de marca basta con cambiar los 3 valores de identidad (cabecera, acento, contenedor).
- Los importes van en verde/rojo **y** con signo (`+`/`-`): el color nunca es la única señal.
- Texto blanco sobre la cabecera; blanco al 82 % para lo secundario.

---

## 3. Tipografía

Fuente del sistema (Roboto), sin fuentes propias. Se usan los estilos de Material 3 y estos tamaños fijos:

| Elemento | Tamaño / peso |
|---|---|
| Cifra protagonista (cabecera) | 32 sp, negrita, interletraje −1 |
| Importe del formulario | 42 sp, negrita (30 sp si es largo) |
| Título de pantalla (hero) | 20 sp, negrita |
| Título de pantalla secundaria | 17 sp, negrita |
| Título de tarjeta / diálogo | `titleMedium` / `titleLarge` |
| Texto normal | `bodyMedium` (14 sp) |
| Texto secundario / pie | `bodySmall` (12 sp), color secundario |
| Subtítulo sobre la cabecera | 12 sp, blanco 82 % |
| Dato pequeño de cabecera | etiqueta 10,5 sp + valor 14 sp negrita, una sola línea |
| Botones y chips | 12–13 sp, semi-negrita |
| Etiquetas de sección | `labelMedium`, color secundario |

Las cifras se formatean con coma decimal y punto de miles (`1.234,56 €`); sin decimales si son redondas en las vistas pequeñas.

---

## 4. Forma y espaciado

| Elemento | Radio |
|---|---|
| Cabecera de inicio | 24 dp solo abajo |
| Tarjeta destacada, diálogos, popup | 20 dp |
| Cabecera secundaria, tarjeta de panel, barra de selección | 16 dp |
| Botones, teclas, campos | 12 dp |
| Chips y pestañas | totalmente redondos (100 dp) |
| Barra de progreso | 4 dp (alto 8 dp) |
| Icono redondeado | 30 % de su tamaño |

- **Márgenes de pantalla:** 16 dp (12 dp en pantallas con listas largas). **Relleno de tarjeta:** 16 dp (20 dp en tarjeta destacada y diálogos).
- **Separación** entre bloques: 8, 12 o 16 dp (nada de valores sueltos); entre filas de lista, 10 dp de relleno vertical y línea fina.
- **Tamaño táctil:** teclas y botones de 46 dp de alto como mínimo; filas de lista de unos 56 dp.
- **Elevación:** casi plana. Tarjetas con borde de 1 dp en vez de sombra; sombra solo en lo flotante (popup, barra de selección).

---

## 5. Componentes

| Componente | Para qué |
|---|---|
| `EstiloTheme` | Envuelve la app: claro/oscuro/sistema y colores de Material |
| `CabeceraHero` + `TituloHero` + `CifraHero` + `MiniDatoHero` | Cabecera de la pantalla de inicio con degradado, dato protagonista y datos pequeños |
| `CabeceraSimple` | Cabecera de pantallas secundarias: volver, título, subtítulo y acciones |
| `TarjetaDestacada` | El dato clave de una pantalla (valor de la cartera) con su variación |
| `TarjetaPanel` | Contenedor estándar con borde de 1 dp |
| `FilaLista` + `IconoCategoria` / `IconoInicial` | Filas de movimientos/cuentas con icono pastel e importe en su color |
| `BotonPrincipal` / `BotonSecundario` | Acción principal (degradado azul) y secundaria (borde) |
| `SegmentButton` | Elegir entre 2–3 opciones (Gasto / Ingreso / Entre cuentas) |
| `FilaPestanas` | Chips desplazables: día rápido, cuenta, periodo |
| `BarraProgresoSemantica` | Presupuestos y límites: verde → ámbar (80 %) → rojo (100 %) |
| `DialogoTarjeta` | Diálogo estándar: título, contenido, Cancelar + acción principal |
| `EstadoVacio` | Lista vacía diciendo qué hacer a continuación |
| `TecladoNumerico` + `teclear` | Teclado propio para importes, con suma y Guardar en la última fila |
| `filaSeleccionable` + `CasillaSeleccion` + `BarraSeleccion` | Selección múltiple manteniendo pulsado |
| `TarjetaOpcion` + `EstadoOpcion` | Opción de respuesta de un test: elegida, correcta («✓ Correcta»), incorrecta («✗ Tu respuesta») o atenuada (añadido en Examind) |

Emoji como iconos de categoría: sin librería de iconos, reconocibles al instante y con tono propio por `id`.

---

## 6. Patrones de interacción

- **Apuntar en segundos:** teclado numérico siempre a la vista, importe enorme arriba, categorías como cuadrícula de iconos grandes
  (8 visibles, "Ver todas"), día a un toque (Hoy, Ayer, …), cuenta y categoría ya sugeridas por el último uso y por lo escrito.
  Opción "⚡ guardar al tocar la categoría" para los más rápidos.
- **Atajos:** lo que más repites sale como chips (toque rellena, mantener pulsado guarda).
- **Confirmación con Deshacer:** tras guardar, una tarjeta "Guardado ✓" con "Deshacer" y "Añadir otro" que se cierra sola a los 3 s
  (más si hay algo que leer).
- **Selección múltiple:** mantener pulsado una fila empieza a seleccionar; aparece una barra abajo con cuántos y cuánto suman, "Todos",
  "Eliminar" (con confirmación) y ✕. Después, "Deshacer" restaura todo.
- **Sugerir, no imponer:** categorías por palabras clave, cuenta de destino por defecto, enlaces de precio comprobados contra lo que
  costaron; siempre editable antes de confirmar.
- **Avisos graduales:** verde → ámbar al 80 % → rojo al pasarse, con el texto exacto ("Te has pasado 20 €").
- **Importar con vista previa:** nunca se escribe nada sin enseñar antes lo que se va a hacer, con casillas y los posibles duplicados sin marcar.
- **Lo plegado:** nota, categorías extra, filtros: un toque para abrirlos, fuera de la vista por defecto.
- **Estados vacíos útiles:** "Aún no hay movimientos este mes. Toca el botón + para apuntar el primero."
- **Botón flotante (+)** abajo a la derecha, del color de acento; se sustituye por la barra de selección cuando hay selección.

---

## 7. Modo oscuro

- Mismo layout, otros valores: fondo casi negro azulado (`#0C0E16`), superficies un paso más claras, acento más claro (`#5A9BF5`).
- Los verdes/rojos/ámbar se aclaran para mantener el contraste sobre fondo oscuro.
- Preferencia en tres estados (sistema / claro / oscuro) guardada y observable: cambiarla repinta toda la app al instante.
- La barra de estado lleva el azul de las cabeceras (`#152C52`) en ambos modos.

---

## 8. Marca e iconos

- **Logo:** una moneda partida en dos mitades desplazadas; la de arriba, verde menta (`#5FD9A0`), sube a la derecha y la de abajo,
  blanca, baja a la izquierda: el hueco dibuja un escalón ascendente. Sobre fondo azul marino `#152C52`.
- **Icono adaptativo** de Android: primer plano en vector (`ejemplos-xml/ic_launcher_foreground.xml`) dentro del área segura central.
- **Widget** (que no puede usar el tema de Compose): fondo con degradado azul marino a 315°, esquinas de 20 dp, texto blanco y
  cifras en verde menta `#5FD9A0` / rojo suave `#F0857A` (`ejemplos-xml/colors.xml`, `fondo_widget.xml`).

---

## 9. Tono de los textos

- Español, **tuteo** y frases cortas: "Te quedan 120 € en tus presupuestos: unos 8 € al día".
- Verbos claros en los botones: Guardar, Eliminar, Añadir otro, Deshacer. Sin signos de exclamación.
- Los errores dicen **qué pasó y qué hacer**: "No se pudo consultar el precio ahora. Se guarda el enlace y se probará de nuevo."
- Lo estimado se llama estimación y lo manual, manual; si algo depende de una fuente externa, se avisa en gris.

---

## 10. Lista de comprobación para una pantalla nueva

- [ ] Tiene un dato protagonista en grande y todo lo demás es secundario.
- [ ] Los importes llevan signo y color de estado; el color no es la única pista.
- [ ] Funciona en claro y en oscuro (probar los dos con `GaleriaEstilo`).
- [ ] Hay estado vacío con la siguiente acción, y estado de carga si tarda.
- [ ] Lo habitual cabe en un toque; lo raro está plegado.
- [ ] Lo destructivo se puede deshacer o pide confirmación.
- [ ] Márgenes de 16 dp, radios de la tabla, objetivos táctiles de 46 dp o más.
- [ ] Los textos están en el tono de la sección 9.

---

## 11. Empezar una app nueva con esta plantilla

1. Copia la carpeta `plantilla-estilo/` a tu proyecto (o añádela como módulo) y cambia el paquete `com.cdiacal.examind.estilo`
   por el tuyo, y la carpeta para que coincida.
2. En tu `build.gradle.kts` añade `implementation(project(":plantilla-estilo"))` (o copia las dependencias de Compose que lista su `build.gradle.kts`).
3. En tu actividad: `TemaState.cargar(this)` y `setContent { EstiloTheme { MiPantalla() } }`. Para ver el conjunto, `GaleriaEstilo()`.
4. Para otra marca, cambia en `Color.kt` los valores de identidad (`Hero*`, `AzulPrincipal*`, `AzulContenedor*`) y, si hace falta, el verde/rojo/ámbar.
5. Copia de `ejemplos-xml/` el tema de Android (barra de estado del color de cabecera) y, si lo necesitas, los colores del widget y el icono.

### Prompt para pedirle a Claude una app con este estilo
Está en `PROMPT.md`, en versión completa (app nueva, con estilo, patrones, técnica y forma de trabajar) y en versión corta
(una pantalla o un cambio en una app que ya usa el estilo).
