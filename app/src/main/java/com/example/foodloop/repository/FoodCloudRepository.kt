package com.example.foodloop.repository

import com.example.foodloop.database.Food
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FoodCloudRepository {

    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    private fun getCollection() =
        db.collection("users")
            .document(
                requireNotNull(auth.currentUser?.uid)
            )
            .collection("foods")

    fun saveFood(
        cloudId: String,
        food: Food,
        onResult: (Boolean, String) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null || food.userId != uid) {
            onResult(false, "Nicht angemeldet")
            return
        }

        val data = hashMapOf(
            "name" to food.name,
            "quantity" to food.quantity,
            "unit" to food.unit,
            "category" to food.category,
            "location" to food.location,
            "expirationDate" to food.expirationDate,
            "userId" to uid
        )

        getCollection()
            .document(cloudId)
            .set(data)
            .addOnSuccessListener {
                onResult(true, "Gespeichert")
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Fehler"
                )
            }
    }

    fun deleteFood(
        cloudId: String,
        onResult: (Boolean, String) -> Unit
    ) {

        getCollection()
            .document(cloudId)
            .delete()
            .addOnSuccessListener {
                onResult(true, "Gelöscht")
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Fehler"
                )
            }
    }

    fun observeFoods(
        onResult: (List<Pair<String, Food>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        val uid = requireNotNull(
            auth.currentUser?.uid
        )

        return getCollection()
            .orderBy("expirationDate")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (auth.currentUser?.uid != uid) {
                    return@addSnapshotListener
                }

                val foods = snapshot?.documents
                    ?.mapNotNull { document ->

                        val name =
                            document.getString("name")

                        val quantity =
                            document.getDouble("quantity")

                        if (name == null || quantity == null) {

                            null

                        } else {

                            val food = Food(
                                name = name,
                                quantity = quantity,
                                unit = document.getString("unit") ?: "",
                                category = document.getString("category") ?: "",
                                location = document.getString("location") ?: "",
                                expirationDate =
                                    document.getString("expirationDate") ?: "",
                                userId = uid
                            )

                            Pair(document.id, food)
                        }
                    } ?: emptyList()

                onResult(foods)
            }
    }
}
