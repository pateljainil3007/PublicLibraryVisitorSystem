package com.library.visitorsystem

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.library.visitorsystem.databinding.ActivityVisitorListBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VisitorListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVisitorListBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val adapter = VisitorAdapter()
    private var allVisitors = listOf<Visitor>()
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
        adapter.listener = object : VisitorAdapter.Listener {
            override fun onCheckOut(visitor: Visitor) = checkOut(visitor)
            override fun onDelete(visitor: Visitor) = confirmDelete(visitor)
            override fun onIssueBook(visitor: Visitor) {
                LibraryDialogs.showIssueBookDialog(this@VisitorListActivity, firestore, preselectedVisitorId = visitor.id)
            }
            override fun onViewHistory(visitor: Visitor) {
                LibraryDialogs.showVisitorHistoryDialog(this@VisitorListActivity, firestore, visitor)
            }
        }

        binding.signOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.newVisitorButton.setOnClickListener {
            startActivity(Intent(this, VisitorFormActivity::class.java))
            finish()
        }
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) { filter(text?.toString().orEmpty()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        observeVisitors()
    }

    private fun observeVisitors() {
        firestore.collection("visitors")
            .orderBy("checkedInAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Could not load visitors: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                allVisitors = snapshot?.documents.orEmpty().map { doc ->
                    Visitor(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        phone = doc.getString("phone").orEmpty(),
                        dob = doc.getString("dob").orEmpty(),
                        email = doc.getString("email").orEmpty(),
                        address = doc.getString("address").orEmpty(),
                        purpose = doc.getString("purpose").orEmpty(),
                        checkedInAt = doc.getLong("checkedInAt") ?: 0L,
                        checkedOutAt = doc.getLong("checkedOutAt") ?: 0L,
                        status = doc.getString("status").orEmpty()
                    )
                }
                updateReport()
                filter(binding.searchInput.text?.toString().orEmpty())

                if (allVisitors.isEmpty() && !seedChecked) {
                    seedChecked = true
                    seedDummyVisitors(showMessage = false)
                }
            }
    }

    private fun filter(query: String) {
        val normalized = query.trim().lowercase(Locale.US)
        val filtered = if (normalized.isEmpty()) allVisitors else allVisitors.filter {
            listOf(it.name, it.phone, it.email, it.address, it.purpose, it.status)
                .any { value -> value.lowercase(Locale.US).contains(normalized) }
        }
        adapter.submitList(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateReport() {
        val checkedIn = allVisitors.count { it.status != "Checked out" }
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val todayCount = allVisitors.count {
            if (it.checkedInAt == 0L) false else SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(it.checkedInAt)) == today
        }
        binding.reportText.text = "Today: $todayCount  •  Currently inside: $checkedIn  •  Total visits: ${allVisitors.size}"
    }

    private fun checkOut(visitor: Visitor) {
        firestore.collection("visitors").document(visitor.id)
            .update(mapOf("status" to "Checked out", "checkedOutAt" to System.currentTimeMillis(), "checkedOutBy" to FirebaseAuth.getInstance().currentUser?.uid))
            .addOnSuccessListener { Toast.makeText(this, "${visitor.name} checked out", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Could not check out visitor", Toast.LENGTH_LONG).show() }
    }

    private fun confirmDelete(visitor: Visitor) {
        AlertDialog.Builder(this)
            .setTitle("Delete visitor record")
            .setMessage("Delete ${visitor.name}'s record permanently?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                firestore.collection("visitors").document(visitor.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Could not delete record", Toast.LENGTH_LONG).show() }
            }.show()
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