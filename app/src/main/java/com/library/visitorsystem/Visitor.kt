package com.library.visitorsystem

data class Visitor(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val dob: String = "",
    val email: String = "",
    val address: String = "",
    val purpose: String = "",
    val checkedInAt: Long = 0L,
    val checkedOutAt: Long = 0L,
    val status: String = "Checked in",
    val activeBorrowCount: Int = 0
)