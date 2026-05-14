package net.abaresults.progresspath.view.objectives.add_objective

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.R
import net.abaresults.progresspath.model.Objective

class AvailableObjectivesAdapter(
    private val onObjectiveClicked: (Objective) -> Unit
) : RecyclerView.Adapter<AvailableObjectivesAdapter.ViewHolder>() {

    private var objectives: List<Objective> = emptyList()

    fun setObjectives(newObjectives: List<Objective>) {
        objectives = newObjectives
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.available_objective_item, parent, false)
        return ViewHolder(view, onObjectiveClicked)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(objectives[position])
    }

    override fun getItemCount(): Int = objectives.size

    class ViewHolder(
        itemView: View,
        private val onObjectiveClicked: (Objective) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val objectiveNameTextView: TextView = itemView.findViewById(R.id.objectiveNameTextView)

        fun bind(objective: Objective) {
            objectiveNameTextView.text = objective.name
            itemView.setOnClickListener {
                onObjectiveClicked(objective)
            }
        }
    }
}
