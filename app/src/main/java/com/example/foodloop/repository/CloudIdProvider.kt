package com.example.foodloop.repository

import android.content.Context
import java.util.UUID

object CloudIdProvider {

    @Synchronized
    fun getId(context: Context, localId: Int): String {

        val preferences = context.getSharedPreferences(
            "foodloop_cloud",
            Context.MODE_PRIVATE
        )

        var deviceId = preferences.getString(
            "device_id",
            null
        )

        if (deviceId == null) {

            deviceId = UUID.randomUUID().toString()

            preferences.edit()
                .putString("device_id", deviceId)
                .apply()
        }

        return "${deviceId}_$localId"
    }
}
