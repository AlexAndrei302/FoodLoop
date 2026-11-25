
package com.example.foodloop.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Query("""
        UPDATE shopping_items
        SET name = :name,
            checked = :checked
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateItem(
        id: Int,
        userId: String,
        name: String,
        checked: Boolean
    ): Int

    @Query("""
        DELETE FROM shopping_items
        WHERE id = :id AND userId = :userId
    """)
    suspend fun deleteItem(
        id: Int,
        userId: String
    ): Int

    @Query("""
        SELECT * FROM shopping_items
        WHERE userId = :userId
        ORDER BY checked ASC, id ASC
    """)
    fun getAllItems(
        userId: String
    ): Flow<List<ShoppingItem>>

    suspend fun update(item: ShoppingItem) {

        check(
            updateItem(
                item.id,
                item.userId,
                item.name,
                item.checked
            ) == 1
        )
    }

    suspend fun delete(item: ShoppingItem): Int {

        return deleteItem(
            item.id,
            item.userId
        )
    }
}
