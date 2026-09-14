package app.springdata.coordinator.view.kids

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.KidItemBinding
import app.springdata.coordinator.model.Kid

class KidsAdapter(
    val onItemClicked: (Kid) -> Unit,
    val onItemEdit: (Kid) -> Unit,
    val onItemRemove: (Kid) -> Unit,
) : RecyclerView.Adapter<KidsAdapter.ViewHolder>() {

    private var items: MutableList<Kid> = mutableListOf()

    fun updateData(newItems: List<Kid>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: KidItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = KidItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kid = items[position]
        holder.binding.nameView.text = kid.name
        holder.binding.initialsView.text = initials(kid.name)
        holder.binding.initialsView.background = initialsBackground(holder.itemView)
        holder.binding.initialsView.setTextColor(generatedInitialsColor(kid.name))
        holder.binding.buttonsLayout.root.isVisible = false
        holder.binding.worksheetBtn.isVisible = false

        holder.binding.root.setOnClickListener {
            if (!holder.binding.buttonsLayout.root.isVisible) {
                onItemClicked(kid)
            } else {
                holder.binding.buttonsLayout.root.isVisible = false
            }
        }

        holder.binding.root.setOnLongClickListener {
            holder.binding.buttonsLayout.root.isVisible = true
            true
        }

        holder.binding.buttonsLayout.removeView.setOnClickListener {
            onItemRemove(kid)
        }

        holder.binding.buttonsLayout.editView.setOnClickListener {
            onItemEdit(kid)
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
        val radius = 10f * android.content.res.Resources.getSystem().displayMetrics.density
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
