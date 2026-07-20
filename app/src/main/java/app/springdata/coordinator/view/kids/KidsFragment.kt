package app.springdata.coordinator.view.kids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.FragmentKidsBinding
import app.springdata.coordinator.model.Kid

@AndroidEntryPoint
class KidsFragment : BaseFragment() {

    private lateinit var binding: FragmentKidsBinding
    private val viewModel: KidsViewModel by viewModels()

    private lateinit var kidsAdapter: KidsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentKidsBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(KidsAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(viewModel: KidsViewModel) {
        val stateObserver = Observer<KidsState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != KidsState.Loading) {
                hideLoading()
            }

            when (it) {
                is KidsState.Loading -> showLoading()
                is KidsState.Error -> showError(it.generalError)
                is KidsState.Idle -> {}
                is KidsState.ContentLoaded -> handleContentLoaded(it.items)
                is KidsState.GoToObjectives -> navigateToObjectives()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.subtitleInclude.subtitleTextView.isVisible = true
            binding.subtitleInclude.subtitleTextView.text = title
        }
    }

    private fun configureViews() {
        showBottomBar()

        kidsAdapter = KidsAdapter(
            onItemClicked = { kid ->
                viewModel.takeAction(KidsAction.KidClicked(kid))
            },
            onItemEdit = { kid ->
                showEditDialog(kid)
            },
            onItemRemove = { kid ->
                showRemoveDialog(kid)
            }
        )

        binding.kidsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kidsAdapter
        }

        binding.fabAddKid.setOnClickListener {
            findNavController().navigate(R.id.addKidFragment)
        }
        binding.fabAddKid.isVisible = true
    }

    private fun handleContentLoaded(kids: List<Kid>) {
        kidsAdapter.updateData(kids)
        addMenuButtons()
    }

    private fun addMenuButtons() {
        // Setup toolbar buttons
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.clinics_toolbar_menu, menu)

                val therapistsItem = menu.findItem(R.id.action_therapists)
                therapistsItem?.isVisible = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_therapists -> {
                        findNavController().navigate(R.id.therapistsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showError(generalError: String) {
        binding.subtitleInclude.errorTextView.text = generalError
        binding.subtitleInclude.errorTextView.visibility = View.VISIBLE
    }

    private fun navigateToObjectives() {
        findNavController().navigate(R.id.objectivesFragment)
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showEditDialog(kid: Kid) {
        val editText = EditText(requireContext()).apply {
            setText(kid.name)
            hint = "Kid name"
        }
        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 0, 48, 0)
            addView(editText)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Kid")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.takeAction(KidsAction.UpdateKidName(kid.copy(name = newName)))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveDialog(kid: Kid) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Kid")
            .setMessage("Are you sure you want to remove ${kid.name}?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(KidsAction.RemoveKid(kid))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
