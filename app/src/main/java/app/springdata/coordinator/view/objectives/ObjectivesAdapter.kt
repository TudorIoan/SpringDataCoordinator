package app.springdata.coordinator.view.objectives

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.databinding.ObjectiveItemBinding
import app.springdata.coordinator.databinding.ObjectiveTypeItemBinding
import app.springdata.coordinator.model.KidObjective
import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.ObjectiveType

sealed class ObjectivesListItem {
    data class ObjectiveTypeItem(val objectiveType: ObjectiveType, val expanded: Boolean = false) : ObjectivesListItem()
    data class ObjectiveItem(val kidObjective: KidObjective, val objectiveName: String, val objectiveType: ObjectiveType) : ObjectivesListItem()
}

class ObjectivesAdapter(
    val onItemClicked: (ObjectivesListItem) -> Unit,
    val onToggleClicked: (KidObjective, Boolean) -> Unit,
    val onGenerateObjectiveReport: (KidObjective, String) -> Unit,
    val onItemRemove: (KidObjective) -> Unit,
    val onItemEdit: (KidObjective, String) -> Unit
) : RecyclerView.Adapter<ObjectivesAdapter.BaseViewHolder<*>>() {


    private var items: List<ObjectivesListItem> = emptyList()

    fun set(newItems: List<ObjectivesListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    class ObjectiveTypeViewHolder(
        private val binding: ObjectiveTypeItemBinding,
        private val onItemClicked: (ObjectivesListItem.ObjectiveTypeItem) -> Unit,
    ) : BaseViewHolder<ObjectivesListItem.ObjectiveTypeItem>(binding.root) {

        override fun bind(item: ObjectivesListItem.ObjectiveTypeItem) {
            binding.textView.text = item.objectiveType.displayName

            // Set level label text and background color
            binding.levelLabel.text = item.objectiveType.level.displayName.uppercase()
            binding.levelLabel.isVisible = true

            // Create rounded background with appropriate color
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f
                setColor(getLevelBackgroundColor(item.objectiveType.level))
            }
            binding.levelLabel.background = backgroundDrawable

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }

        private fun getLevelBackgroundColor(level: ObjLevel): Int {
            return when (level) {
                ObjLevel.BEGINNER -> 0xFF4CAF50.toInt() // Green
                ObjLevel.INTERMEDIATE -> 0xFFe3dc12.toInt() // Darker shade of yellow
                ObjLevel.ADVANCED -> 0xFF2196F3.toInt() // Blue
            }
        }
    }

    class ObjectiveViewHolder(
        private val binding: ObjectiveItemBinding,
        private val onItemClicked: (ObjectivesListItem.ObjectiveItem) -> Unit,
        private val onToggleClicked: (KidObjective, Boolean) -> Unit,
        private val onGenerateObjectiveReport: (KidObjective, String) -> Unit,
        private val onItemRemove: (KidObjective) -> Unit,
        private val onItemEdit: (KidObjective, String) -> Unit
    ) : BaseViewHolder<ObjectivesListItem.ObjectiveItem>(binding.root) {

        override fun bind(item: ObjectivesListItem.ObjectiveItem) {
            binding.nameView.text = item.objectiveName

            binding.activeToggle.isVisible = true
            binding.objectiveReportView.isVisible = item.kidObjective.active
            binding.activeToggle.setOnCheckedChangeListener(null)
            binding.activeToggle.isChecked = item.kidObjective.active

            // Set toggle listener
            binding.activeToggle.setOnCheckedChangeListener { _, isChecked ->
                onToggleClicked(item.kidObjective, isChecked)
            }

            // Hide buttons layout initially
            binding.buttonsLayout.root.isVisible = false

            binding.root.setOnClickListener {
                if (binding.buttonsLayout.root.isVisible == false) {
                    onItemClicked(item)
                } else {
                    binding.buttonsLayout.root.isVisible = false
                    binding.activeToggle.isVisible = true
                    binding.objectiveReportView.isVisible = item.kidObjective.active
                }
            }

            // Set long click listener to show buttons layout
            binding.root.setOnLongClickListener {
                binding.buttonsLayout.root.isVisible = true
                binding.activeToggle.isVisible = false
                binding.objectiveReportView.isVisible = false
                true
            }

            // Set click listener for objective report
            binding.objectiveReportView.setOnClickListener {
                onGenerateObjectiveReport(item.kidObjective, item.objectiveName)
            }

            // Get references to the buttons from the included layout
            val removeView = binding.buttonsLayout.root.findViewById<View>(app.springdata.coordinator.R.id.removeView)
            val editView = binding.buttonsLayout.root.findViewById<View>(app.springdata.coordinator.R.id.editView)

            editView.isVisible = true

            // Set click listener for remove button
            removeView.setOnClickListener {
                // Remove kidObjective (not the master objective)
                onItemRemove(item.kidObjective)
            }

            editView.setOnClickListener {
                binding.buttonsLayout.root.isVisible = false
                binding.activeToggle.isVisible = true
                binding.objectiveReportView.isVisible = item.kidObjective.active
                onItemEdit(item.kidObjective, item.objectiveName)
            }

            // Gray out objective with all items mastered
            if (item.kidObjective.active && item.kidObjective.itemsList.all { it.mastered }) {
                binding.root.alpha = 0.5f
            } else {
                binding.root.alpha = 1.0f
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ObjectivesListItem.ObjectiveTypeItem -> VIEW_TYPE_TYPE
            is ObjectivesListItem.ObjectiveItem -> VIEW_TYPE_OBJECTIVE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TYPE -> {
                val binding = ObjectiveTypeItemBinding.inflate(inflater, parent, false)
                ObjectiveTypeViewHolder(binding, onItemClicked)
            }
            VIEW_TYPE_OBJECTIVE -> {
                val binding = ObjectiveItemBinding.inflate(inflater, parent, false)
                ObjectiveViewHolder(binding, onItemClicked, onToggleClicked, onGenerateObjectiveReport, onItemRemove, onItemEdit)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        val item = items[position]
        when (holder) {
            is ObjectiveTypeViewHolder -> holder.bind(item as ObjectivesListItem.ObjectiveTypeItem)
            is ObjectiveViewHolder -> holder.bind(item as ObjectivesListItem.ObjectiveItem)
        }
    }

    override fun getItemCount() = items.size

    companion object {
        private const val VIEW_TYPE_TYPE = 1
        private const val VIEW_TYPE_OBJECTIVE = 2
    }
}
