package com.library.visitorsystem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.library.visitorsystem.databinding.ItemBookBinding

class BookAdapter : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
    private val books = mutableListOf<Book>()
    var listener: Listener? = null

    interface Listener {
        fun onIssueBook(book: Book)
        fun onDeleteBook(book: Book)
    }

    fun submitList(items: List<Book>) {
        books.clear()
        books.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position], listener)
    }

    override fun getItemCount() = books.size

    class BookViewHolder(private val binding: ItemBookBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book, listener: Listener?) {
            binding.bookTitleText.text = book.title
            binding.authorText.text = "by ${book.author}"
            binding.isbnText.text = "ISBN: ${book.isbn}"
            binding.categoryChip.text = book.category
            binding.shelfText.text = book.shelfLocation

            val context = binding.root.context
            if (book.availableCopies > 0) {
                binding.copiesText.text = "${book.availableCopies} / ${book.totalCopies} Available"
                binding.copiesText.setTextColor(ContextCompat.getColor(context, R.color.status_green_text))
                binding.issueBookButton.isEnabled = true
            } else {
                binding.copiesText.text = "All Copies Issued (0/${book.totalCopies})"
                binding.copiesText.setTextColor(ContextCompat.getColor(context, R.color.status_red_text))
                binding.issueBookButton.isEnabled = false
            }

            binding.issueBookButton.setOnClickListener { listener?.onIssueBook(book) }
            binding.deleteBookButton.setOnClickListener { listener?.onDeleteBook(book) }
        }
    }
}