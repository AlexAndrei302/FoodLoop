package com.example.foodloop.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val checked: Boolean = false,

    @ColumnInfo(defaultValue = "''")
    val userId: String
)
