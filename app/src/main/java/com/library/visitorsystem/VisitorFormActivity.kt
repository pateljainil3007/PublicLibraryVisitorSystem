package com.library.visitorsystem

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.library.visitorsystem.databinding.ActivityVisitorFormBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class VisitorFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVisitorFormBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisitorFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.dobInput.setOnClickListener { showDatePicker() }
        binding.saveVisitorButton.setOnClickListener { saveVisitor() }
        binding.viewVisitorsButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showDatePicker() {
        val today = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply { set(year, month, day) }
                binding.dobInput.setText(dateFormat.format(selected.time))
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.maxDate = today.timeInMillis }.show()
    }

    private fun saveVisitor() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()
        val dob = binding.dobInput.text?.toString()?.trim().orEmpty()
        val email = binding.visitorEmailInput.text?.toString()?.trim().orEmpty()
        val address = binding.addressInput.text?.toString()?.trim().orEmpty()
        val purpose = binding.purposeInput.text?.toString()?.trim().orEmpty()

        if (name.length < 2) {
            binding.nameInput.error = "Enter the visitor's full name"
            return
        }
        if (phone.count(Char::isDigit) < 7) {
            binding.phoneInput.error = "Enter a valid phone number"
            return
        }
        if (dob.isBlank()) {
            binding.dobInput.error = "Select a date of birth"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.visitorEmailInput.error = "Enter a valid email address"
            return
        }
        if (address.length < 5) {
            binding.addressInput.error = "Enter a complete address"
            return
        }
        if (purpose.length < 3) {
            binding.purposeInput.error = "Enter the visit purpose"
            return
        }

        binding.saveVisitorButton.isEnabled = false
        val record = hashMapOf(
            "name" to name,
            "phone" to phone,
            "dob" to dob,
            "email" to email,
            "address" to address,
            "purpose" to purpose,
            "status" to "Checked in",
            "checkedInAt" to System.currentTimeMillis(),
            "checkedOutAt" to 0L,
            "createdBy" to FirebaseAuth.getInstance().currentUser?.uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("visitors")
            .add(record)
            .addOnSuccessListener {
                binding.saveVisitorButton.isEnabled = true
                Toast.makeText(this, "$name checked in successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                binding.saveVisitorButton.isEnabled = true
                Toast.makeText(this, "Could not save visitor: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}