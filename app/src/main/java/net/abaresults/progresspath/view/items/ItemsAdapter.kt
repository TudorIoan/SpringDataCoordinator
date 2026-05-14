package net.abaresults.progresspath.view.items

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.ItemItemBinding
import net.abaresults.progresspath.model.KidObjectiveItem
import net.abaresults.progresspath.model.ObjItemType
import net.abaresults.progresspath.model.UserType
import java.text.SimpleDateFormat
import java.util.Locale

class ItemsAdapter(
    private val onToggleClicked: (KidObjectiveItem) -> Unit,
    private val onYesClicked: (KidObjectiveItem) -> Unit,
    private val onNoClicked: (KidObjectiveItem) -> Unit,
    private val onFrequencySet: (KidObjectiveItem, Int) -> Unit,
    private val onProgressSet: (KidObjectiveItem, Int) -> Unit,
    private val onShowProgress: (KidObjectiveItem) -> Unit,
    private val onSetMastered: (KidObjectiveItem) -> Unit,
    private val onCheckmarkClicked: (KidObjectiveItem) -> Unit
) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {

    var userType: UserType = UserType.COORDINATOR
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

        // Show/hide controls based on user type
        val isTherapist = userType == UserType.THERAPIST
        val isCoordinator = userType == UserType.COORDINATOR

        // Show item type only for coordinator
        holder.binding.itemTypeView.text = item.objItem.type.displayName
        holder.binding.itemTypeView.visibility = if (isCoordinator) View.VISIBLE else View.GONE

        // Show appropriate controls based on user type and item type
        val isYesNoItem = item.objItem.type == ObjItemType.YES_NO
        val isFrequencyItem = item.objItem.type == ObjItemType.FREQUENCY
        val isCheckmarkItem = item.objItem.type == ObjItemType.CHECKMARK
        val isPercentageItem = item.objItem.type == ObjItemType.PERCENTAGE

        holder.binding.yesNoButtonsLayout.visibility = if (isTherapist && item.active && !item.mastered && isYesNoItem) View.VISIBLE else View.GONE
        holder.binding.frequencyInputLayout.visibility = if (isTherapist && item.active && !item.mastered && isFrequencyItem) View.VISIBLE else View.GONE
        holder.binding.percentageInputLayout.visibility = if (isTherapist && item.active && !item.mastered && isPercentageItem) View.VISIBLE else View.GONE
        holder.binding.checkmarkYesButton.visibility = if (isTherapist && item.active && !item.mastered && isCheckmarkItem) View.VISIBLE else View.GONE
        holder.binding.showProgressButton.visibility = if (isCoordinator) View.VISIBLE else View.GONE
        holder.binding.setMasteredButton.visibility = View.GONE
        holder.binding.setMasteredButton.text = if (item.mastered) "Set Un-mastered" else "Set Mastered"
        holder.binding.switchToggle.visibility = if ((isCoordinator && !item.mastered) || (isTherapist && !item.active && !item.mastered)) View.VISIBLE else View.GONE
        holder.binding.masteredImageView.visibility = if ((isCoordinator || isTherapist) && item.mastered) View.VISIBLE else View.GONE

        holder.binding.switchToggle.setOnCheckedChangeListener(null) // Prevent triggering listener
        holder.binding.switchToggle.isChecked = item.active

        // For coordinator show item progress so far
        val lastYesNo = item.yesNoList.lastOrNull()
        if (isCoordinator && lastYesNo != null) {
            val yesNo = if (lastYesNo.yes) context.getString(R.string.yes) else context.getString(R.string.no)
            val dateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())
            val formattedDate = dateFormat.format(item.lastResponseTime!!)
            holder.binding.lastModificationView.text = Html.fromHtml(context.getString(R.string.item_last_answer, yesNo, formattedDate), Html.FROM_HTML_MODE_LEGACY)
            holder.binding.therapistView.text = Html.fromHtml(context.getString(R.string.item_by_therapist, item.lastModificationByUserName!!), Html.FROM_HTML_MODE_LEGACY)
        }
        holder.binding.lastModificationView.isVisible = isCoordinator && lastYesNo != null
        holder.binding.therapistView.isVisible = isCoordinator && lastYesNo != null

        holder.binding.yesButton.setOnClickListener {
            onYesClicked(item)
        }

        holder.binding.noButton.setOnClickListener {
            onNoClicked(item)
        }

        holder.binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            item.active = isChecked
            onToggleClicked(item)
        }

        holder.binding.setButton.setOnClickListener {
            val frequencyText = holder.binding.frequencyEditText.text.toString()
            val frequency = frequencyText.toIntOrNull() ?: 0
            onFrequencySet(item, frequency)
        }

        holder.binding.setPercentageButton.setOnClickListener {
            val progressText = holder.binding.percentageEditText.text.toString()
            val progress = progressText.toIntOrNull()?.coerceIn(0, 100) ?: 0
            onProgressSet(item, progress)
        }

        holder.binding.showProgressButton.setOnClickListener {
            onShowProgress(item)
        }

        holder.binding.setMasteredButton.setOnClickListener {
            onSetMastered(item)
        }

        holder.binding.checkmarkYesButton.setOnClickListener {
            onCheckmarkClicked(item)
        }

        // Long click listener for non-mastered items (frequency, checkmark, progress, and yes/no)
        holder.binding.root.setOnLongClickListener {
            if (isCoordinator) {
                // Hide normal controls
                holder.binding.switchToggle.visibility = View.GONE
                holder.binding.showProgressButton.visibility = View.GONE
                holder.binding.masteredImageView.visibility = View.GONE
                // Show SetMastered button
                holder.binding.setMasteredButton.visibility = View.VISIBLE
                true
            } else {
                false
            }
        }

        // Regular click to hide Set Mastered button if it's visible
        holder.binding.root.setOnClickListener {
            if (holder.binding.setMasteredButton.isVisible) {
                // Hide Set Mastered button and show normal controls
                holder.binding.setMasteredButton.visibility = View.GONE
                if (isCoordinator) {
                    holder.binding.showProgressButton.visibility = View.VISIBLE
                    if (item.mastered) {
                        holder.binding.masteredImageView.visibility = View.VISIBLE
                        holder.binding.switchToggle.visibility = View.GONE
                    } else {
                        holder.binding.switchToggle.visibility = View.VISIBLE
                    }
                }
            }
        }

    }

    override fun getItemCount() = items.size
}
