package com.library.visitorsystem

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val isbn: String = "",
    val category: String = "General",
    val shelfLocation: String = "Shelf A-1",
    val totalCopies: Int = 1,
    val availableCopies: Int = 1,
    val addedAt: Long = 0L
)