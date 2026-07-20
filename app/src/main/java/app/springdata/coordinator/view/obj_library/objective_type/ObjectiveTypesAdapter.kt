package app.springdata.coordinator.view.obj_library.objective_type

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.databinding.ItemObjectiveTypeBinding

data class ObjectiveType(
    val id: Long,
    val name: String,
    val level: String
)

class ObjectiveTypesAdapter(
    private val onDeleteClick: (Long) -> Unit
) : RecyclerView.Adapter<ObjectiveTypesAdapter.ViewHolder>() {

    private var objectiveTypes: List<ObjectiveType> = emptyList()

    fun updateObjectiveTypes(newObjectiveTypes: List<ObjectiveType>) {
        objectiveTypes = newObjectiveTypes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemObjectiveTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(objectiveTypes[position])
    }

    override fun getItemCount(): Int = objectiveTypes.size

    inner class ViewHolder(private val binding: ItemObjectiveTypeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(objectiveType: ObjectiveType) {
            binding.objectiveTypeNameTextView.text = objectiveType.name
            binding.deleteButton.setOnClickListener {
                onDeleteClick(objectiveType.id)
            }
        }
    }
}