package net.abaresults.progresspath.view.obj_library.objective_type

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.databinding.FragmentObjectiveTypesBinding
import net.abaresults.progresspath.model.ObjLevel

@AndroidEntryPoint
class ObjectiveTypesFragment : BaseFragment() {

    private lateinit var binding: FragmentObjectiveTypesBinding
    private val viewModel: ObjectiveTypesViewModel by viewModels()
    private lateinit var adapter: ObjectiveTypesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentObjectiveTypesBinding.inflate(inflater, container, false)
        setupViews()
        observeData()
        viewModel.takeAction(ObjectiveTypesAction.Start)
        return binding.root
    }

    private fun setupViews() {
        hideBottomBar()

        // Setup level toggle group - default to Beginner
        binding.levelToggleGroup.check(binding.beginnerButton.id)

        // Setup RecyclerView
        adapter = ObjectiveTypesAdapter { objectiveTypeId ->
            viewModel.takeAction(ObjectiveTypesAction.DeleteObjectiveType(objectiveTypeId))
        }
        binding.objectiveTypesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.objectiveTypesRecyclerView.adapter = adapter

        // Setup level toggle listener
        binding.levelToggleGroup.addOnButtonCheckedListener { _, _, _ ->
            val selectedLevel = getSelectedLevel()
            viewModel.takeAction(ObjectiveTypesAction.LoadObjectiveTypes(selectedLevel))
        }

        // Setup add button
        binding.addObjectiveTypeButton.setOnClickListener {
            val name = binding.newObjectiveTypeEditText.text.toString().trim()
            val level = getSelectedLevel()
            if (name.isNotEmpty()) {
                viewModel.takeAction(ObjectiveTypesAction.AddObjectiveType(name, level))
                binding.newObjectiveTypeEditText.text?.clear()
            }
        }
    }

    private fun observeData() {
        val stateObserver = Observer<ObjectiveTypesState?> {
            it ?: return@Observer

            when (it) {
                is ObjectiveTypesState.Loading -> {
                    // Show loading if needed
                }
                is ObjectiveTypesState.Error -> showError(it.error)
                is ObjectiveTypesState.ContentLoaded -> {
                    hideError()
                    adapter.updateObjectiveTypes(it.objectiveTypes)
                }
                is ObjectiveTypesState.ObjectiveTypeAdded -> {
                    hideError()
                    // Reload the list for the current level
                    val selectedLevel = getSelectedLevel()
                    viewModel.takeAction(ObjectiveTypesAction.LoadObjectiveTypes(selectedLevel))
                }
                is ObjectiveTypesState.ObjectiveTypeDeleted -> {
                    hideError()
                    // Reload the list for the current level
                    val selectedLevel = getSelectedLevel()
                    viewModel.takeAction(ObjectiveTypesAction.LoadObjectiveTypes(selectedLevel))
                }
                is ObjectiveTypesState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun getSelectedLevel(): ObjLevel {
        return when (binding.levelToggleGroup.checkedButtonId) {
            binding.beginnerButton.id -> ObjLevel.BEGINNER
            binding.intermediateButton.id -> ObjLevel.INTERMEDIATE
            binding.advancedButton.id -> ObjLevel.ADVANCED
            else -> ObjLevel.BEGINNER
        }
    }

    private fun showError(error: String) {
        binding.errorTextView.text = error
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.errorTextView.visibility = View.GONE
    }
}