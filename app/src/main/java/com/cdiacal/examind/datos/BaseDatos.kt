package com.cdiacal.examind.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

class Conversores {
    @TypeConverter fun opcionesAJson(opciones: List<String>): String = JSONArray(opciones).toString()

    @TypeConverter fun jsonAOpciones(json: String): List<String> =
        JSONArray(json).let { a -> (0 until a.length()).map { a.getString(it) } }
}

/**
 * Base de datos local. Al cambiar las tablas: sube [version], escribe la migración (nunca se borran datos al
 * actualizar; no hay fallbackToDestructiveMigration) y añádela en [MIGRACIONES]. El esquema de cada versión se
 * guarda en app/schemas para comprobarlo.
 */
@Database(
    entities = [TemarioEntidad::class, FragmentoEntidad::class, PreguntaEntidad::class, TestEntidad::class, RespuestaEntidad::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class BaseDatos : RoomDatabase() {
    abstract fun temarios(): TemarioDao
    abstract fun preguntas(): PreguntaDao
    abstract fun tests(): TestDao

    companion object {
        /** Migraciones entre versiones, en orden. Vacía mientras solo exista la versión 1. */
        val MIGRACIONES = arrayOf<androidx.room.migration.Migration>()

        @Volatile private var instancia: BaseDatos? = null

        fun obtener(context: Context): BaseDatos = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, BaseDatos::class.java, "examind.db")
                .addMigrations(*MIGRACIONES)
                .build()
                .also { instancia = it }
        }
    }
}
