package com.cdiacal.examind.datos

import com.cdiacal.examind.nucleo.Candidata
import com.cdiacal.examind.nucleo.CopiaSeguridad
import com.cdiacal.examind.nucleo.Fuente
import com.cdiacal.examind.nucleo.Origen
import com.cdiacal.examind.nucleo.PreguntaNueva
import com.cdiacal.examind.nucleo.ResultadoTest

/** Paso entre las tablas de Room y los modelos puros de :nucleo. */

fun PreguntaEntidad.aNueva(): PreguntaNueva = PreguntaNueva(
    enunciado = enunciado,
    opciones = opciones,
    correcta = correcta,
    explicacion = explicacion,
    origen = Origen.valueOf(origen),
    pagina = pagina,
    fuente = if (fuenteUrl != null) Fuente(fuenteTitulo.orEmpty(), fuenteUrl, fuenteConsultadaEl ?: 0) else null
)

fun PreguntaNueva.aEntidad(temarioId: Long, creadaEl: Long, id: Long = 0): PreguntaEntidad = PreguntaEntidad(
    id = id,
    temarioId = temarioId,
    origen = origen.name,
    enunciado = enunciado,
    opciones = opciones,
    correcta = correcta,
    explicacion = explicacion,
    pagina = pagina,
    fuenteTitulo = fuente?.titulo,
    fuenteUrl = fuente?.url,
    fuenteConsultadaEl = fuente?.consultadaEl,
    huella = huella,
    creadaEl = creadaEl
)

fun FilaCandidata.aCandidata(): Candidata = Candidata(
    id = id,
    origen = Origen.valueOf(origen),
    vecesRespondida = vecesRespondida,
    vecesFallada = vecesRespondida - vecesAcertada,
    ultimaFallada = vecesRespondida > 0 && ultimaAcertada != true
)

fun TestEntidad.aResultado(): ResultadoTest = ResultadoTest(aciertos, fallos, enBlanco, nota)

/** Toda la base de datos como copia de seguridad. */
suspend fun BaseDatos.crearCopia(ahora: Long): CopiaSeguridad.Copia = CopiaSeguridad.Copia(
    creadaEl = ahora,
    temarios = temarios().todos().map { CopiaSeguridad.Temario(it.id, it.nombre, it.tema, it.paginas, it.creadoEl) },
    fragmentos = temarios().todosLosFragmentos().map { CopiaSeguridad.Fragmento(it.temarioId, it.pagina, it.orden, it.texto) },
    preguntas = preguntas().todas().map { CopiaSeguridad.Pregunta(it.id, it.temarioId, it.creadaEl, it.aNueva()) },
    tests = tests().todos().map {
        CopiaSeguridad.Test(it.id, it.temarioId, it.fecha, it.origen, it.aciertos, it.fallos, it.enBlanco, it.nota, it.duracionSegundos)
    },
    respuestas = tests().todasLasRespuestas().map { CopiaSeguridad.Respuesta(it.testId, it.preguntaId, it.orden, it.elegida, it.acertada) }
)
