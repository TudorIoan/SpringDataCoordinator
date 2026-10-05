package app.springdata.coordinator.view.items

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.ItemItemBinding
import app.springdata.coordinator.model.KidObjectiveItem
import java.text.SimpleDateFormat
import java.util.Locale

class ItemsAdapter(
    private val onToggleClicked: (KidObjectiveItem) -> Unit,
    private val onShowProgress: (KidObjectiveItem) -> Unit,
    private val onSetMastered: (KidObjectiveItem) -> Unit
) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

    private var items: MutableList<KidObjectiveItem> = mutableListOf()

    fun updateData(newItems: List<KidObjectiveItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val item = items[position]

        holder.binding.itemNameView.text = item.objItem.name

        holder.binding.itemTypeView.text = item.objItem.type.displayName
        holder.binding.itemTypeView.visibility = View.VISIBLE

        holder.binding.setMasteredButton.visibility = View.GONE
        holder.binding.actionTray.visibility = View.GONE
        holder.binding.setMasteredButton.text = if (item.mastered) "Set Un-mastered" else "Set Mastered"
        holder.binding.switchToggle.visibility = if (!item.mastered) View.VISIBLE else View.GONE
        holder.binding.masteredImageView.visibility = if (item.mastered) View.VISIBLE else View.GONE

        holder.binding.switchToggle.setOnCheckedChangeListener(null) // Prevent triggering listener
        holder.binding.switchToggle.isChecked = item.active

        // For coordinator show item progress so far
        val lastYesNo = item.yesNoList.lastOrNull()
        val lastResponseTime = item.lastResponseTime ?: lastYesNo?.date
        if (lastYesNo != null && lastResponseTime != null) {
            val yesNo = if (lastYesNo.yes) context.getString(R.string.yes) else context.getString(R.string.no)
            val dateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())
            val formattedDate = dateFormat.format(lastResponseTime)
            val lastModificationByUserName = item.lastModificationByUserName ?: "Unknown"
            holder.binding.lastModificationView.text = Html.fromHtml(context.getString(R.string.item_last_answer, yesNo, formattedDate), Html.FROM_HTML_MODE_LEGACY)
            holder.binding.therapistView.text = Html.fromHtml(context.getString(R.string.item_by_therapist, lastModificationByUserName), Html.FROM_HTML_MODE_LEGACY)
        }
        holder.binding.lastModificationView.isVisible = lastYesNo != null && lastResponseTime != null
        holder.binding.therapistView.isVisible = lastYesNo != null && lastResponseTime != null

        holder.binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            item.active = isChecked
            onToggleClicked(item)
        }


        holder.binding.setMasteredButton.setOnClickListener {
            onSetMastered(item)
        }

        // Long press exposes the Coordinator-only mastery action.
        holder.binding.root.setOnLongClickListener {
            holder.binding.actionTray.visibility = View.VISIBLE
            holder.binding.setMasteredButton.visibility = View.VISIBLE
            true
        }

        // Regular click to hide Set Mastered button if it's visible
        holder.binding.root.setOnClickListener {
            if (holder.binding.setMasteredButton.isVisible) {
                // Hide Set Mastered button and show normal controls
                holder.binding.setMasteredButton.visibility = View.GONE
                holder.binding.actionTray.visibility = View.GONE
                if (item.mastered) {
                    holder.binding.masteredImageView.visibility = View.VISIBLE
                    holder.binding.switchToggle.visibility = View.GONE
                } else {
                    holder.binding.switchToggle.visibility = View.VISIBLE
                }
            } else {
                onShowProgress(item)
            }
        }

    }

    override fun getItemCount() = items.size
}
