package com.example.foodloop.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class Food(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val quantity: Double,

    val unit: String,

    val category: String,

    val location: String,

    val expirationDate: String,

    val photoPath: String? = null,

    @ColumnInfo(defaultValue = "''")
    val userId: String
)
