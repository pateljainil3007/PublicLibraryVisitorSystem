package com.library.visitorsystem

import java.util.concurrent.TimeUnit

data class BorrowRecord(
    val id: String = "",
    val visitorId: String = "",
    val visitorName: String = "",
    val visitorPhone: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val borrowedAt: Long = System.currentTimeMillis(),
    val durationDays: Int = 14,
    val dueDate: Long = 0L,
    val returnedAt: Long = 0L,
    val status: String = "ACTIVE"
) {
    fun isOverdue(): Boolean {
        if (status == "RETURNED") return false
        val now = System.currentTimeMillis()
        return dueDate > 0 && now > dueDate
    }

    fun getDaysOverdue(): Long {
        if (!isOverdue()) return 0L
        val diffMs = System.currentTimeMillis() - dueDate
        return TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(1)
    }

    fun getCalculatedFine(): Double {
        val days = getDaysOverdue()
        return days * 0.50
    }
}