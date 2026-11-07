package com.cdiacal.examind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cdiacal.examind.estilo.EstiloTheme
import com.cdiacal.examind.estilo.TemaState
import com.cdiacal.examind.ui.AppExamind

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaState.cargar(this)
        setContent {
            EstiloTheme { AppExamind() }
        }
    }
}
