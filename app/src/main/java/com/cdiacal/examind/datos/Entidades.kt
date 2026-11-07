package com.cdiacal.examind.datos

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Un temario subido en PDF. [archivo] es el nombre de la copia del PDF dentro de la app; [tema], lo que se busca online. */
@Entity(tableName = "temarios")
data class TemarioEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tema: String,
    val archivo: String,
    val paginas: Int,
    val creadoEl: Long
)

/** Un trozo del texto del temario. [usado] es true cuando ya se han generado preguntas con él. */
@Entity(
    tableName = "fragmentos",
    foreignKeys = [ForeignKey(TemarioEntidad::class, ["id"], ["temarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("temarioId")]
)
data class FragmentoEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val temarioId: Long,
    val pagina: Int,
    val orden: Int,
    val texto: String,
    @ColumnInfo(defaultValue = "0") val usado: Boolean = false
)

/**
 * Una pregunta tipo test. [origen] es "TEMARIO" u "ONLINE"; [opciones] se guarda como JSON.
 * [huella] evita guardar dos veces la misma pregunta en un temario.
 */
@Entity(
    tableName = "preguntas",
    foreignKeys = [ForeignKey(TemarioEntidad::class, ["id"], ["temarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("temarioId"), Index(value = ["temarioId", "huella"], unique = true)]
)
data class PreguntaEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val temarioId: Long,
    val origen: String,
    val enunciado: String,
    val opciones: List<String>,
    val correcta: Int,
    val explicacion: String,
    val pagina: Int?,
    val fuenteTitulo: String?,
    val fuenteUrl: String?,
    val fuenteConsultadaEl: Long?,
    val huella: String,
    val creadaEl: Long
)

/** Un test hecho. [temarioId] queda a null si después se borra el temario (el historial se conserva). */
@Entity(
    tableName = "tests",
    foreignKeys = [ForeignKey(TemarioEntidad::class, ["id"], ["temarioId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("temarioId"), Index("fecha")]
)
data class TestEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val temarioId: Long?,
    val fecha: Long,
    val origen: String,
    val aciertos: Int,
    val fallos: Int,
    val enBlanco: Int,
    val nota: Double,
    val duracionSegundos: Long
)

/** La respuesta a una pregunta dentro de un test. [elegida] es el índice en las opciones guardadas; null = en blanco. */
@Entity(
    tableName = "respuestas",
    foreignKeys = [
        ForeignKey(TestEntidad::class, ["id"], ["testId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PreguntaEntidad::class, ["id"], ["preguntaId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("testId"), Index("preguntaId")]
)
data class RespuestaEntidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long,
    val preguntaId: Long,
    val orden: Int,
    val elegida: Int?,
    val acertada: Boolean?
)
