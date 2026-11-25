
package com.example.foodloop.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodloop.R
import com.example.foodloop.repository.AuthRepository

class RegisterActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initializeViews()

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun registerUser() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            etName.error = "Bitte Namen eingeben"
            etName.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ungültige E-Mail-Adresse"
            etEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Mindestens 6 Zeichen"
            etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwörter stimmen nicht überein"
            etConfirmPassword.requestFocus()
            return
        }

        setLoading(true)

        authRepository.register(name, email, password) { success, message ->

            setLoading(false)

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()

            if (success) {
                authRepository.logout()
                finish()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {

        progressBar.visibility = if (isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        btnRegister.isEnabled = !isLoading
        etName.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
        tvLogin.isEnabled = !isLoading
    }
}
