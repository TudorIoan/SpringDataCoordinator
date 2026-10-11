package app.springdata.coordinator.view.clinics

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.R
import app.springdata.coordinator.model.Clinic

class ClinicsAdapter(
    val onItemClicked: (Clinic) -> Unit,
    val onItemEdit: (Clinic) -> Unit,
    val onItemRemove: (Clinic) -> Unit
) : RecyclerView.Adapter<ClinicsAdapter.ViewHolder>() {

    private var items: MutableList<Clinic> = mutableListOf()

    fun updateData(newItems: List<Clinic>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.nameView)
        val initialsView: TextView = view.findViewById(R.id.initialsView)
        val buttonsLayout: LinearLayout = view.findViewById(R.id.buttons_layout)
        val removeView: ImageView = view.findViewById(R.id.removeView)
        val editView: ImageView = view.findViewById(R.id.editView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.clinic_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val clinic = items[position]
        holder.nameView.text = clinic.name
        holder.initialsView.text = initials(clinic.name)
        holder.initialsView.background = initialsBackground(holder.itemView)
        holder.initialsView.setTextColor(generatedInitialsColor(clinic.name))
        holder.buttonsLayout.isVisible = false

        holder.itemView.setOnClickListener {
            if (!holder.buttonsLayout.isVisible) {
                onItemClicked(clinic)
            } else {
                holder.buttonsLayout.isVisible = false
            }
        }

        holder.itemView.setOnLongClickListener {
            holder.buttonsLayout.isVisible = true
            true
        }

        holder.removeView.setOnClickListener {
            onItemRemove(clinic)
        }

        holder.editView.setOnClickListener {
            onItemEdit(clinic)
        }
    }

    override fun getItemCount() = items.size

    private fun initials(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}"
            words.size == 1 -> words[0].take(2)
            else -> "?"
        }.uppercase()
    }

    private fun initialsBackground(view: View): GradientDrawable {
        val radius = view.resources.getDimension(R.dimen.icon_corner_radius)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(ContextCompat.getColor(view.context, R.color.itemInitialsBackground))
        }
    }

    private fun generatedInitialsColor(name: String): Int {
        val hue = (name.hashCode() and Int.MAX_VALUE) % 360
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.76f, 0.76f))
    }
}
