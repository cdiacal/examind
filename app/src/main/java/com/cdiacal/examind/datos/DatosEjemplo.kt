package com.cdiacal.examind.datos

import androidx.room.withTransaction
import com.cdiacal.examind.nucleo.Fuente
import com.cdiacal.examind.nucleo.Origen
import com.cdiacal.examind.nucleo.PreguntaNueva
import com.cdiacal.examind.nucleo.Puntuacion

/**
 * Un temario de ejemplo para ver la app con datos antes de subir un PDF. Las fuentes online son ficticias y lo
 * dicen (apuntan a example.org, un dominio reservado para ejemplos).
 */
object DatosEjemplo {

    const val NOMBRE = "Biología celular (ejemplo)"

    private val preguntas = listOf(
        PreguntaNueva("¿Qué orgánulo produce la mayor parte del ATP de la célula?", listOf("Mitocondria", "Ribosoma", "Aparato de Golgi", "Lisosoma"), 0, "La respiración celular aerobia ocurre en la mitocondria.", Origen.TEMARIO, 3),
        PreguntaNueva("¿Dónde se sintetizan las proteínas?", listOf("En el núcleo", "En los ribosomas", "En los peroxisomas", "En la vacuola"), 1, "Los ribosomas traducen el ARN mensajero a proteínas.", Origen.TEMARIO, 4),
        PreguntaNueva("¿Qué estructura tienen las células vegetales y no las animales?", listOf("Membrana plasmática", "Mitocondrias", "Pared celular", "Ribosomas"), 2, "La pared celular de celulosa es propia de las células vegetales.", Origen.TEMARIO, 5),
        PreguntaNueva("¿En qué fase de la mitosis se separan las cromátidas hermanas?", listOf("Profase", "Metafase", "Anafase", "Telofase"), 2, "En la anafase las cromátidas migran a polos opuestos.", Origen.TEMARIO, 8),
        PreguntaNueva("¿Qué molécula almacena la información genética?", listOf("ADN", "ATP", "Glucosa", "Colesterol"), 0, "El ADN contiene la información hereditaria.", Origen.TEMARIO, 2),
        PreguntaNueva("¿Cuál es la función principal del lisosoma?", listOf("Fotosíntesis", "Digestión celular", "Síntesis de lípidos", "Transporte de ARN"), 1, "Los lisosomas contienen enzimas hidrolíticas.", Origen.TEMARIO, 6),
        PreguntaNueva("¿Qué orgánulo realiza la fotosíntesis?", listOf("Mitocondria", "Cloroplasto", "Núcleo", "Centriolo"), 1, "La fotosíntesis ocurre en los cloroplastos.", Origen.TEMARIO, 5),
        PreguntaNueva("¿Cuántos cromosomas tiene una célula humana somática?", listOf("23", "44", "46", "48"), 2, "23 pares: 46 cromosomas.", Origen.TEMARIO, 9),
        PreguntaNueva("¿Qué modelo describe la membrana plasmática?", listOf("Mosaico fluido", "Doble hélice", "Llave y cerradura", "Modelo atómico"), 0, "Singer y Nicolson propusieron el modelo de mosaico fluido (1972).", Origen.ONLINE, null, Fuente("Ejemplo: examen ficticio de Biología, curso 2024-25", "https://example.org/examen-biologia-2025.pdf", 1_730_000_000_000)),
        PreguntaNueva("¿Qué tipo de transporte necesita gastar ATP?", listOf("Difusión simple", "Ósmosis", "Transporte activo", "Difusión facilitada"), 2, "El transporte activo va contra gradiente y consume energía.", Origen.ONLINE, null, Fuente("Ejemplo: examen ficticio de Biología, curso 2024-25", "https://example.org/examen-biologia-2025.pdf", 1_730_000_000_000)),
        PreguntaNueva("¿En qué orgánulo se modifican y empaquetan las proteínas?", listOf("Aparato de Golgi", "Mitocondria", "Nucléolo", "Citoesqueleto"), 0, "El aparato de Golgi modifica, clasifica y empaqueta.", Origen.ONLINE, null, Fuente("Ejemplo: banco de preguntas ficticio", "https://example.org/banco-preguntas", 1_730_000_000_000)),
        PreguntaNueva("¿Qué proceso da lugar a células con la mitad de cromosomas?", listOf("Mitosis", "Meiosis", "Citocinesis", "Replicación"), 1, "La meiosis produce gametos haploides.", Origen.ONLINE, null, Fuente("Ejemplo: banco de preguntas ficticio", "https://example.org/banco-preguntas", 1_730_000_000_000))
    )

    /** Lo que se ha creado, para poder deshacerlo. */
    data class Creado(val temarioId: Long, val testIds: List<Long>)

    suspend fun cargar(bd: BaseDatos, ahora: Long): Creado = bd.withTransaction {
        val temarioId = bd.temarios().insertar(TemarioEntidad(nombre = NOMBRE, tema = "biología celular", archivo = "", paginas = 12, creadoEl = ahora))
        val ids = bd.preguntas().insertar(preguntas.mapIndexed { i, p -> p.aEntidad(temarioId, ahora + i) })

        // Dos tests ya hechos, para que el inicio enseñe una media
        val testIds = listOf(
            Triple(ahora - 86_400_000L, listOf(true, true, false, true, true, false, true, true), "TEMARIO"),
            Triple(ahora - 3_600_000L, listOf(true, true, true, false, true, true, true, true, true, true), "MEZCLA")
        ).map { (fecha, aciertos, origen) ->
            val r = Puntuacion.calcular(aciertos)
            bd.tests().guardar(
                TestEntidad(temarioId = temarioId, fecha = fecha, origen = origen, aciertos = r.aciertos, fallos = r.fallos, enBlanco = r.enBlanco, nota = r.nota, duracionSegundos = 60L * aciertos.size),
                aciertos.mapIndexed { i, acierto ->
                    val pregunta = preguntas[i]
                    RespuestaEntidad(testId = 0, preguntaId = ids[i], orden = i, elegida = if (acierto) pregunta.correcta else (pregunta.correcta + 1) % 4, acertada = acierto)
                }
            )
        }
        Creado(temarioId, testIds)
    }

    suspend fun deshacer(bd: BaseDatos, creado: Creado) = bd.withTransaction {
        bd.tests().borrarPorIds(creado.testIds)
        bd.temarios().porId(creado.temarioId)?.let { bd.temarios().borrar(it) }
    }
}
