# Prompt para crear una app nueva con el estilo de Ceca

Copia el bloque de abajo, rellena lo que va entre `[corchetes]` y pégalo en un chat nuevo de Claude Code con el repositorio
de la app nueva abierto. Si el repositorio no incluye la carpeta `plantilla-estilo/`, cópiala antes (con `ESTILO.md`).

---

```
Vas a crear la app de Android «[NOMBRE]»: [UNA FRASE: qué hace y para quién].

## Qué quiero
- Pantallas: [lista, p. ej. Inicio con el resumen, Lista de X, Añadir X, Ajustes].
- Datos que guarda: [p. ej. gastos con importe, fecha, categoría y nota], todo local en el móvil (sin cuentas ni servidor).
- Lo que más se hace, y debe costar un toque o dos: [la acción más frecuente].
- Idioma: español, tuteo.

## Estilo (obligatorio)
Lee `plantilla-estilo/ESTILO.md` entero antes de escribir nada y síguelo al pie de la letra. Resumen:
- Azul marino sobrio estilo banca, con degradado en las cabeceras; una cifra protagonista grande por pantalla.
- Verde = entra/bien, rojo = sale/mal, ámbar = aviso; siempre con signo o texto, el color nunca es la única pista.
- Tarjetas con borde fino y esquinas de 16–20 dp, chips redondos, márgenes de 16 dp, objetivos táctiles de 46 dp o más.
- Modo claro, oscuro y según el sistema desde el primer día; los dos se prueban con `GaleriaEstilo()`.
- Reutiliza los componentes del módulo `plantilla-estilo` (`EstiloTheme`, `CabeceraHero`, `CabeceraSimple`, `TarjetaDestacada`,
  `TarjetaPanel`, `FilaLista`, `BotonPrincipal`, `FilaPestanas`, `BarraProgresoSemantica`, `DialogoTarjeta`, `EstadoVacio`,
  `TecladoNumerico`…). No inventes otro estilo ni pantallas con otros colores; si falta un componente, créalo en ese mismo
  estilo y añádelo a la plantilla.
- Si quiero otra marca, cambia solo los colores de identidad en `Color.kt`.

## Cómo se usa (patrones de Ceca)
- Apuntar algo nuevo en segundos: teclado numérico propio siempre a la vista, importe enorme arriba, opciones habituales ya
  elegidas por el último uso, lo raro plegado («Ver todas», «Añadir nota»).
- Deshacer en vez de confirmar: tras guardar o borrar, aviso con «Deshacer». Solo se pide confirmación al borrar en bloque.
- Mantener pulsado una fila empieza la selección múltiple, con barra inferior (todos, eliminar, cancelar).
- Estados vacíos que dicen qué hacer; importaciones siempre con vista previa y sin duplicar.
- Textos en tono de `ESTILO.md` (sección 9): cortos, claros, sin exclamaciones; los errores dicen qué pasó y qué hacer.

## Técnica
- Kotlin, Jetpack Compose y Material 3; Room para los datos con migraciones explícitas (nunca borrar datos al actualizar);
  minSdk 26. Sin librerías que no hagan falta.
- La lógica de cálculo y los lectores de archivos como código puro (sin Android) y con pruebas unitarias; la interfaz fina.
- Copia de seguridad y exportar a CSV desde el principio.

## Cómo quiero que trabajes
1. Antes de programar, dime en pocas líneas el plan de pantallas y el modelo de datos, y pregúntame solo lo que de verdad
   cambie el resultado.
2. Trabaja en cambios pequeños y en este orden: esqueleto con el tema y la galería → modelo de datos y pruebas → pantalla
   de inicio → lo que más se usa → el resto.
3. Deja el APK compilado por GitHub Actions (flujo `build-apk.yml` como el de Ceca: pruebas y `assembleDebug`, el APK como
   artefacto). Tras cada cambio espera a que termine, y si falla, léelo y arréglalo antes de avisarme.
4. Cuando termines algo, dime en pocas líneas qué hace, el enlace al APK, cómo probarlo y qué límites tiene. No digas que algo
   funciona si solo se ha compilado: distingue «compila y pasan las pruebas» de «probado en el móvil».
5. No fusiones nada sin que yo lo diga. Mantén el README al día con lo que hace la app.

## Cuando haya algo que decidir
Si hay dos caminos razonables con costes distintos, no elijas en silencio: expón las opciones con tu recomendación.
```

---

## Versión corta (para una pantalla o un cambio dentro de una app que ya tiene el estilo)

```
En la app [NOMBRE] (ya usa `plantilla-estilo/ESTILO.md`): [lo que quieres].
Sigue el estilo de la guía y reutiliza los componentes del módulo; no cambies colores ni formas. Pruébalo en claro y en oscuro.
Hazlo en un cambio pequeño con prueba unitaria si hay lógica, deja el APK compilado y dime cómo probarlo y qué límites tiene.
```
