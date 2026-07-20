package app.springdata.coordinator.view.clinics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.R
import app.springdata.coordinator.model.Clinic

class ClinicsAdapter(
    val onItemClicked: (Clinic) -> Unit,
    val onItemEdit: (Clinic) -> Unit,
    val onItemRemove: (Clinic) -> Unit
) :
    RecyclerView.Adapter<ClinicsAdapter.ViewHolder>() {

    private var items: MutableList<Clinic> = mutableListOf()

    fun updateData(newItems: List<Clinic>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.nameView)
        val buttonsLayout: LinearLayout = view.findViewById<LinearLayout>(R.id.buttons_layout)
        val removeView: ImageView = view.findViewById(R.id.removeView)
        val editView: ImageView = view.findViewById(R.id.editView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.nameView.text = items[position].name
        holder.itemView.setOnClickListener {
            if (holder.buttonsLayout.isVisible == false) {
                onItemClicked(items[position])
            } else {
                holder.buttonsLayout.isVisible = false
            }
        }
        holder.buttonsLayout.isVisible = false

        holder.itemView.setOnLongClickListener { it: View? ->
            holder.buttonsLayout.isVisible = true
            true
        }

        holder.removeView.setOnClickListener {
            onItemRemove(items[position])
        }

        holder.editView.setOnClickListener {
            onItemEdit(items[position])
        }
    }

    override fun getItemCount() = items.size
}
