package net.abaresults.progresspath.view.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.databinding.ItemProgressEntryBinding

class ItemProgressAdapter : RecyclerView.Adapter<ItemProgressAdapter.ViewHolder>() {

    private var items: List<Pair<String, String>> = emptyList()

    fun updateData(newItems: List<Pair<String, String>>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemProgressEntryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgressEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (value, date) = items[position]
        holder.binding.progressValue.text = value
        holder.binding.progressDate.text = date
    }

    override fun getItemCount() = items.size
}