package app.springdata.coordinator.view.obj_library

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
import app.springdata.coordinator.databinding.FragmentObjLibraryBinding
import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.Objective
import com.google.android.material.tabs.TabLayout

@AndroidEntryPoint
class ObjLibraryFragment : BaseFragment() {

    private lateinit var binding: FragmentObjLibraryBinding
    private val viewModel: ObjLibraryViewModel by viewModels()

    private lateinit var objLibraryAdapter: ObjLibraryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentObjLibraryBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(ObjLibraryAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun addMenuButtons() {
        // Setup toolbar buttons
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // No specific menu for obj library yet
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeData(viewModel: ObjLibraryViewModel) {
        val stateObserver = Observer<ObjLibraryState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != ObjLibraryState.Loading) {
                hideLoading()
            }

            when (it) {
                is ObjLibraryState.Loading -> showLoading()
                is ObjLibraryState.Error -> showError(it.generalError)
                is ObjLibraryState.Idle -> {}
                is ObjLibraryState.ContentLoaded -> handleContentLoaded(it.items, it.selectedLevel)
                is ObjLibraryState.GoToAddObjective -> navigateToAddObjective()
                is ObjLibraryState.GoToItemLibrary -> navigateToItemLibrary(it.objective)
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        showBottomBar()

        objLibraryAdapter = ObjLibraryAdapter(
            onItemClicked = { objectiveListItem ->
                viewModel.takeAction(ObjLibraryAction.ObjectiveClicked(objectiveListItem))
            },
            onItemEdit = { objective ->
                showEditDialog(objective)
            },
            onItemRemove = { objective ->
                showRemoveDialog(objective)
            }
        )

        binding.objLibraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = objLibraryAdapter
        }

        binding.fabAddObjective.setOnClickListener {
            viewModel.takeAction(ObjLibraryAction.AddObjectiveClicked)
        }
        binding.fabAddObjective.isVisible = true

        setupTabs()
        addMenuButtons()
    }

    private fun setupTabs() {
        // Add tabs for each level
        binding.levelTabLayout.addTab(binding.levelTabLayout.newTab().setText("Beginner"))
        binding.levelTabLayout.addTab(binding.levelTabLayout.newTab().setText("Intermediate"))
        binding.levelTabLayout.addTab(binding.levelTabLayout.newTab().setText("Advanced"))

        // Handle tab selection
        binding.levelTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val level = when (tab?.position) {
                    0 -> ObjLevel.BEGINNER
                    1 -> ObjLevel.INTERMEDIATE
                    2 -> ObjLevel.ADVANCED
                    else -> ObjLevel.BEGINNER
                }
                viewModel.takeAction(ObjLibraryAction.LevelTabSelected(level))
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun navigateToAddObjective() {
        // Navigate to add objective fragment if needed
        findNavController().navigate(R.id.addObjLibraryFragment)
    }

    private fun navigateToItemLibrary(objective: Objective) {
        // TODO: Pass the objective to ObjItemLibraryFragment
        findNavController().navigate(R.id.objItemLibraryFragment)
    }

    private fun handleContentLoaded(objectives: List<ObjLibraryListItem>, selectedLevel: ObjLevel) {
        objLibraryAdapter.set(objectives)
        updateSelectedTab(selectedLevel)
    }

    private fun updateSelectedTab(level: ObjLevel) {
        val tabIndex = when (level) {
            ObjLevel.BEGINNER -> 0
            ObjLevel.INTERMEDIATE -> 1
            ObjLevel.ADVANCED -> 2
        }
        binding.levelTabLayout.selectTab(binding.levelTabLayout.getTabAt(tabIndex))
    }

    private fun showError(generalError: String) {
        binding.subtitleInclude.errorTextView.text = generalError
        binding.subtitleInclude.errorTextView.visibility = View.VISIBLE
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }


    private fun showEditDialog(objective: Objective) {
        val nameEditText = EditText(requireContext()).apply {
            setText(objective.name)
            hint = "Objective name"
        }
        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 0, 48, 0)
            addView(nameEditText)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Objective")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.takeAction(ObjLibraryAction.UpdateObjective(objective.copy(name = newName)))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveDialog(objective: Objective) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Objective")
            .setMessage("Are you sure you want to remove ${objective.name}? This will also remove it from all kids.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(ObjLibraryAction.RemoveObjective(objective))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
