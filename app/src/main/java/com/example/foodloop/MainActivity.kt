
package com.example.foodloop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.foodloop.auth.LoginActivity
import com.example.foodloop.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private val auth = FirebaseAuth.getInstance()

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

        findViewById<Button>(R.id.btn_pantry)
            .setOnClickListener {
                openFragment(PantryFragment())
            }

        findViewById<Button>(R.id.btn_recipes)
            .setOnClickListener {
                openFragment(RecipesFragment())
            }

        findViewById<Button>(R.id.btn_shopping)
            .setOnClickListener {
                openFragment(ShoppingListFragment())
            }

        findViewById<ImageButton>(R.id.btn_profile)
            .setOnClickListener {
                showProfileDialog()
            }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showProfileDialog() {

        val currentUser = auth.currentUser

        val email = currentUser?.email ?: "Unbekannt"

        AlertDialog.Builder(this)
            .setTitle("Mein Profil")
            .setMessage("E-Mail: $email")
            .setPositiveButton("Abmelden") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("Schließen", null)
            .show()
    }

    private fun logoutUser() {

        authRepository.logout()

        openLoginActivity()
    }

    private fun openLoginActivity() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
