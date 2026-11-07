package com.cdiacal.examind.segundoplano

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cdiacal.examind.MainActivity
import com.cdiacal.examind.R

/** Las notificaciones de Examind: el progreso de crear preguntas y el aviso de que ya se pueden revisar. */
object Avisos {
    const val CANAL_PROGRESO = "progreso"
    const val CANAL_LISTAS = "listas"
    const val ID_PROGRESO = 1
    private const val ID_LISTAS_BASE = 1000

    /** Extra del intent para abrir un temario al tocar la notificación. */
    const val EXTRA_TEMARIO = "temarioId"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = context.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_PROGRESO, "Creando preguntas", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progreso mientras la IA escribe preguntas en segundo plano"
            }
        )
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_LISTAS, "Preguntas listas", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Aviso cuando hay preguntas nuevas para revisar"
            }
        )
    }

    private fun abrirTemario(context: Context, temarioId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_TEMARIO, temarioId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, temarioId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun progreso(context: Context, temarioId: Long, titulo: String, texto: String, hechos: Int, total: Int, cancelar: PendingIntent) =
        NotificationCompat.Builder(context, CANAL_PROGRESO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(total, hechos, total == 0)
            .setContentIntent(abrirTemario(context, temarioId))
            .addAction(0, "Cancelar", cancelar)
            .build()

    fun actualizarProgreso(context: Context, notificacion: android.app.Notification) = enviar(context, ID_PROGRESO, notificacion)

    /** Aviso final: preguntas listas para revisar, o qué ha fallado. */
    fun terminado(context: Context, temarioId: Long, titulo: String, texto: String) {
        val n = NotificationCompat.Builder(context, CANAL_LISTAS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setAutoCancel(true)
            .setContentIntent(abrirTemario(context, temarioId))
            .build()
        enviar(context, ID_LISTAS_BASE + temarioId.toInt(), n)
    }

    private fun enviar(context: Context, id: Int, n: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            // Sin permiso de notificaciones: el trabajo sigue igual, solo no se avisa
        }
    }
}
