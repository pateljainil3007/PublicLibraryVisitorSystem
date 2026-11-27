package com.library.visitorsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.library.visitorsystem.databinding.ItemVisitorEnhancedBinding

class VisitorAdapter : RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder>() {
    private val visitors = mutableListOf<Visitor>()
    var listener: Listener? = null

    interface Listener {
        fun onCheckOut(visitor: Visitor)
        fun onDelete(visitor: Visitor)
        fun onIssueBook(visitor: Visitor)
        fun onViewHistory(visitor: Visitor)
    }

    fun submitList(items: List<Visitor>) {
        visitors.clear()
        visitors.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitorViewHolder {
        val binding = ItemVisitorEnhancedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitorViewHolder, position: Int) {
        holder.bind(visitors[position], listener)
    }

    override fun getItemCount() = visitors.size

    class VisitorViewHolder(private val binding: ItemVisitorEnhancedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(visitor: Visitor, listener: Listener?) {
            val context = binding.root.context
            binding.visitorNameText.text = visitor.name
            binding.visitorContactText.text = "Phone: ${visitor.phone}  •  Email: ${visitor.email}"
            binding.visitorPurposeText.text = "Purpose: ${visitor.purpose.ifBlank { "Reading / General Visit" }}"

            if (visitor.status == "Checked out") {
                binding.visitorStatusBadge.text = "Checked Out"
                binding.visitorStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.status_amber_bg))
                binding.visitorStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_amber_text))
                binding.checkOutVisitorButton.visibility = View.GONE
            } else {
                binding.visitorStatusBadge.text = "Inside Library"
                binding.visitorStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.status_green_bg))
                binding.visitorStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_green_text))
                binding.checkOutVisitorButton.visibility = View.VISIBLE
            }

            if (visitor.activeBorrowCount > 0) {
                binding.borrowBadgeText.visibility = View.VISIBLE
                binding.borrowBadgeText.text = "📖 ${visitor.activeBorrowCount} Book(s) Currently Borrowed"
            } else {
                binding.borrowBadgeText.visibility = View.GONE
            }

            binding.checkOutVisitorButton.setOnClickListener { listener?.onCheckOut(visitor) }
            binding.deleteVisitorButton.setOnClickListener { listener?.onDelete(visitor) }
            binding.issueBookToVisitorButton.setOnClickListener { listener?.onIssueBook(visitor) }
            binding.historyButton.setOnClickListener { listener?.onViewHistory(visitor) }
        }
    }
}