package com.library.visitorsystem

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.library.visitorsystem.databinding.DialogAddBookBinding
import com.library.visitorsystem.databinding.DialogIssueBookBinding
import com.library.visitorsystem.databinding.DialogVisitorHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LibraryDialogs {

    fun showAddBookDialog(context: Context, firestore: FirebaseFirestore) {
        val binding = DialogAddBookBinding.inflate(LayoutInflater.from(context))
        val categories = arrayOf("General", "Fiction", "Non-Fiction", "Technology", "Sci-Fi", "Classics", "History", "Self-Help", "Reference")
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        binding.categorySpinner.setAdapter(categoryAdapter)
        binding.categorySpinner.setText(categories[1], false)

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        binding.cancelBookButton.setOnClickListener { dialog.dismiss() }
        binding.saveBookButton.setOnClickListener {
            val title = binding.bookTitleInput.text?.toString()?.trim().orEmpty()
            val author = binding.bookAuthorInput.text?.toString()?.trim().orEmpty()
            val isbn = binding.bookIsbnInput.text?.toString()?.trim().orEmpty()
            val categoryStr = binding.categorySpinner.text?.toString()?.trim().orEmpty()
            val category = if (categoryStr.isBlank()) "General" else categoryStr
            val shelfStr = binding.shelfInput.text?.toString()?.trim().orEmpty()
            val shelf = if (shelfStr.isBlank()) "Shelf A-01" else shelfStr
            val copiesStr = binding.totalCopiesInput.text?.toString()?.trim().orEmpty()
            val totalCopies = copiesStr.toIntOrNull() ?: 1

            if (title.isBlank()) {
                binding.bookTitleInput.error = "Enter book title"
                return@setOnClickListener
            }
            if (author.isBlank()) {
                binding.bookAuthorInput.error = "Enter author name"
                return@setOnClickListener
            }

            binding.saveBookButton.isEnabled = false
            val newBook = hashMapOf(
                "title" to title,
                "author" to author,
                "isbn" to if (isbn.isBlank()) "N/A" else isbn,
                "category" to category,
                "shelfLocation" to shelf,
                "totalCopies" to totalCopies,
                "availableCopies" to totalCopies,
                "addedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("books").add(newBook)
                .addOnSuccessListener {
                    Toast.makeText(context, "\"$title\" added to catalog!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    binding.saveBookButton.isEnabled = true
                    Toast.makeText(context, "Failed to add book: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        dialog.show()
    }

    fun showIssueBookDialog(
        context: Context,
        firestore: FirebaseFirestore,
        preselectedVisitorId: String? = null,
        preselectedBookId: String? = null
    ) {
        val binding = DialogIssueBookBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        val durationOptions = arrayOf("7 Days", "14 Days", "21 Days", "30 Days")
        val durationDaysMap = mapOf("7 Days" to 7, "14 Days" to 14, "21 Days" to 21, "30 Days" to 30)
        val durationAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, durationOptions)
        binding.durationDropdown.setAdapter(durationAdapter)
        binding.durationDropdown.setText(durationOptions[1], false)

        val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US)
        fun updateDueDateText() {
            val selectedText = binding.durationDropdown.text.toString()
            val days = durationDaysMap[selectedText] ?: 14
            val dueMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            binding.dueDatePreviewText.text = "Due Date: ${dateFormat.format(Date(dueMs))}"
        }
        updateDueDateText()
        binding.durationDropdown.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateDueDateText() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        var visitorList = listOf<Visitor>()
        var bookList = listOf<Book>()

        firestore.collection("visitors").get().addOnSuccessListener { vSnapshot ->
            visitorList = vSnapshot.documents.map { doc ->
                Visitor(
                    id = doc.id,
                    name = doc.getString("name").orEmpty(),
                    phone = doc.getString("phone").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    status = doc.getString("status").orEmpty()
                )
            }
            val vNames = visitorList.map { "${it.name} (${it.phone})" }.toTypedArray()
            val vAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, vNames)
            binding.visitorDropdown.setAdapter(vAdapter)

            if (preselectedVisitorId != null) {
                val found = visitorList.find { it.id == preselectedVisitorId }
                if (found != null) {
                    binding.visitorDropdown.setText("${found.name} (${found.phone})", false)
                }
            } else if (visitorList.isNotEmpty()) {
                binding.visitorDropdown.setText(vNames[0], false)
            }
        }

        firestore.collection("books").get().addOnSuccessListener { bSnapshot ->
            bookList = bSnapshot.documents.map { doc ->
                Book(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    author = doc.getString("author").orEmpty(),
                    isbn = doc.getString("isbn").orEmpty(),
                    availableCopies = doc.getLong("availableCopies")?.toInt() ?: 0,
                    totalCopies = doc.getLong("totalCopies")?.toInt() ?: 1
                )
            }.filter { it.availableCopies > 0 }

            val bTitles = bookList.map { "${it.title} by ${it.author} (${it.availableCopies} left)" }.toTypedArray()
            val bAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, bTitles)
            binding.bookDropdown.setAdapter(bAdapter)

            if (preselectedBookId != null) {
                val found = bookList.find { it.id == preselectedBookId }
                if (found != null) {
                    binding.bookDropdown.setText("${found.title} by ${found.author} (${found.availableCopies} left)", false)
                }
            } else if (bookList.isNotEmpty()) {
                binding.bookDropdown.setText(bTitles[0], false)
            }
        }

        binding.cancelIssueButton.setOnClickListener { dialog.dismiss() }
        binding.confirmIssueButton.setOnClickListener {
            val selectedVisitorText = binding.visitorDropdown.text.toString()
            val selectedBookText = binding.bookDropdown.text.toString()
            val selectedDurationText = binding.durationDropdown.text.toString()
            val days = durationDaysMap[selectedDurationText] ?: 14

            val selectedVisitor = visitorList.find { "${it.name} (${it.phone})" == selectedVisitorText }
            val selectedBook = bookList.find { "${it.title} by ${it.author} (${it.availableCopies} left)" == selectedBookText }

            if (selectedVisitor == null) {
                Toast.makeText(context, "Please select a visitor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedBook == null) {
                Toast.makeText(context, "Please select a book", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.confirmIssueButton.isEnabled = false
            val now = System.currentTimeMillis()
            val dueMs = now + TimeUnit.DAYS.toMillis(days.toLong())

            val record = hashMapOf(
                "visitorId" to selectedVisitor.id,
                "visitorName" to selectedVisitor.name,
                "visitorPhone" to selectedVisitor.phone,
                "bookId" to selectedBook.id,
                "bookTitle" to selectedBook.title,
                "bookAuthor" to selectedBook.author,
                "borrowedAt" to now,
                "durationDays" to days,
                "dueDate" to dueMs,
                "returnedAt" to 0L,
                "status" to "ACTIVE"
            )

            val batch = firestore.batch()
            val newBorrowRef = firestore.collection("borrow_records").document()
            batch.set(newBorrowRef, record)

            val bookRef = firestore.collection("books").document(selectedBook.id)
            val newAvailable = (selectedBook.availableCopies - 1).coerceAtLeast(0)
            batch.update(bookRef, "availableCopies", newAvailable)

            batch.commit().addOnSuccessListener {
                Toast.makeText(context, "\"${selectedBook.title}\" issued to ${selectedVisitor.name} for $days days!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }.addOnFailureListener { e ->
                binding.confirmIssueButton.isEnabled = true
                Toast.makeText(context, "Failed to issue book: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }

    fun showVisitorHistoryDialog(context: Context, firestore: FirebaseFirestore, visitor: Visitor) {
        val binding = DialogVisitorHistoryBinding.inflate(LayoutInflater.from(context))
        binding.historyVisitorTitle.text = "Borrowing History: ${visitor.name}"
        binding.historyVisitorSubtitle.text = "Phone: ${visitor.phone}  •  Email: ${visitor.email}"

        val adapter = BorrowRecordAdapter()
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.historyRecyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        binding.closeHistoryButton.setOnClickListener { dialog.dismiss() }

        firestore.collection("borrow_records")
            .whereEqualTo("visitorId", visitor.id)
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.map { doc ->
                    BorrowRecord(
                        id = doc.id,
                        visitorId = doc.getString("visitorId").orEmpty(),
                        visitorName = doc.getString("visitorName").orEmpty(),
                        visitorPhone = doc.getString("visitorPhone").orEmpty(),
                        bookId = doc.getString("bookId").orEmpty(),
                        bookTitle = doc.getString("bookTitle").orEmpty(),
                        bookAuthor = doc.getString("bookAuthor").orEmpty(),
                        borrowedAt = doc.getLong("borrowedAt") ?: 0L,
                        durationDays = doc.getLong("durationDays")?.toInt() ?: 14,
                        dueDate = doc.getLong("dueDate") ?: 0L,
                        returnedAt = doc.getLong("returnedAt") ?: 0L,
                        status = doc.getString("status").orEmpty()
                    )
                }
                adapter.submitList(records)
                binding.historyEmptyText.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
            }

        dialog.show()
    }
}