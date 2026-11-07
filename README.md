# Examind

App de Android para estudiar con tests: crea preguntas tipo test a partir de los temarios en PDF que le subas y,
además, tests con preguntas de exámenes de temas relacionados encontrados en internet, indicando siempre la fuente.
Pensada para estudiantes de universidad. Todo se guarda en el móvil, sin cuentas ni servidor.

## Estado

| Paso | Estado |
|---|---|
| Esqueleto con el tema (claro, oscuro y según el sistema) y la galería de estilo | Hecho: compila en GitHub Actions |
| Modelo de datos (Room) y pruebas | Pendiente |
| Pantalla de inicio | Pendiente |
| Añadir temario PDF y generar preguntas (IA en el móvil) | Pendiente |
| Hacer test y resultado | Pendiente |
| Tests con exámenes online y su fuente | Pendiente |
| Copia de seguridad y exportar a CSV | Pendiente |

## Qué hace ahora

- Inicio con la cabecera y el estado vacío de temarios.
- Ajustes: tema según el sistema, claro u oscuro (se aplica al instante y se recuerda).
- Ajustes → «Ver galería de estilo»: todos los componentes, para revisar el modo claro y el oscuro.

## Decisiones

- **IA en el móvil**: las preguntas se generan con un modelo que se ejecuta en el propio teléfono (sin claves ni servidor).
- **Universidad**: 4 opciones por pregunta, una correcta, sin penalización por fallo.

## Estilo

Usa el módulo `plantilla-estilo` (copiado de Ceca). Lee `plantilla-estilo/ESTILO.md` antes de tocar la interfaz.
Para cambiar de marca, cambia solo los colores de identidad en `plantilla-estilo/.../estilo/Color.kt`.

## Compilar

GitHub Actions (`.github/workflows/build-apk.yml`) pasa las pruebas y compila el APK de depuración en cada push.
El APK queda como artefacto «Examind-debug» en la ejecución del flujo.

En local: `gradle testDebugUnitTest assembleDebug` (Gradle 8.9, JDK 17 y el SDK de Android 34).
