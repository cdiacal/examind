package com.cdiacal.examind

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cdiacal.examind.estilo.EstiloTheme
import com.cdiacal.examind.estilo.TemaState
import com.cdiacal.examind.segundoplano.Avisos
import com.cdiacal.examind.ui.AppExamind

class MainActivity : ComponentActivity() {
    /** Temario que hay que abrir porque se ha tocado su notificación («preguntas listas para revisar»). */
    private var temarioPedido by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaState.cargar(this)
        Avisos.crearCanales(this)
        if (savedInstanceState == null) leerIntent(intent)
        setContent {
            EstiloTheme { AppExamind(temarioPedido = temarioPedido, onTemarioAbierto = { temarioPedido = null }) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        leerIntent(intent)
    }

    private fun leerIntent(intent: Intent?) {
        val id = intent?.getLongExtra(Avisos.EXTRA_TEMARIO, -1) ?: -1
        if (id >= 0) temarioPedido = id
    }
}
