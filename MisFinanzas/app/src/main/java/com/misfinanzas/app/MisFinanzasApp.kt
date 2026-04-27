package com.misfinanzas.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.misfinanzas.app.notifications.NotificacionesHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MisFinanzasApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        crearCanalesNotificacion()
    }

    private fun crearCanalesNotificacion() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val canal = NotificationChannel(
            NotificacionesHelper.CANAL_ID,
            "Recordatorios",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Recordatorios de Mis Finanzas" }
        nm.createNotificationChannel(canal)
    }
}
