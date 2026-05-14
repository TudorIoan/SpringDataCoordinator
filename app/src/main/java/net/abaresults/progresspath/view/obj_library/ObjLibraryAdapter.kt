package net.abaresults.progresspath.view.obj_library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.databinding.ObjectiveItemBinding
import net.abaresults.progresspath.databinding.ObjectiveTypeItemBinding
import net.abaresults.progresspath.model.Objective
import net.abaresults.progresspath.model.ObjectiveType
import net.abaresults.progresspath.model.UserType

sealed class ObjLibraryListItem {
    data class ObjectiveTypeItem(val objectiveType: ObjectiveType, val expanded: Boolean = false) : ObjLibraryListItem()
    data class ObjectiveItem(val objective: Objective) : ObjLibraryListItem()
}

class ObjLibraryAdapter(
    val onItemClicked: (ObjLibraryListItem) -> Unit,
    val onItemEdit: (Objective) -> Unit,
    val onItemRemove: (Objective) -> Unit
) : RecyclerView.Adapter<ObjLibraryAdapter.BaseViewHolder<*>>() {

    var userType = UserType.COORDINATOR

    private var items: List<ObjLibraryListItem> = emptyList()

    fun set(newItems: List<ObjLibraryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    class ObjectiveTypeViewHolder(
        private val binding: ObjectiveTypeItemBinding,
        private val onItemClicked: (ObjLibraryListItem.ObjectiveTypeItem) -> Unit,
    ) : BaseViewHolder<ObjLibraryListItem.ObjectiveTypeItem>(binding.root) {

        override fun bind(item: ObjLibraryListItem.ObjectiveTypeItem) {
            binding.textView.text = item.objectiveType.displayName

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    class ObjectiveViewHolder(
        private val binding: ObjectiveItemBinding,
        private val onItemClicked: (ObjLibraryListItem.ObjectiveItem) -> Unit,
        private val onItemEdit: (Objective) -> Unit,
        private val onItemRemove: (Objective) -> Unit,
        private val userType: UserType
    ) : BaseViewHolder<ObjLibraryListItem.ObjectiveItem>(binding.root) {

        override fun bind(item: ObjLibraryListItem.ObjectiveItem) {
            binding.nameView.text = item.objective.name

            // Hide buttons layout initially
            binding.buttonsLayout.root.isVisible = false

            binding.root.setOnClickListener {
                if (binding.buttonsLayout.root.isVisible == false) {
                    onItemClicked(item)
                } else {
                    binding.buttonsLayout.root.isVisible = false
                }
            }

            binding.root.setOnLongClickListener {
                if (userType == UserType.COORDINATOR) {
                    binding.buttonsLayout.root.isVisible = true
                    true
                } else false
            }

            // Get references to the buttons from the included layout
            val removeView = binding.buttonsLayout.root.findViewById<View>(net.abaresults.progresspath.R.id.removeView)
            val editView = binding.buttonsLayout.root.findViewById<View>(net.abaresults.progresspath.R.id.editView)

            removeView.setOnClickListener {
                onItemRemove(item.objective)
            }

            editView.setOnClickListener {
                onItemEdit(item.objective)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ObjLibraryListItem.ObjectiveTypeItem -> VIEW_TYPE_TYPE
            is ObjLibraryListItem.ObjectiveItem -> VIEW_TYPE_OBJECTIVE
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
                ObjectiveViewHolder(binding, onItemClicked, onItemEdit, onItemRemove, userType)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        val item = items[position]
        when (holder) {
            is ObjectiveTypeViewHolder -> holder.bind(item as ObjLibraryListItem.ObjectiveTypeItem)
            is ObjectiveViewHolder -> holder.bind(item as ObjLibraryListItem.ObjectiveItem)
        }
    }

    override fun getItemCount() = items.size

    companion object {
        private const val VIEW_TYPE_TYPE = 1
        private const val VIEW_TYPE_OBJECTIVE = 2
    }
}