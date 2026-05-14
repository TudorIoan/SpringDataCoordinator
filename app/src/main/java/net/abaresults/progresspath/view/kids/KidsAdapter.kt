package net.abaresults.progresspath.view.kids

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.databinding.ListItemBinding
import net.abaresults.progresspath.model.Kid
import net.abaresults.progresspath.model.UserType

class KidsAdapter(
    val onItemClicked: (Kid) -> Unit,
    val onItemEdit: (Kid) -> Unit,
    val onItemRemove: (Kid) -> Unit,
    val onGenerateKidWorksheet: (Kid) -> Unit,
) :
    RecyclerView.Adapter<KidsAdapter.ViewHolder>() {

    var userType = UserType.COORDINATOR
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

        holder.binding.worksheetBtn.isVisible = userType == UserType.THERAPIST

        holder.binding.root.setOnClickListener {
            if (holder.binding.buttonsLayout.root.isVisible == false) {
                onItemClicked(items[position])
            } else {
                holder.binding.buttonsLayout.root.isVisible = false
            }
        }

        holder.binding.root.setOnLongClickListener { it: View? ->
            if (userType == UserType.COORDINATOR) {
                holder.binding.buttonsLayout.root.isVisible = true
                true
            } else false
        }

        holder.binding.worksheetBtn.setOnClickListener {
            onGenerateKidWorksheet(items[position])
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
