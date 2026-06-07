package com.example.mycar.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mycar.data.model.Car
import com.example.mycar.ui.car.CarListActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "DOWORK START")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()

        return try {
            val snapshot = db.collection("users").document(userId).collection("cars").get().await()
            val cars = snapshot.toObjects(Car::class.java)
            Log.d("NotificationWorker", "Mașini găsite: ${cars.size}")

            val today = Calendar.getInstance().time
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

            cars.forEach { car ->
                Log.d("NotificationWorker", "Verific mașina: ${car.brand} ${car.model} (${car.licensePlate})")
                checkAndNotify(car.itpExpiry, "ITP", car.brand, car.model, today, sdf)
                checkAndNotify(car.rcaExpiry, "RCA", car.brand, car.model, today, sdf)
                checkAndNotify(car.rovinietaExpiry, "Rovinietă", car.brand, car.model, today, sdf)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Eroare: ${e.message}")
            Result.retry()
        }
    }

    private fun checkAndNotify(dateStr: String, type: String, brand: String, model: String, today: Date, sdf: SimpleDateFormat) {
        if (dateStr.isEmpty()) return

        try {
            val expiryDate = sdf.parse(dateStr) ?: return
            val diffInMillies = expiryDate.time - today.time
            val diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)
            
            Log.d("NotificationWorker", "Zile până la expirare $type: $diffInDays")

            if (diffInDays in 0..7) {
                Log.d("NotificationWorker", "TRIMIT NOTIFICARE pentru $type")
                sendNotification(
                    "Expirare $type",
                    "Documentul $type pentru $brand $model expiră în $diffInDays zile!"
                )
            }
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Eroare parsare dată: $dateStr")
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "car_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alerte Mașină", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, CarListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (title + message).hashCode()
        notificationManager.notify(notificationId, notification)
    }
}
