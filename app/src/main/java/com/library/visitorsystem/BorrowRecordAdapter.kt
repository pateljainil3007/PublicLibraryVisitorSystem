package com.library.visitorsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.library.visitorsystem.databinding.ItemBorrowRecordBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BorrowRecordAdapter : RecyclerView.Adapter<BorrowRecordAdapter.BorrowViewHolder>() {
    private val records = mutableListOf<BorrowRecord>()
    var listener: Listener? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    interface Listener {
        fun onReturnBook(record: BorrowRecord)
    }

    fun submitList(items: List<BorrowRecord>) {
        records.clear()
        records.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BorrowViewHolder {
        val binding = ItemBorrowRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BorrowViewHolder(binding, dateFormat)
    }

    override fun onBindViewHolder(holder: BorrowViewHolder, position: Int) {
        holder.bind(records[position], listener)
    }

    override fun getItemCount() = records.size

    class BorrowViewHolder(
        private val binding: ItemBorrowRecordBinding,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: BorrowRecord, listener: Listener?) {
            binding.bookTitleText.text = record.bookTitle
            binding.borrowerNameText.text = "Borrower: ${record.visitorName} (${record.visitorPhone})"

            val borrowedStr = dateFormat.format(Date(record.borrowedAt))
            val dueStr = dateFormat.format(Date(record.dueDate))
            binding.datesText.text = "Borrowed: $borrowedStr • Due: $dueStr (${record.durationDays} days)"

            val context = binding.root.context

            if (record.status == "RETURNED") {
                val returnedStr = if (record.returnedAt > 0) dateFormat.format(Date(record.returnedAt)) else "Yes"
                binding.statusBadge.text = "Returned on $returnedStr"
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
                binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                binding.returnBookButton.visibility = View.GONE
                binding.fineAlertText.visibility = View.GONE
            } else if (record.isOverdue()) {
                val daysOverdue = record.getDaysOverdue()
                val fine = record.getCalculatedFine()
                binding.statusBadge.text = "OVERDUE ($daysOverdue days)"
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.status_red_bg))
                binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_red_text))
                binding.fineAlertText.visibility = View.VISIBLE
                binding.fineAlertText.text = String.format(Locale.US, "Late Fine Accrued: $%.2f", fine)
                binding.returnBookButton.visibility = View.VISIBLE
            } else {
                binding.statusBadge.text = "ACTIVE LOAN"
                binding.statusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.status_green_bg))
                binding.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_green_text))
                binding.returnBookButton.visibility = View.VISIBLE
                binding.fineAlertText.visibility = View.GONE
            }

            binding.returnBookButton.setOnClickListener { listener?.onReturnBook(record) }
        }
    }
}