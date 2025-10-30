package com.rodrigo.googletask_clone.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.rodrigo.googletask_clone.R
import com.rodrigo.googletask_clone.data.Category

class CategoryAdapter(
    private val onCategoryClicked: (Category?) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedPosition = 0 // Posição do item "Todas"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false) as Chip
        return CategoryViewHolder(chip)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // O primeiro item é sempre "Todas"
        if (position == 0) {
            holder.bind("Todas", position == selectedPosition)
        } else {
            val category = getItem(position - 1)
            holder.bind(category.name, position == selectedPosition)
        }
        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)

                val clickedCategory = if (selectedPosition > 0) getItem(selectedPosition - 1) else null
                onCategoryClicked(clickedCategory)
            }
        }
    }

    // Adicionamos 1 para a categoria "Todas"
    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    class CategoryViewHolder(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(categoryName: String, isSelected: Boolean) {
            chip.text = categoryName
            chip.isChecked = isSelected
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}