package com.cdiacal.examind.datos

import android.content.Context
import com.cdiacal.examind.nucleo.ConfigTest
import com.cdiacal.examind.nucleo.Correccion
import com.cdiacal.examind.nucleo.OrigenTest

/** La configuración del último test, para que el siguiente salga ya elegida. */
data class UltimaConfig(val temarioId: Long?, val config: ConfigTest, val correccion: Correccion)

class PreferenciasTest(context: Context) {
    private val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

    fun leer(): UltimaConfig = UltimaConfig(
        temarioId = prefs.getLong("temarioId", -1).takeIf { it >= 0 },
        config = ConfigTest(
            numPreguntas = prefs.getInt("num", 10),
            origen = runCatching { OrigenTest.valueOf(prefs.getString("origen", null)!!) }.getOrDefault(OrigenTest.TEMARIO),
            soloFalladas = prefs.getBoolean("soloFalladas", false)
        ),
        correccion = runCatching { Correccion.valueOf(prefs.getString("correccion", null)!!) }.getOrDefault(Correccion.AL_MOMENTO)
    )

    fun guardar(ultima: UltimaConfig) {
        prefs.edit()
            .putLong("temarioId", ultima.temarioId ?: -1)
            .putInt("num", ultima.config.numPreguntas)
            .putString("origen", ultima.config.origen.name)
            .putBoolean("soloFalladas", ultima.config.soloFalladas)
            .putString("correccion", ultima.correccion.name)
            .apply()
    }
}
