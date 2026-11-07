package com.cdiacal.examind.datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Un temario con sus cuentas, para las listas. */
data class TemarioConCuentas(
    val id: Long,
    val nombre: String,
    val tema: String,
    val paginas: Int,
    val creadoEl: Long,
    val preguntasTemario: Int,
    val preguntasOnline: Int,
    val fragmentosSinUsar: Int
)

/** Lo que el selector de preguntas necesita de cada una (ver nucleo.Candidata). */
data class FilaCandidata(
    val id: Long,
    val origen: String,
    val vecesRespondida: Int,
    val vecesAcertada: Int,
    /** 1 acertada, 0 fallada, null en blanco o nunca respondida. */
    val ultimaAcertada: Boolean?
)

@Dao
interface TemarioDao {
    @Insert suspend fun insertar(temario: TemarioEntidad): Long
    @Insert suspend fun insertarTodos(temarios: List<TemarioEntidad>)
    @Update suspend fun actualizar(temario: TemarioEntidad)
    @Delete suspend fun borrar(temario: TemarioEntidad)

    @Query("SELECT * FROM temarios WHERE id = :id") suspend fun porId(id: Long): TemarioEntidad?
    @Query("SELECT * FROM temarios ORDER BY creadoEl") suspend fun todos(): List<TemarioEntidad>
    @Query("SELECT COUNT(*) FROM temarios WHERE nombre = :nombre") suspend fun contarLlamados(nombre: String): Int

    @Query(
        """
        SELECT t.id, t.nombre, t.tema, t.paginas, t.creadoEl,
            (SELECT COUNT(*) FROM preguntas p WHERE p.temarioId = t.id AND p.origen = 'TEMARIO') AS preguntasTemario,
            (SELECT COUNT(*) FROM preguntas p WHERE p.temarioId = t.id AND p.origen = 'ONLINE') AS preguntasOnline,
            (SELECT COUNT(*) FROM fragmentos f WHERE f.temarioId = t.id AND f.usado = 0) AS fragmentosSinUsar
        FROM temarios t ORDER BY t.creadoEl DESC
        """
    )
    fun observarConCuentas(): Flow<List<TemarioConCuentas>>

    @Insert suspend fun insertarFragmentos(fragmentos: List<FragmentoEntidad>)
    @Query("SELECT * FROM fragmentos WHERE temarioId = :temarioId ORDER BY orden") suspend fun fragmentos(temarioId: Long): List<FragmentoEntidad>
    @Query("SELECT * FROM fragmentos WHERE temarioId = :temarioId AND usado = 0 ORDER BY orden LIMIT :limite")
    suspend fun fragmentosSinUsar(temarioId: Long, limite: Int): List<FragmentoEntidad>
    @Query("UPDATE fragmentos SET usado = 1 WHERE id = :id") suspend fun marcarUsado(id: Long)
    @Query("SELECT * FROM fragmentos ORDER BY temarioId, orden") suspend fun todosLosFragmentos(): List<FragmentoEntidad>
}

@Dao
interface PreguntaDao {
    /** Las repetidas (misma huella en el mismo temario) se ignoran: devuelve -1 para ellas. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertar(preguntas: List<PreguntaEntidad>): List<Long>
    @Insert suspend fun insertarConId(preguntas: List<PreguntaEntidad>)
    @Update suspend fun actualizar(pregunta: PreguntaEntidad)
    @Delete suspend fun borrar(preguntas: List<PreguntaEntidad>)

    @Query("SELECT * FROM preguntas WHERE id IN (:ids)") suspend fun porIds(ids: List<Long>): List<PreguntaEntidad>
    @Query("SELECT huella FROM preguntas WHERE temarioId = :temarioId") suspend fun huellas(temarioId: Long): List<String>
    @Query("SELECT * FROM preguntas WHERE temarioId = :temarioId AND origen = :origen ORDER BY creadaEl DESC")
    fun observar(temarioId: Long, origen: String): Flow<List<PreguntaEntidad>>
    @Query("SELECT * FROM preguntas ORDER BY temarioId, creadaEl") suspend fun todas(): List<PreguntaEntidad>
    @Query("SELECT COUNT(*) FROM preguntas") fun observarTotal(): Flow<Int>

    @Query(
        """
        SELECT p.id, p.origen,
            (SELECT COUNT(*) FROM respuestas r WHERE r.preguntaId = p.id) AS vecesRespondida,
            (SELECT COUNT(*) FROM respuestas r WHERE r.preguntaId = p.id AND r.acertada = 1) AS vecesAcertada,
            (SELECT r.acertada FROM respuestas r JOIN tests t ON t.id = r.testId
                WHERE r.preguntaId = p.id ORDER BY t.fecha DESC, r.id DESC LIMIT 1) AS ultimaAcertada
        FROM preguntas p WHERE p.temarioId = :temarioId
        """
    )
    suspend fun candidatas(temarioId: Long): List<FilaCandidata>
}

@Dao
interface TestDao {
    @Insert suspend fun insertar(test: TestEntidad): Long
    @Insert suspend fun insertarTodos(tests: List<TestEntidad>)
    @Insert suspend fun insertarRespuestas(respuestas: List<RespuestaEntidad>)
    @Delete suspend fun borrar(tests: List<TestEntidad>)
    @Query("DELETE FROM tests WHERE id IN (:ids)") suspend fun borrarPorIds(ids: List<Long>)

    /** Guarda un test con sus respuestas de una vez; devuelve el id del test. */
    @Transaction
    suspend fun guardar(test: TestEntidad, respuestas: List<RespuestaEntidad>): Long {
        val id = insertar(test)
        insertarRespuestas(respuestas.map { it.copy(testId = id) })
        return id
    }

    @Query("SELECT * FROM tests ORDER BY fecha DESC") fun observar(): Flow<List<TestEntidad>>
    @Query("SELECT * FROM tests ORDER BY fecha") suspend fun todos(): List<TestEntidad>
    @Query("SELECT * FROM respuestas WHERE testId = :testId ORDER BY orden") suspend fun respuestas(testId: Long): List<RespuestaEntidad>
    @Query("SELECT * FROM respuestas ORDER BY testId, orden") suspend fun todasLasRespuestas(): List<RespuestaEntidad>
}
