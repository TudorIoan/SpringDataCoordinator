package app.springdata.coordinator.view.kids

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.databinding.ListItemBinding
import app.springdata.coordinator.model.Kid

class KidsAdapter(
    val onItemClicked: (Kid) -> Unit,
    val onItemEdit: (Kid) -> Unit,
    val onItemRemove: (Kid) -> Unit,
) :
    RecyclerView.Adapter<KidsAdapter.ViewHolder>() {

    private var items: MutableList<Kid> = mutableListOf()

    fun updateData(newItems: List<Kid>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.nameView.text = items[position].name

        // Hide buttons layout initially
        holder.binding.buttonsLayout.root.isVisible = false

        holder.binding.worksheetBtn.isVisible = false

        holder.binding.root.setOnClickListener {
            if (holder.binding.buttonsLayout.root.isVisible == false) {
                onItemClicked(items[position])
            } else {
                holder.binding.buttonsLayout.root.isVisible = false
            }
        }

        holder.binding.root.setOnLongClickListener { it: View? ->
            holder.binding.buttonsLayout.root.isVisible = true
            true
        }

        // Get references to the buttons from the included layout using binding
        holder.binding.buttonsLayout.removeView.setOnClickListener {
            onItemRemove(items[position])
        }

        holder.binding.buttonsLayout.editView.setOnClickListener {
            onItemEdit(items[position])
        }
    }

    override fun getItemCount() = items.size
}
