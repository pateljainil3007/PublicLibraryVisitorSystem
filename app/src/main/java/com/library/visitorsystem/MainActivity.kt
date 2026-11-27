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
import com.google.firebase.firestore.FirebaseFirestore
import com.library.visitorsystem.databinding.ActivityMainBinding
import com.library.visitorsystem.databinding.FragmentBooksBinding
import com.library.visitorsystem.databinding.FragmentBorrowingsBinding
import com.library.visitorsystem.databinding.FragmentDashboardBinding
import com.library.visitorsystem.databinding.FragmentVisitorsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var allVisitors = listOf<Visitor>()
    private var allBooks = listOf<Book>()
    private var allBorrowRecords = listOf<BorrowRecord>()

    private var dashboardBinding: FragmentDashboardBinding? = null
    private var visitorsBinding: FragmentVisitorsBinding? = null
    private var booksBinding: FragmentBooksBinding? = null
    private var borrowingsBinding: FragmentBorrowingsBinding? = null

    private val visitorAdapter = VisitorAdapter()
    private val bookAdapter = BookAdapter()
    private val borrowAdapter = BorrowRecordAdapter()
    private val overdueAdapter = BorrowRecordAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        SampleDataSeeder.seedIfEmpty()

        setupBottomNavigation()
        observeData()

        // Default to Dashboard tab
        selectTab(R.id.navigation_dashboard)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            selectTab(item.itemId)
            true
        }
    }

    private fun selectTab(itemId: Int) {
        binding.fragmentContainer.removeAllViews()

        when (itemId) {
            R.id.navigation_dashboard -> {
                if (dashboardBinding == null) {
                    dashboardBinding = FragmentDashboardBinding.inflate(layoutInflater)
                    setupDashboardTab()
                }
                binding.fragmentContainer.addView(dashboardBinding!!.root)
                renderDashboard()
            }
            R.id.navigation_visitors -> {
                if (visitorsBinding == null) {
                    visitorsBinding = FragmentVisitorsBinding.inflate(layoutInflater)
                    setupVisitorsTab()
                }
                binding.fragmentContainer.addView(visitorsBinding!!.root)
                renderVisitors()
            }
            R.id.navigation_books -> {
                if (booksBinding == null) {
                    booksBinding = FragmentBooksBinding.inflate(layoutInflater)
                    setupBooksTab()
                }
                binding.fragmentContainer.addView(booksBinding!!.root)
                renderBooks()
            }
            R.id.navigation_borrowings -> {
                if (borrowingsBinding == null) {
                    borrowingsBinding = FragmentBorrowingsBinding.inflate(layoutInflater)
                    setupBorrowingsTab()
                }
                binding.fragmentContainer.addView(borrowingsBinding!!.root)
                renderBorrowings()
            }
        }
    }

    private fun observeData() {
        // 1. Observe Visitors
        firestore.collection("visitors")
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null) return@addSnapshotListener
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
                updateAllUI()
            }

        // 2. Observe Books
        firestore.collection("books")
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null) return@addSnapshotListener
                allBooks = snapshot?.documents.orEmpty().map { doc ->
                    Book(
                        id = doc.id,
                        title = doc.getString("title").orEmpty(),
                        author = doc.getString("author").orEmpty(),
                        isbn = doc.getString("isbn").orEmpty(),
                        category = doc.getString("category").orEmpty().ifEmpty { "General" },
                        shelfLocation = doc.getString("shelfLocation").orEmpty().ifEmpty { "Shelf A-01" },
                        totalCopies = doc.getLong("totalCopies")?.toInt() ?: 1,
                        availableCopies = doc.getLong("availableCopies")?.toInt() ?: 1,
                        addedAt = doc.getLong("addedAt") ?: 0L
                    )
                }
                updateAllUI()
            }

        // 3. Observe Borrow Records
        firestore.collection("borrow_records")
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null) return@addSnapshotListener
                allBorrowRecords = snapshot?.documents.orEmpty().map { doc ->
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
                updateAllUI()
            }
    }

    private fun updateAllUI() {
        renderDashboard()
        renderVisitors()
        renderBooks()
        renderBorrowings()
    }

    // ==================== DASHBOARD TAB ====================
    private fun setupDashboardTab() {
        val b = dashboardBinding ?: return
        b.overdueRecyclerView.layoutManager = LinearLayoutManager(this)
        b.overdueRecyclerView.adapter = overdueAdapter
        overdueAdapter.listener = object : BorrowRecordAdapter.Listener {
            override fun onReturnBook(record: BorrowRecord) = returnBook(record)
        }

        b.dashboardSignOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        b.quickCheckInButton.setOnClickListener {
            startActivity(Intent(this, VisitorFormActivity::class.java))
        }

        b.quickIssueBookButton.setOnClickListener {
            LibraryDialogs.showIssueBookDialog(this, firestore)
        }
    }

    private fun renderDashboard() {
        val b = dashboardBinding ?: return

        val dateStr = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US).format(Date())
        b.dateSubtitleText.text = dateStr

        val visitorsInsideCount = allVisitors.count { it.status != "Checked out" }
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val visitorsTodayCount = allVisitors.count { v ->
            if (v.checkedInAt == 0L) false else SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(v.checkedInAt)) == todayStr
        }
        b.visitorsCountText.text = visitorsInsideCount.toString()
        b.visitorsSubtext.text = "$visitorsTodayCount visitor(s) checked in today"

        val activeLoans = allBorrowRecords.filter { it.status == "ACTIVE" }
        b.activeLoansCountText.text = activeLoans.size.toString()

        val overdueLoans = activeLoans.filter { it.isOverdue() }
        b.overdueCountText.text = overdueLoans.size.toString()

        val totalBooksCount = allBooks.sumOf { it.totalCopies }
        val availableBooksCount = allBooks.sumOf { it.availableCopies }
        b.totalBooksCountText.text = totalBooksCount.toString()
        b.availableCopiesSubtext.text = "$availableBooksCount available to borrow"

        if (overdueLoans.isEmpty()) {
            b.noOverdueText.visibility = View.VISIBLE
            b.overdueRecyclerView.visibility = View.GONE
        } else {
            b.noOverdueText.visibility = View.GONE
            b.overdueRecyclerView.visibility = View.VISIBLE
            overdueAdapter.submitList(overdueLoans)
        }
    }

    // ==================== VISITORS TAB ====================
    private fun setupVisitorsTab() {
        val b = visitorsBinding ?: return
        b.visitorsRecyclerView.layoutManager = LinearLayoutManager(this)
        b.visitorsRecyclerView.adapter = visitorAdapter

        visitorAdapter.listener = object : VisitorAdapter.Listener {
            override fun onCheckOut(visitor: Visitor) = checkOutVisitor(visitor)
            override fun onDelete(visitor: Visitor) = confirmDeleteVisitor(visitor)
            override fun onIssueBook(visitor: Visitor) {
                LibraryDialogs.showIssueBookDialog(this@MainActivity, firestore, preselectedVisitorId = visitor.id)
            }
            override fun onViewHistory(visitor: Visitor) {
                LibraryDialogs.showVisitorHistoryDialog(this@MainActivity, firestore, visitor)
            }
        }

        b.addVisitorFabButton.setOnClickListener {
            startActivity(Intent(this, VisitorFormActivity::class.java))
        }

        b.visitorSearchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderVisitors() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.visitorFilterChipGroup.setOnCheckedStateChangeListener { _, _ -> renderVisitors() }
    }

    private fun renderVisitors() {
        val b = visitorsBinding ?: return

        // Compute active borrow count for each visitor
        val activeLoansPerVisitor = allBorrowRecords.filter { it.status == "ACTIVE" }.groupBy { it.visitorId }
        val enrichedVisitors = allVisitors.map { visitor ->
            visitor.copy(activeBorrowCount = activeLoansPerVisitor[visitor.id]?.size ?: 0)
        }

        val query = b.visitorSearchInput.text?.toString()?.trim()?.lowercase(Locale.US).orEmpty()
        var filtered = if (query.isEmpty()) enrichedVisitors else enrichedVisitors.filter {
            listOf(it.name, it.phone, it.email, it.address, it.purpose)
                .any { text -> text.lowercase(Locale.US).contains(query) }
        }

        when (b.visitorFilterChipGroup.checkedChipId) {
            R.id.chipCheckedIn -> filtered = filtered.filter { it.status != "Checked out" }
            R.id.chipCheckedOut -> filtered = filtered.filter { it.status == "Checked out" }
            R.id.chipBorrowingBooks -> filtered = filtered.filter { it.activeBorrowCount > 0 }
        }

        visitorAdapter.submitList(filtered)
        b.visitorsEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ==================== BOOKS TAB ====================
    private fun setupBooksTab() {
        val b = booksBinding ?: return
        b.booksRecyclerView.layoutManager = LinearLayoutManager(this)
        b.booksRecyclerView.adapter = bookAdapter

        bookAdapter.listener = object : BookAdapter.Listener {
            override fun onIssueBook(book: Book) {
                LibraryDialogs.showIssueBookDialog(this@MainActivity, firestore, preselectedBookId = book.id)
            }
            override fun onDeleteBook(book: Book) = confirmDeleteBook(book)
        }

        b.addBookButton.setOnClickListener {
            LibraryDialogs.showAddBookDialog(this, firestore)
        }

        b.bookSearchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderBooks() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.categoryFilterChipGroup.setOnCheckedStateChangeListener { _, _ -> renderBooks() }
    }

    private fun renderBooks() {
        val b = booksBinding ?: return
        val query = b.bookSearchInput.text?.toString()?.trim()?.lowercase(Locale.US).orEmpty()

        var filtered = if (query.isEmpty()) allBooks else allBooks.filter {
            listOf(it.title, it.author, it.isbn, it.category, it.shelfLocation)
                .any { text -> text.lowercase(Locale.US).contains(query) }
        }

        when (b.categoryFilterChipGroup.checkedChipId) {
            R.id.chipFiction -> filtered = filtered.filter { it.category.equals("Fiction", ignoreCase = true) }
            R.id.chipTech -> filtered = filtered.filter { it.category.equals("Technology", ignoreCase = true) }
            R.id.chipClassics -> filtered = filtered.filter { it.category.equals("Classics", ignoreCase = true) }
            R.id.chipAvailableOnly -> filtered = filtered.filter { it.availableCopies > 0 }
        }

        bookAdapter.submitList(filtered)
        b.booksEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ==================== BORROWINGS TAB ====================
    private fun setupBorrowingsTab() {
        val b = borrowingsBinding ?: return
        b.borrowRecyclerView.layoutManager = LinearLayoutManager(this)
        b.borrowRecyclerView.adapter = borrowAdapter

        borrowAdapter.listener = object : BorrowRecordAdapter.Listener {
            override fun onReturnBook(record: BorrowRecord) = returnBook(record)
        }

        b.issueBookFabButton.setOnClickListener {
            LibraryDialogs.showIssueBookDialog(this, firestore)
        }

        b.borrowSearchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderBorrowings() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        b.borrowFilterChipGroup.setOnCheckedStateChangeListener { _, _ -> renderBorrowings() }
    }

    private fun renderBorrowings() {
        val b = borrowingsBinding ?: return
        val query = b.borrowSearchInput.text?.toString()?.trim()?.lowercase(Locale.US).orEmpty()

        var filtered = if (query.isEmpty()) allBorrowRecords else allBorrowRecords.filter {
            listOf(it.visitorName, it.visitorPhone, it.bookTitle, it.bookAuthor, it.status)
                .any { text -> text.lowercase(Locale.US).contains(query) }
        }

        when (b.borrowFilterChipGroup.checkedChipId) {
            R.id.chipActiveBorrowings -> filtered = filtered.filter { it.status == "ACTIVE" }
            R.id.chipOverdueBorrowings -> filtered = filtered.filter { it.status == "ACTIVE" && it.isOverdue() }
            R.id.chipReturnedBorrowings -> filtered = filtered.filter { it.status == "RETURNED" }
        }

        borrowAdapter.submitList(filtered)
        b.borrowEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    // ==================== ACTIONS ====================
    private fun returnBook(record: BorrowRecord) {
        AlertDialog.Builder(this)
            .setTitle("Return Borrowed Book")
            .setMessage("Confirm return of \"${record.bookTitle}\" borrowed by ${record.visitorName}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Return Book") { _, _ ->
                val batch = firestore.batch()
                val recordRef = firestore.collection("borrow_records").document(record.id)
                batch.update(recordRef, mapOf(
                    "status" to "RETURNED",
                    "returnedAt" to System.currentTimeMillis()
                ))

                // Increment available book copies
                val bookRef = firestore.collection("books").document(record.bookId)
                val bookDoc = allBooks.find { it.id == record.bookId }
                if (bookDoc != null) {
                    val newAvailable = (bookDoc.availableCopies + 1).coerceAtMost(bookDoc.totalCopies)
                    batch.update(bookRef, "availableCopies", newAvailable)
                }

                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "\"${record.bookTitle}\" marked as returned!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error returning book: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }.show()
    }

    private fun checkOutVisitor(visitor: Visitor) {
        firestore.collection("visitors").document(visitor.id)
            .update(mapOf("status" to "Checked out", "checkedOutAt" to System.currentTimeMillis()))
            .addOnSuccessListener { Toast.makeText(this, "${visitor.name} checked out", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Could not check out visitor", Toast.LENGTH_LONG).show() }
    }

    private fun confirmDeleteVisitor(visitor: Visitor) {
        AlertDialog.Builder(this)
            .setTitle("Delete Visitor Record")
            .setMessage("Permanently delete visitor record for ${visitor.name}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                firestore.collection("visitors").document(visitor.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Visitor record deleted", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Could not delete record", Toast.LENGTH_LONG).show() }
            }.show()
    }

    private fun confirmDeleteBook(book: Book) {
        AlertDialog.Builder(this)
            .setTitle("Delete Book")
            .setMessage("Remove \"${book.title}\" from the library catalog?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                firestore.collection("books").document(book.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Book removed from catalog", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(this, "Could not delete book", Toast.LENGTH_LONG).show() }
            }.show()
    }
}