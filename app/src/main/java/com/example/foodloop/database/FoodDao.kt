
package com.example.foodloop.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert
    suspend fun insert(food: Food)

    @Update
    suspend fun update(food: Food)

    @Delete
    suspend fun delete(food: Food)

    @Query("SELECT * FROM foods ORDER BY expirationDate ASC")
    fun getAllFoods(): Flow<List<Food>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFoodById(id: Int): Food?
}
