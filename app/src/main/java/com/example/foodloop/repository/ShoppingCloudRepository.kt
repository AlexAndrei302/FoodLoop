package com.example.foodloop.repository

import com.example.foodloop.database.ShoppingItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ShoppingCloudRepository {

    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    private fun getCollection() =
        db.collection("users")
            .document(
                requireNotNull(auth.currentUser?.uid)
            )
            .collection("shopping_items")

    fun saveItem(
        cloudId: String,
        item: ShoppingItem,
        onResult: (Boolean, String) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null || item.userId != uid) {
            onResult(false, "Nicht angemeldet")
            return
        }

        val data = hashMapOf(
            "name" to item.name,
            "checked" to item.checked,
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

    fun deleteItem(
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

    fun observeItems(
        onResult: (List<Pair<String, ShoppingItem>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        val uid = requireNotNull(
            auth.currentUser?.uid
        )

        return getCollection()
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (auth.currentUser?.uid != uid) {
                    return@addSnapshotListener
                }

                val items = snapshot?.documents
                    ?.mapNotNull { document ->

                        val name =
                            document.getString("name")

                        if (name == null) {

                            null

                        } else {

                            val item = ShoppingItem(
                                name = name,
                                checked =
                                    document.getBoolean("checked") ?: false,
                                userId = uid
                            )

                            Pair(document.id, item)
                        }
                    }
                    ?.sortedBy { it.second.checked }
                    ?: emptyList()

                onResult(items)
            }
    }
}
