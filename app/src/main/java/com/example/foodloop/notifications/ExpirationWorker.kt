package com.example.foodloop.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodloop.database.FoodDatabase
import com.example.foodloop.repository.AuthRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpirationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val authRepository = AuthRepository()

        val userId = authRepository.getCurrentUserId()
            ?: return Result.success()

        val foodDao = FoodDatabase
            .getDatabase(applicationContext)
            .foodDao()

        val foods = foodDao.getAllFoods(userId).first()

        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        dateFormat.isLenient = false

        val today = Calendar.getInstance()

        val todayString = dateFormat.format(today.time)

        val limit = Calendar.getInstance()

        limit.add(Calendar.DAY_OF_YEAR, 3)

        val limitString = dateFormat.format(limit.time)

        val expiredFoods = mutableListOf<String>()

        val expiringFoods = mutableListOf<String>()

        for (food in foods) {

            val expirationDate = food.expirationDate

            val validDate = try {

                expirationDate.matches(
                    Regex("\\d{4}-\\d{2}-\\d{2}")
                ) && dateFormat.parse(expirationDate) != null

            } catch (e: Exception) {
                false
            }

            if (!validDate) {
                continue
            }

            if (expirationDate < todayString) {

                expiredFoods.add(food.name)

            } else if (expirationDate <= limitString) {

                expiringFoods.add(food.name)
            }
        }

        if (authRepository.getCurrentUserId() != userId) {
            return Result.success()
        }

        if (expiredFoods.isNotEmpty()) {

            NotificationHelper.showNotification(
                applicationContext,
                1001,
                "Abgelaufene Lebensmittel",
                expiredFoods.joinToString(", ") +
                        " sind bereits abgelaufen."
            )
        }

        if (expiringFoods.isNotEmpty()) {

            NotificationHelper.showNotification(
                applicationContext,
                1002,
                "Lebensmittel laufen bald ab!",
                expiringFoods.joinToString(", ") +
                        " laufen innerhalb von 3 Tagen ab."
            )
        }

        return Result.success()
    }
}
