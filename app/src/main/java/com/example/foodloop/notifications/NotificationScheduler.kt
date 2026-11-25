package com.example.foodloop.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun schedule(context: Context) {

        val request =
            PeriodicWorkRequestBuilder<ExpirationWorker>(
                24,
                TimeUnit.HOURS
            ).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "food_expiration_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun testNow(context: Context) {

        val request =
            OneTimeWorkRequestBuilder<ExpirationWorker>()
                .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "food_expiration_test",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun cancel(context: Context) {

        val manager = WorkManager.getInstance(context)

        manager.cancelUniqueWork("food_expiration_check")

        manager.cancelUniqueWork("food_expiration_test")
    }
}
