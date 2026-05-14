package net.abaresults.progresspath.view.therapists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.databinding.TherapistItemBinding

class TherapistAdapter(
    val onItemClicked: (TherapistVM) -> Unit,
    val onItemRemove: (TherapistVM) -> Unit
) :
    RecyclerView.Adapter<TherapistAdapter.ViewHolder>() {

    private var items: MutableList<TherapistVM> = mutableListOf()

    fun updateData(newItems: List<TherapistVM>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: TherapistItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TherapistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.nameView.text = items[position].name
        holder.binding.emailView.text = items[position].email

        // Hide buttons layout initially
        holder.binding.buttonsLayout.root.isVisible = false

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

        // Only show remove button (hide edit button for therapists)
        holder.binding.buttonsLayout.editView.isVisible = false

        holder.binding.buttonsLayout.removeView.setOnClickListener {
            onItemRemove(items[position])
        }
    }

    override fun getItemCount() = items.size
}
