package com.cdiacal.examind.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    entities = [
        TemarioEntidad::class, FragmentoEntidad::class, PreguntaEntidad::class, TestEntidad::class, RespuestaEntidad::class,
        BorradorEntidad::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class BaseDatos : RoomDatabase() {
    abstract fun temarios(): TemarioDao
    abstract fun preguntas(): PreguntaDao
    abstract fun tests(): TestDao
    abstract fun borradores(): BorradorDao

    companion object {
        /** 1 → 2: huella del PDF en cada temario, para no añadir dos veces el mismo. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE temarios ADD COLUMN huellaArchivo TEXT NOT NULL DEFAULT ''")
            }
        }

        /** 2 → 3: preguntas pendientes de revisar (la IA las escribe en segundo plano). */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `borradores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`temarioId` INTEGER NOT NULL, `fragmentoId` INTEGER NOT NULL, `enunciado` TEXT NOT NULL, " +
                        "`opciones` TEXT NOT NULL, `correcta` INTEGER NOT NULL, `explicacion` TEXT NOT NULL, `pagina` INTEGER, " +
                        "`huella` TEXT NOT NULL, `creadaEl` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`temarioId`) REFERENCES `temarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_borradores_temarioId` ON `borradores` (`temarioId`)")
            }
        }

        /** Migraciones entre versiones, en orden. Nunca se borran datos: no hay fallbackToDestructiveMigration. */
        val MIGRACIONES = arrayOf(MIGRACION_1_2, MIGRACION_2_3)

        @Volatile private var instancia: BaseDatos? = null

        fun obtener(context: Context): BaseDatos = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(context.applicationContext, BaseDatos::class.java, "examind.db")
                .addMigrations(*MIGRACIONES)
                .build()
                .also { instancia = it }
        }
    }
}
