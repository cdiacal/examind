# plantilla-estilo

El estilo visual de **Ceca** convertido en un módulo de Android reutilizable para futuras apps: tema claro/oscuro, paleta,
componentes de Compose y una galería que los enseña todos.

- **`ESTILO.md`**: la guía de estilo (principios, colores con sus códigos, tipografía, formas, componentes, patrones de
  interacción, tono de los textos y cómo empezar una app nueva). Se puede pegar tal cual a Claude al pedirle una app.
- **`PROMPT.md`**: el prompt para pedirle a Claude una app nueva (o una pantalla) con este estilo, listo para rellenar y pegar.
- **`src/main/java/com/cdiacal/examind/estilo/`**: el código.
  - `Color.kt`: paleta de identidad, estado, superficies e iconos (claro y oscuro).
  - `Theme.kt`: `EstiloTheme`, `ModoTema` y `TemaState` (preferencia claro/oscuro/sistema, observable).
  - `Componentes.kt`: cabeceras, botones, pestañas, tarjeta de panel, iconos redondeados y colores de estado.
  - `Extras.kt`: tarjeta destacada, fila de lista, barra de progreso, diálogo, selección múltiple y teclado numérico.
  - `TecladoImporte.kt`: la lógica pura del teclado (con sumas), sin dependencias de Android.
  - `Galeria.kt`: `GaleriaEstilo()` con todo junto y vistas previas en claro y oscuro.
- **`ejemplos-xml/`**: el tema de Android, los colores del widget, el fondo del widget y el logo (vector) de Ceca.

Es un módulo de Gradle (`:plantilla-estilo`) copiado de `cdiacal/ceca-app` y renombrado al paquete `com.cdiacal.examind.estilo`; la app Examind lo usa
directamente. Si falta un componente, se crea aquí en el mismo estilo. Para usarlo en otra app, sigue la sección 11 de `ESTILO.md`.
