package com.example.foodloop.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert
    suspend fun insert(food: Food): Long

    @Query("""
        UPDATE foods
        SET name = :name,
            quantity = :quantity,
            unit = :unit,
            category = :category,
            location = :location,
            expirationDate = :expirationDate,
            photoPath = :photoPath
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateFood(
        id: Int,
        userId: String,
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        location: String,
        expirationDate: String,
        photoPath: String?
    ): Int

    @Query("""
        DELETE FROM foods
        WHERE id = :id AND userId = :userId
    """)
    suspend fun deleteFood(
        id: Int,
        userId: String
    ): Int

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId
        ORDER BY expirationDate ASC
    """)
    fun getAllFoods(userId: String): Flow<List<Food>>

    @Query("""
        SELECT * FROM foods
        WHERE id = :id AND userId = :userId
    """)
    suspend fun getFoodById(
        id: Int,
        userId: String
    ): Food?

    suspend fun update(food: Food) {

        val affectedRows = updateFood(
            id = food.id,
            userId = food.userId,
            name = food.name,
            quantity = food.quantity,
            unit = food.unit,
            category = food.category,
            location = food.location,
            expirationDate = food.expirationDate,
            photoPath = food.photoPath
        )

        check(affectedRows == 1) {
            "Lebensmittel konnte nicht aktualisiert werden"
        }
    }

    suspend fun delete(food: Food) {

        val affectedRows = deleteFood(
            food.id,
            food.userId
        )

        check(affectedRows == 1) {
            "Lebensmittel konnte nicht gelöscht werden"
        }
    }
}
