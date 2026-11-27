
package com.example.foodloop

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.lifecycle.lifecycleScope
import com.example.foodloop.auth.LoginActivity
import com.example.foodloop.repository.AuthRepository
import com.example.foodloop.notifications.NotificationHelper
import com.example.foodloop.notifications.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    private val auth = FirebaseAuth.getInstance()

    private var isLoggingOut = false

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted && authRepository.isLoggedIn()) {

                NotificationScheduler.testNow(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (!authRepository.isLoggedIn()) {

            openLoginActivity()

            return
        }

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {

            openFragment(PantryFragment())
        }


        val bottomNavigation =
            findViewById<BottomNavigationView>(
                R.id.bottom_navigation
            )

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.btn_pantry -> {

                    openFragment(PantryFragment())

                    true
                }

                R.id.btn_recipes -> {

                    openFragment(RecipesFragment())

                    true
                }

                R.id.btn_shopping -> {

                    openFragment(ShoppingListFragment())

                    true
                }

                else -> false
            }
        }

        bottomNavigation.setOnItemReselectedListener {
            // Nu reîncărcăm fragmentul deja deschis.
        }


        findViewById<ImageButton>(R.id.btn_profile)
            .setOnClickListener {

                showProfileDialog()
            }

        setupNotifications()
    }

    private fun setupNotifications() {

        NotificationHelper.createChannel(this)

        NotificationScheduler.schedule(this)

        if (Build.VERSION.SDK_INT >= 33) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun openFragment(fragment: Fragment) {

        if (isLoggingOut) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment
            )
            .commit()
    }

    private fun showProfileDialog() {

        if (isLoggingOut) {
            return
        }

        val currentUser = auth.currentUser

        val email = currentUser?.email ?: "Unbekannt"

        val name = currentUser?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")

        val isGoogleUser =
            currentUser?.providerData?.any {

                it.providerId == "google.com"

            } == true

        val loginMethod = if (isGoogleUser) {

            "Google"

        } else {

            "E-Mail"
        }

        AlertDialog.Builder(this)

            .setTitle("Mein Profil")

            .setMessage(
                "Name: $name\n\n" +
                        "E-Mail: $email\n\n" +
                        "Anmeldemethode: $loginMethod"
            )

            .setPositiveButton("Abmelden") { _, _ ->

                logoutUser()
            }

            .setNegativeButton("Schließen", null)

            .show()
    }

    private fun logoutUser() {

        if (isLoggingOut) {
            return
        }

        isLoggingOut = true

        authRepository.logout()

        NotificationScheduler.cancel(this)

        getSystemService(
            NotificationManager::class.java
        ).cancelAll()

        lifecycleScope.launch {

            try {

                CredentialManager.create(this@MainActivity)
                    .clearCredentialState(
                        ClearCredentialStateRequest()
                    )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                Log.e(
                    "FoodLoop",
                    "Fehler bei der Google-Abmeldung",
                    e
                )
            }

            openLoginActivity()
        }
    }

    private fun openLoginActivity() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}
