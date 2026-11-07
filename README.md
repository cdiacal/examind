# Examind

App de Android para estudiar con tests: crea preguntas tipo test a partir de los temarios en PDF que le subas y,
además, tests con preguntas de exámenes de temas relacionados encontrados en internet, indicando siempre la fuente.
Pensada para estudiantes de universidad. Todo se guarda en el móvil, sin cuentas ni servidor.

## Estado

| Paso | Estado |
|---|---|
| Esqueleto con el tema (claro, oscuro y según el sistema) y la galería de estilo | Hecho: compila en GitHub Actions |
| Modelo de datos (Room) y pruebas | Hecho: compila y pasan las pruebas |
| Pantalla de inicio | Hecho: compila y pasan las pruebas |
| Añadir temario PDF y generar preguntas (IA en el móvil) | Pendiente |
| Hacer test y resultado | Hecho: compila y pasan las pruebas |
| Tests con exámenes online y su fuente | Pendiente |
| Copia de seguridad y exportar a CSV | Pendiente |

## Qué hace ahora

- Inicio: % de aciertos de tus últimos 20 tests en grande, cuántos temarios, preguntas y tests llevas, tus temarios
  (con sus preguntas del temario y online) y tus 5 últimos tests con su nota y calificación (Suspenso, Aprobado,
  Notable, Sobresaliente). Botón + para añadir temario (aún no disponible).
- Hacer un test: eliges temario, origen (del temario, online o mezcla), «solo las que fallaste», número de preguntas
  y corrección «al momento» (ves si aciertas y la explicación) o «al final» (como un examen, puedes cambiar
  respuestas). Sale ya elegido lo del último test. Las opciones se barajan cada vez y se priorizan las preguntas
  nuevas y las que más fallas.
- Resultado: nota y calificación, aciertos, fallos, en blanco y tiempo; revisión de cada pregunta con la respuesta
  correcta, la explicación y la fuente (página del temario o enlace del examen online). «Repetir las que fallaste»
  hace un test nuevo solo con esas. Cada test queda guardado en el historial.
- Ajustes → «Añadir el ejemplo»: un temario de biología con 12 preguntas y dos tests, para ver la app con datos.
  Se puede deshacer. Sus fuentes online son ficticias (example.org).
- Ajustes: tema según el sistema, claro u oscuro (se aplica al instante y se recuerda).
- Ajustes → «Ver galería de estilo»: todos los componentes, para revisar el modo claro y el oscuro.

## Datos (todo en el móvil)

- **Temarios**: nombre, tema para buscar online, copia del PDF y nº de páginas.
- **Fragmentos**: el texto del PDF limpio y troceado, con su página (sirve para generar preguntas y citar la página).
- **Preguntas**: enunciado, 4 opciones, la correcta, explicación, origen (temario u online), página o fuente
  (título, enlace y fecha de consulta) y una huella para no guardar duplicados.
- **Tests y respuestas**: fecha, aciertos, fallos, en blanco, nota, duración y qué se contestó en cada pregunta.

Base de datos Room con migraciones explícitas: al actualizar nunca se borran datos. El esquema de cada versión lo
genera Room en `app/schemas` al compilar y el flujo lo sube como artefacto «esquema-bd»; se guardará en el
repositorio antes de la primera migración.

## Código

- `nucleo/`: lógica pura sin Android, con pruebas: limpieza y troceado del texto del PDF, lectura de las preguntas
  que devuelve la IA (tolerante a respuestas cortadas o mal formadas), duplicados, puntuación, selección de preguntas
  para un test, formatos en español, CSV y formato de la copia de seguridad.
- `app/`: interfaz (Compose) y base de datos (Room).
- `plantilla-estilo/`: el estilo compartido con Ceca.

## Decisiones

- **IA en el móvil**: las preguntas se generan con un modelo que se ejecuta en el propio teléfono (sin claves ni servidor):
  Gemini Nano donde el sistema lo ofrece y, si no, un modelo Gemma que se descarga una vez.
- **Exámenes online**: la app busca exámenes de universidad en internet, descarga la página o el PDF y el modelo del
  móvil saca las preguntas, guardando el enlace como fuente. Si la búsqueda falla, se puede pegar un enlace a mano.
- **Universidad**: 4 opciones por pregunta, una correcta, sin penalización por fallo.

## Estilo

Usa el módulo `plantilla-estilo` (copiado de Ceca). Lee `plantilla-estilo/ESTILO.md` antes de tocar la interfaz.
Para cambiar de marca, cambia solo los colores de identidad en `plantilla-estilo/.../estilo/Color.kt`.

## Compilar

GitHub Actions (`.github/workflows/build-apk.yml`) pasa las pruebas y compila el APK de depuración en cada push.
El APK queda como artefacto «Examind-debug» en la ejecución del flujo.

En local: `gradle :nucleo:test testDebugUnitTest assembleDebug` (Gradle 8.9, JDK 17 y el SDK de Android 34).
