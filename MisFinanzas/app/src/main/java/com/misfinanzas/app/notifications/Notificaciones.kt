package com.misfinanzas.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.misfinanzas.app.MainActivity
import com.misfinanzas.app.R
import com.misfinanzas.app.domain.repository.RecordatorioRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificacionesHelper {
    const val CANAL_ID = "misfinanzas_canal"

    fun mostrar(context: Context, id: Int, titulo: String, mensaje: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(id, notif)
        }
    }

    /** Programa un worker único que dispare la notificación a la próxima ocurrencia de [hora]. */
    fun programar(context: Context, recordatorioId: Long, hora: LocalTime) {
        val ahora = LocalDateTime.now()
        var siguiente = ahora.toLocalDate().atTime(hora)
        if (!siguiente.isAfter(ahora)) siguiente = siguiente.plusDays(1)
        val delay = Duration.between(ahora, siguiente).toMillis()

        val request = OneTimeWorkRequestBuilder<RecordatorioWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(RecordatorioWorker.KEY_ID to recordatorioId))
            .addTag("recordatorio_$recordatorioId")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "recordatorio_$recordatorioId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelar(context: Context, recordatorioId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("recordatorio_$recordatorioId")
    }
}

@HiltWorker
class RecordatorioWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: RecordatorioRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1L)
        if (id < 0) return Result.failure()
        val recordatorio = repo.obtenerActivos().firstOrNull { it.id == id } ?: return Result.success()
        if (!recordatorio.activo) return Result.success()

        val hoy = java.time.LocalDate.now().dayOfWeek.value // 1=Lun..7=Dom
        if (hoy in recordatorio.diasSemana) {
            NotificacionesHelper.mostrar(
                applicationContext, id.toInt(), recordatorio.titulo, recordatorio.mensaje
            )
        }
        // reprograma para mañana a la misma hora
        NotificacionesHelper.programar(applicationContext, id, recordatorio.hora)
        return Result.success()
    }

    companion object { const val KEY_ID = "recordatorio_id" }
}
