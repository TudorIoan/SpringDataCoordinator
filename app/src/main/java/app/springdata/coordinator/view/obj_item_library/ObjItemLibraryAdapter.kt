package app.springdata.coordinator.view.obj_item_library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.ObjItemLibraryItemBinding
import app.springdata.coordinator.model.ObjItem

class ObjItemLibraryAdapter(
    private val onItemEdit: (ObjItem) -> Unit,
    private val onItemRemove: (ObjItem) -> Unit
) : RecyclerView.Adapter<ObjItemLibraryAdapter.ViewHolder>() {

    private var items: MutableList<ObjItem> = mutableListOf()

    fun updateData(newItems: List<ObjItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ObjItemLibraryItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ObjItemLibraryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.binding.itemNameView.text = item.name
        holder.binding.itemTypeView.text = item.type.displayName

        // Hide buttons layout initially
        holder.binding.buttonsLayout.root.isVisible = false

        holder.binding.root.setOnClickListener {
            if (holder.binding.buttonsLayout.root.isVisible == false) {
                // Just show item details or do nothing
            } else {
                holder.binding.buttonsLayout.root.isVisible = false
            }
        }

        holder.binding.root.setOnLongClickListener {
            holder.binding.buttonsLayout.root.isVisible = true
            true
        }

        // Get references to the buttons from the included layout
        val removeView = holder.binding.buttonsLayout.root.findViewById<View>(R.id.removeView)
        val editView = holder.binding.buttonsLayout.root.findViewById<View>(R.id.editView)

        removeView.setOnClickListener {
            onItemRemove(item)
        }

        editView.setOnClickListener {
            onItemEdit(item)
        }
    }

    override fun getItemCount() = items.size
}