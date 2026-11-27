package com.library.visitorsystem

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

object SampleDataSeeder {

    fun seedIfEmpty(onComplete: () -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        db.collection("books").get().addOnSuccessListener { bookSnapshot ->
            if (bookSnapshot.isEmpty) {
                seedData(db, onComplete)
            } else {
                onComplete()
            }
        }.addOnFailureListener {
            onComplete()
        }
    }

    private fun seedData(db: FirebaseFirestore, onComplete: () -> Unit) {
        val batch = db.batch()

        // 1. Seed Books
        val books = listOf(
            Book("book_1", "To Kill a Mockingbird", "Harper Lee", "978-0446310789", "Fiction", "Shelf A-04", 5, 4, System.currentTimeMillis()),
            Book("book_2", "1984", "George Orwell", "978-0451524935", "Sci-Fi", "Shelf B-12", 4, 2, System.currentTimeMillis()),
            Book("book_3", "Clean Code", "Robert C. Martin", "978-0132350884", "Technology", "Shelf C-01", 3, 2, System.currentTimeMillis()),
            Book("book_4", "The Great Gatsby", "F. Scott Fitzgerald", "978-0743273565", "Classics", "Shelf A-09", 6, 6, System.currentTimeMillis()),
            Book("book_5", "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "978-0062316097", "History", "Shelf H-05", 4, 3, System.currentTimeMillis()),
            Book("book_6", "Atomic Habits", "James Clear", "978-0735211292", "Self-Help", "Shelf S-02", 5, 3, System.currentTimeMillis())
        )

        books.forEach { book ->
            val docRef = db.collection("books").document(book.id)
            batch.set(docRef, mapOf(
                "title" to book.title,
                "author" to book.author,
                "isbn" to book.isbn,
                "category" to book.category,
                "shelfLocation" to book.shelfLocation,
                "totalCopies" to book.totalCopies,
                "availableCopies" to book.availableCopies,
                "addedAt" to FieldValue.serverTimestamp()
            ))
        }

        // 2. Seed Visitors
        val visitors = listOf(
            Visitor("sample_1", "Aarav Patel", "+1 555 010 1001", "1998-04-12", "aarav.patel@example.com", "12 Maple Street", "Book Borrowing & Reading", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2), 0L, "Checked in"),
            Visitor("sample_2", "Emma Johnson", "+1 555 010 1002", "2001-09-23", "emma.johnson@example.com", "45 Oak Avenue", "Research", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(12), 0L, "Checked in"),
            Visitor("sample_3", "Liam Chen", "+1 555 010 1003", "1995-01-08", "liam.chen@example.com", "8 Cedar Lane", "Book Borrowing", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4), 0L, "Checked in"),
            Visitor("sample_4", "Mia Garcia", "+1 555 010 1004", "2003-06-17", "mia.garcia@example.com", "71 Pine Road", "Study Group", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1), System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), "Checked out"),
            Visitor("sample_5", "Noah Williams", "+1 555 010 1005", "1989-12-30", "noah.williams@example.com", "29 Birch Court", "Computer Use", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), 0L, "Checked in")
        )

        visitors.forEach { visitor ->
            val docRef = db.collection("visitors").document(visitor.id)
            batch.set(docRef, mapOf(
                "name" to visitor.name,
                "phone" to visitor.phone,
                "dob" to visitor.dob,
                "email" to visitor.email,
                "address" to visitor.address,
                "purpose" to visitor.purpose,
                "status" to visitor.status,
                "checkedInAt" to visitor.checkedInAt,
                "checkedOutAt" to visitor.checkedOutAt,
                "createdAt" to FieldValue.serverTimestamp()
            ))
        }

        // 3. Seed Borrowings
        val now = System.currentTimeMillis()
        val borrowings = listOf(
            BorrowRecord(
                id = "borrow_1",
                visitorId = "sample_1",
                visitorName = "Aarav Patel",
                visitorPhone = "+1 555 010 1001",
                bookId = "book_2",
                bookTitle = "1984",
                bookAuthor = "George Orwell",
                borrowedAt = now - TimeUnit.DAYS.toMillis(3),
                durationDays = 14,
                dueDate = now + TimeUnit.DAYS.toMillis(11),
                returnedAt = 0L,
                status = "ACTIVE"
            ),
            BorrowRecord(
                id = "borrow_2",
                visitorId = "sample_2",
                visitorName = "Emma Johnson",
                visitorPhone = "+1 555 010 1002",
                bookId = "book_3",
                bookTitle = "Clean Code",
                bookAuthor = "Robert C. Martin",
                borrowedAt = now - TimeUnit.DAYS.toMillis(12),
                durationDays = 7,
                dueDate = now - TimeUnit.DAYS.toMillis(5),
                returnedAt = 0L,
                status = "ACTIVE" // Overdue!
            ),
            BorrowRecord(
                id = "borrow_3",
                visitorId = "sample_3",
                visitorName = "Liam Chen",
                visitorPhone = "+1 555 010 1003",
                bookId = "book_5",
                bookTitle = "Sapiens: A Brief History of Humankind",
                bookAuthor = "Yuval Noah Harari",
                borrowedAt = now - TimeUnit.DAYS.toMillis(5),
                durationDays = 21,
                dueDate = now + TimeUnit.DAYS.toMillis(16),
                returnedAt = 0L,
                status = "ACTIVE"
            ),
            BorrowRecord(
                id = "borrow_4",
                visitorId = "sample_4",
                visitorName = "Mia Garcia",
                visitorPhone = "+1 555 010 1004",
                bookId = "book_6",
                bookTitle = "Atomic Habits",
                bookAuthor = "James Clear",
                borrowedAt = now - TimeUnit.DAYS.toMillis(10),
                durationDays = 7,
                dueDate = now - TimeUnit.DAYS.toMillis(3),
                returnedAt = now - TimeUnit.HOURS.toMillis(5),
                status = "RETURNED"
            )
        )

        borrowings.forEach { record ->
            val docRef = db.collection("borrow_records").document(record.id)
            batch.set(docRef, mapOf(
                "visitorId" to record.visitorId,
                "visitorName" to record.visitorName,
                "visitorPhone" to record.visitorPhone,
                "bookId" to record.bookId,
                "bookTitle" to record.bookTitle,
                "bookAuthor" to record.bookAuthor,
                "borrowedAt" to record.borrowedAt,
                "durationDays" to record.durationDays,
                "dueDate" to record.dueDate,
                "returnedAt" to record.returnedAt,
                "status" to record.status
            ))
        }

        batch.commit().addOnCompleteListener { onComplete() }
    }
}