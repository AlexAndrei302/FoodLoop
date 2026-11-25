
package com.example.foodloop.repository

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.foodloop.R
import com.example.foodloop.model.User
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException

class GoogleAuthRepository {

    private val auth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    suspend fun signIn(
        activity: Activity,
        onResult: (Boolean, String) -> Unit
    ) {

        try {

            val credentialManager =
                CredentialManager.create(activity)

            val googleOption =
                GetSignInWithGoogleOption.Builder(
                    activity.getString(
                        R.string.default_web_client_id
                    )
                ).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential

            if (
                credential is CustomCredential &&
                credential.type ==
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleCredential =
                    GoogleIdTokenCredential.createFrom(
                        credential.data
                    )

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )

                auth.signInWithCredential(firebaseCredential)
                    .addOnSuccessListener(activity) { result ->

                        val firebaseUser = result.user

                        if (firebaseUser == null) {

                            onResult(
                                false,
                                "Anmeldung fehlgeschlagen"
                            )

                        } else {

                            val document = db.collection("users")
                                .document(firebaseUser.uid)

                            document.get()
                                .addOnSuccessListener(activity) { snapshot ->

                                    if (snapshot.exists()) {

                                        onResult(
                                            true,
                                            "Anmeldung erfolgreich"
                                        )

                                    } else {

                                        val user = User(
                                            uid = firebaseUser.uid,
                                            name = firebaseUser.displayName ?: "",
                                            email = firebaseUser.email ?: "",
                                            createdAt = System.currentTimeMillis()
                                        )

                                        document.set(user)
                                            .addOnSuccessListener(activity) {

                                                onResult(
                                                    true,
                                                    "Anmeldung erfolgreich"
                                                )
                                            }
                                            .addOnFailureListener(activity) {

                                                auth.signOut()

                                                onResult(
                                                    false,
                                                    "Profil konnte nicht gespeichert werden"
                                                )
                                            }
                                    }
                                }
                                .addOnFailureListener(activity) {

                                    auth.signOut()

                                    onResult(
                                        false,
                                        "Profil konnte nicht geladen werden"
                                    )
                                }
                        }
                    }
                    .addOnFailureListener(activity) {

                        onResult(
                            false,
                            it.message ?: "Google-Anmeldung fehlgeschlagen"
                        )
                    }

            } else {

                onResult(
                    false,
                    "Ungültige Anmeldedaten"
                )
            }

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            onResult(
                false,
                e.message ?: "Google-Anmeldung fehlgeschlagen"
            )
        }
    }
}
