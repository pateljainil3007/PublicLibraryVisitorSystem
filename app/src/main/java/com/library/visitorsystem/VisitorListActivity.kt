package com.library.visitorsystem

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.library.visitorsystem.databinding.ActivityVisitorListBinding

class VisitorListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVisitorListBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val adapter = VisitorAdapter()
    private var seedChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisitorListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.visitorRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.visitorRecyclerView.adapter = adapter
        binding.signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.seedButton.setOnClickListener { seedDummyVisitors() }
        observeVisitors()
    }

    private fun observeVisitors() {
        firestore.collection("visitors")
            .orderBy("name")
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Could not load visitors: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                val visitors = snapshot?.documents.orEmpty().map { document ->
                    Visitor(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        phone = document.getString("phone").orEmpty(),
                        dob = document.getString("dob").orEmpty(),
                        email = document.getString("email").orEmpty(),
                        address = document.getString("address").orEmpty()
                    )
                }
                adapter.submitList(visitors)
                binding.emptyState.visibility =
                    if (visitors.isEmpty()) View.VISIBLE else View.GONE

                if (visitors.isEmpty() && !seedChecked) {
                    seedChecked = true
                    seedDummyVisitors(showMessage = false)
                }
            }
    }

    private fun seedDummyVisitors(showMessage: Boolean = true) {
        val examples = listOf(
            Visitor(
                name = "Aarav Patel",
                phone = "+1 555 010 1001",
                dob = "1998-04-12",
                email = "aarav.patel@example.com",
                address = "12 Maple Street"
            ),
            Visitor(
                name = "Emma Johnson",
                phone = "+1 555 010 1002",
                dob = "2001-09-23",
                email = "emma.johnson@example.com",
                address = "45 Oak Avenue"
            ),
            Visitor(
                name = "Liam Chen",
                phone = "+1 555 010 1003",
                dob = "1995-01-08",
                email = "liam.chen@example.com",
                address = "8 Cedar Lane"
            ),
            Visitor(
                name = "Mia Garcia",
                phone = "+1 555 010 1004",
                dob = "2003-06-17",
                email = "mia.garcia@example.com",
                address = "71 Pine Road"
            ),
            Visitor(
                name = "Noah Williams",
                phone = "+1 555 010 1005",
                dob = "1989-12-30",
                email = "noah.williams@example.com",
                address = "29 Birch Court"
            )
        )

        val batch = firestore.batch()
        examples.forEachIndexed { index, visitor ->
            val document = firestore.collection("visitors").document("sample_${index + 1}")
            batch.set(
                document,
                mapOf(
                    "name" to visitor.name,
                    "phone" to visitor.phone,
                    "dob" to visitor.dob,
                    "email" to visitor.email,
                    "address" to visitor.address,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit()
            .addOnSuccessListener {
                if (showMessage) {
                    Toast.makeText(this, "Sample records added", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Could not seed sample records: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
