package app.springdata.coordinator.view.obj_library.add_objective

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.FragmentAddObjLibraryBinding
import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.ObjectiveType

@AndroidEntryPoint
class AddObjLibraryFragment : BaseFragment() {

    private lateinit var binding: FragmentAddObjLibraryBinding
    private val viewModel: AddObjLibraryViewModel by viewModels()

    private val objectiveTypes = mutableListOf<ObjectiveType>()
    private var currentLevel: ObjLevel = ObjLevel.BEGINNER

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddObjLibraryBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(AddObjLibraryAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(viewModel: AddObjLibraryViewModel) {
        val stateObserver = Observer<AddObjLibraryState?> {
            it ?: return@Observer

            if (it != AddObjLibraryState.Loading) {
                hideLoading()
            }

            if (it !is AddObjLibraryState.Error) {
                hideError()
            }

            when (it) {
                is AddObjLibraryState.Loading -> showLoading()
                is AddObjLibraryState.Error -> showError(it.error)
                is AddObjLibraryState.ContentLoaded -> handleContentLoaded(it.objLevel)
                is AddObjLibraryState.ObjectiveCreated -> showObjectiveCreatedDialog(it.objectiveName)
                is AddObjLibraryState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        hideBottomBar()

        // Handle create objective button
        binding.createObjectiveButton.setOnClickListener {
            val objectiveName = binding.objectiveNameEditText.text.toString()
            val objectiveType = objectiveTypes.find { it.displayName == binding.objectiveTypeTextView.text.toString() }!!
            viewModel.takeAction(AddObjLibraryAction.CreateObjectiveClicked(objectiveType, objectiveName, currentLevel))
        }
    }

    private fun handleContentLoaded(objLevel: ObjLevel) {
        currentLevel = objLevel
        updateObjectiveTypeDropdown(objLevel)
        updateLevelDisplay(objLevel)
    }

    private fun updateLevelDisplay(level: ObjLevel) {
        binding.selectedLevelTextView.text = level.displayName
    }

    private fun updateObjectiveTypeDropdown(level: ObjLevel) {
        objectiveTypes.clear()
        objectiveTypes.addAll(ObjectiveType.getObjectiveTypesByLevel(level))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, objectiveTypes.map { it.displayName })
        binding.objectiveTypeTextView.setAdapter(adapter)

        // Set the first item as selected if available
        if (objectiveTypes.isNotEmpty()) {
            binding.objectiveTypeTextView.setText(objectiveTypes[0].displayName, false)
        }
    }

    private fun showError(generalError: String) {
        binding.errorTextView.text = generalError
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.errorTextView.visibility = View.GONE
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showObjectiveCreatedDialog(objectiveName: String) {
        // Update the success message text with the objective name
        binding.successMessageText.text = getString(R.string.objective_was_created, objectiveName)

        // Make the view visible and animate it down
        binding.successMessageView.apply {
            visibility = View.VISIBLE
            animate()
                .translationY(0f)
                .setDuration(300)
                .withEndAction {
                    // After animation completes, wait 1 second then animate back up
                    Handler(Looper.getMainLooper()).postDelayed({
                        animate()
                            .translationY(-100f)
                            .setDuration(300)
                            .withEndAction {
                                visibility = View.GONE
                                binding.objectiveNameEditText.text?.clear()
                            }
                    }, 1000)
                }
        }
    }
}