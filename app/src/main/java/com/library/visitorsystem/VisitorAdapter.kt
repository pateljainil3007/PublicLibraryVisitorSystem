package com.library.visitorsystem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.library.visitorsystem.databinding.ItemVisitorBinding

class VisitorAdapter : RecyclerView.Adapter<VisitorAdapter.VisitorViewHolder>() {
    private val visitors = mutableListOf<Visitor>()

    fun submitList(items: List<Visitor>) {
        visitors.clear()
        visitors.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitorViewHolder {
        val binding = ItemVisitorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VisitorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitorViewHolder, position: Int) {
        holder.bind(visitors[position])
    }

    override fun getItemCount(): Int = visitors.size

    class VisitorViewHolder(private val binding: ItemVisitorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(visitor: Visitor) {
            binding.nameText.text = visitor.name
            binding.contactText.text = "${visitor.phone} | ${visitor.email}"
            binding.detailsText.text = "DOB: ${visitor.dob} | ${visitor.address}"
        }
    }
}
