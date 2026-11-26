package com.library.visitorsystem

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.library.visitorsystem.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var creatingAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser != null) {
            openVisitorForm()
            return
        }

        binding.submitButton.setOnClickListener { authenticate() }
        binding.toggleModeButton.setOnClickListener {
            creatingAccount = !creatingAccount
            binding.submitButton.setText(
                if (creatingAccount) R.string.create_account else R.string.sign_in
            )
            binding.toggleModeButton.setText(
                if (creatingAccount) R.string.sign_in else R.string.create_account
            )
        }
    }

    private fun authenticate() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Enter a valid email address"
            return
        }
        if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            return
        }

        binding.submitButton.isEnabled = false
        val request = if (creatingAccount) {
            auth.createUserWithEmailAndPassword(email, password)
        } else {
            auth.signInWithEmailAndPassword(email, password)
        }
        request.addOnCompleteListener { task ->
            binding.submitButton.isEnabled = true
            if (task.isSuccessful) {
                openVisitorForm()
            } else {
                Toast.makeText(
                    this,
                    task.exception?.localizedMessage ?: "Authentication failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openVisitorForm() {
        startActivity(Intent(this, VisitorFormActivity::class.java))
        finish()
    }
}
