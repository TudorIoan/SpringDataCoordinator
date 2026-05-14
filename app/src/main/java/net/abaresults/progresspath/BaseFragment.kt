package net.abaresults.progresspath

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.abaresults.progresspath.view.main.MainActivity

open class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Set top inset padding to 0
            view.updatePadding(
                left = ime.left,
                right = ime.right,
                bottom = ime.bottom,
                top = 0
            )

            insets
        }
    }

    fun hideTopBar() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.findViewById<MaterialToolbar>(R.id.toolbar)?.isVisible = false
        }
    }

    fun showTopBar() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.findViewById<MaterialToolbar>(R.id.toolbar)?.isVisible = true
        }
    }

    fun hideBottomBar() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.isVisible = false
        }
    }

    fun showBottomBar() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.isVisible = true
        }
    }

    fun setTitle(title: String) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
    }

    fun updateBottomNav() {
        (requireActivity() as MainActivity).updateBottomNav()
    }
}