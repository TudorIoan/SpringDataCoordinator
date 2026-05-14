package net.abaresults.progresspath.view.clinics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import net.abaresults.progresspath.R
import net.abaresults.progresspath.model.Clinic
import net.abaresults.progresspath.model.UserType

class ClinicsAdapter(
    val onItemClicked: (Clinic) -> Unit,
    val onItemEdit: (Clinic) -> Unit,
    val onItemRemove: (Clinic) -> Unit
) :
    RecyclerView.Adapter<ClinicsAdapter.ViewHolder>() {

    var userType = UserType.COORDINATOR
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
            if (userType == UserType.COORDINATOR) {
                holder.buttonsLayout.isVisible = true
                true
            } else false
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
