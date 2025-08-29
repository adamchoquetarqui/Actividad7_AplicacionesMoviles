package com.example.actividad7.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.actividad7.R

class AppointmentNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val KEY_APPOINTMENT_ID = "appointment_id"
        const val KEY_APPOINTMENT_DATE = "appointment_date"
        const val KEY_APPOINTMENT_TIME = "appointment_time"
        const val KEY_APPOINTMENT_REASON = "appointment_reason"
        const val CHANNEL_ID = "appointment_notifications"
        private const val TAG = "AppointmentNotificationWorker"
    }

    override fun doWork(): Result {
        Log.d(TAG, "🚀 Worker iniciado - ${java.util.Date()}")
        
        try {
            val appointmentId = inputData.getString(KEY_APPOINTMENT_ID) ?: return Result.failure()
            val appointmentDate = inputData.getString(KEY_APPOINTMENT_DATE) ?: return Result.failure()
            val appointmentTime = inputData.getString(KEY_APPOINTMENT_TIME) ?: return Result.failure()
            val appointmentReason = inputData.getString(KEY_APPOINTMENT_REASON) ?: return Result.failure()

            Log.d(TAG, "📋 Datos recibidos:")
            Log.d(TAG, "   🆔 ID: $appointmentId")
            Log.d(TAG, "   📅 Fecha: $appointmentDate")
            Log.d(TAG, "   ⏰ Hora: $appointmentTime")
            Log.d(TAG, "   📝 Motivo: $appointmentReason")

            createNotificationChannel()
            showNotification(appointmentId, appointmentDate, appointmentTime, appointmentReason)

            Log.d(TAG, "✅ Worker completado exitosamente")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en doWork: ${e.message}", e)
            return Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Citas"
            val descriptionText = "Canal para notificaciones de citas programadas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificación creado: $CHANNEL_ID")
        }
    }

    private fun showNotification(
        appointmentId: String,
        appointmentDate: String,
        appointmentTime: String,
        appointmentReason: String
    ) {
        try {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Usar un ID único para cada notificación
            val notificationId = appointmentId.hashCode()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🔔 Recordatorio de Cita")
                .setContentText("Tu cita está programada para hoy a las $appointmentTime")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Tu cita está programada para hoy ($appointmentDate) a las $appointmentTime.\n\nMotivo: $appointmentReason")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "✅ Notificación mostrada exitosamente con ID: $notificationId")
            Log.d(TAG, "📱 Título: Recordatorio de Cita")
            Log.d(TAG, "⏰ Hora: $appointmentTime")
            Log.d(TAG, "📅 Fecha: $appointmentDate")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al mostrar notificación: ${e.message}", e)
        }
    }
}
