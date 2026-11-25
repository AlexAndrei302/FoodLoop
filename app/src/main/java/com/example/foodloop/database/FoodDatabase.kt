
package com.example.foodloop.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Food::class, ShoppingItem::class],
    version = 4,
    exportSchema = false
)
abstract class FoodDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao

    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {

        @Volatile
        private var INSTANCE: FoodDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        checked INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(db: SupportSQLiteDatabase) {

                var hasPhotoPath = false

                db.query("PRAGMA table_info(foods)").use { cursor ->

                    val nameIndex =
                        cursor.getColumnIndexOrThrow("name")

                    while (cursor.moveToNext()) {

                        if (cursor.getString(nameIndex) == "photoPath") {
                            hasPhotoPath = true
                        }
                    }
                }

                if (!hasPhotoPath) {

                    db.execSQL(
                        "ALTER TABLE foods ADD COLUMN photoPath TEXT"
                    )
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    ALTER TABLE foods
                    ADD COLUMN userId TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE shopping_items
                    ADD COLUMN userId TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): FoodDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FoodDatabase::class.java,
                    "foodloop_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4
                    )
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
