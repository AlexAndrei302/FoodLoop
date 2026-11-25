
package com.example.foodloop.repository

import com.example.foodloop.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun register(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user!!.uid

                val user = User(
                    uid = uid,
                    name = name,
                    email = email,
                    createdAt = System.currentTimeMillis()
                )

                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        onResult(true, "Registrierung erfolgreich")
                    }
                    .addOnFailureListener { error ->
                        onResult(
                            false,
                            "Konto erstellt, Profil konnte nicht gespeichert werden: " +
                                    (error.message ?: "Unbekannter Fehler")
                        )
                    }
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.message ?: "Registrierung fehlgeschlagen"
                )
            }
    }

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onResult(true, "Anmeldung erfolgreich")
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.message ?: "Anmeldung fehlgeschlagen"
                )
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun resetPassword(
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onResult(
                    true,
                    "Wenn ein Konto existiert, wurde eine E-Mail gesendet."
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    false,
                    error.message ?: "Fehler beim Zurücksetzen des Passworts"
                )
            }
    }

}
