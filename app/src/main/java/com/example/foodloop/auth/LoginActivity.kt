
package com.example.foodloop.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodloop.MainActivity
import com.example.foodloop.R
import com.example.foodloop.repository.AuthRepository

class LoginActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRegister: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (authRepository.isLoggedIn()) {
            openMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        initializeViews()

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvRegister.setOnClickListener {
            val intent = Intent(
                this,
                RegisterActivity::class.java
            )
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.loginProgressBar)
        tvRegister = findViewById(R.id.tvRegister)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ungültige E-Mail-Adresse"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Bitte Passwort eingeben"
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        authRepository.login(email, password) { success, message ->

            setLoading(false)

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()

            if (success) {
                openMainActivity()
            }
        }
    }

    private fun resetPassword() {

        val email = etEmail.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Bitte gültige E-Mail-Adresse eingeben"
            etEmail.requestFocus()
            return
        }

        setLoading(true)

        authRepository.resetPassword(email) { success, message ->

            setLoading(false)

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openMainActivity() {

        val intent = Intent(this, MainActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {

        progressBar.visibility = if (isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        btnLogin.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        tvRegister.isEnabled = !isLoading
        tvForgotPassword.isEnabled = !isLoading
    }
}
