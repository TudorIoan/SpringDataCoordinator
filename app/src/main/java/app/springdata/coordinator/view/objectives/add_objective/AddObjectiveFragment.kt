package app.springdata.coordinator.view.objectives.add_objective

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.FragmentAddObjectiveBinding
import app.springdata.coordinator.model.ObjItemType
import app.springdata.coordinator.model.ObjLevel
import app.springdata.coordinator.model.Objective
import app.springdata.coordinator.model.ObjectiveType

@AndroidEntryPoint
class AddObjectiveFragment : BaseFragment() {

    private lateinit var binding: FragmentAddObjectiveBinding
    private val viewModel: AddObjectiveViewModel by viewModels()
    private lateinit var availableObjectivesAdapter: AvailableObjectivesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddObjectiveBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(AddObjectiveAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(viewModel: AddObjectiveViewModel) {
        val stateObserver = Observer<AddObjectiveState?> {
            it ?: return@Observer

            if (it != AddObjectiveState.Loading) {
                hideLoading()
            }

            when (it) {
                is AddObjectiveState.Loading -> showLoading()
                is AddObjectiveState.Error -> showError(it.error)
                is AddObjectiveState.ContentLoaded -> hideLoading()
                is AddObjectiveState.ObjectiveAdded -> showObjectiveAddedDialog(it.objectiveName)
                is AddObjectiveState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)

        // Observe available objectives
        viewModel.availableObjectives.observe(viewLifecycleOwner) { objectives ->
            availableObjectivesAdapter.setObjectives(objectives)
            binding.availableObjectivesTitle.isVisible = objectives.isNotEmpty()
            binding.availableObjectivesRecyclerView.isVisible = objectives.isNotEmpty()
        }

        // Keep the tabs in sync with the selected level
        viewModel.selectedLevel.observe(viewLifecycleOwner) { level ->
            updateLevelTabs(level)
        }

        // Observe available objective types to update dropdown
        viewModel.availableObjectiveTypes.observe(viewLifecycleOwner) { types ->
            updateObjectiveTypeDropdown(types)
        }
    }

    private fun configureViews() {
        hideBottomBar()

        // Setup RecyclerView for available objectives
        availableObjectivesAdapter = AvailableObjectivesAdapter { objective ->
            if (objective.hasAnyYesNoItems()) {
                showMasteryCriteriaDialog(objective)
            } else {
                viewModel.takeAction(AddObjectiveAction.ObjectiveSelected(objective, null))
            }
        }

        binding.availableObjectivesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = availableObjectivesAdapter
        }

        ObjLevel.values().forEach { level ->
            binding.levelTabLayout.addTab(
                binding.levelTabLayout.newTab().setText(level.displayName)
            )
        }
        updateLevelTabs(viewModel.selectedLevel.value ?: ObjLevel.BEGINNER)
        binding.levelTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val level = ObjLevel.values()[tab.position]
                updateLevelTabs(level)
                viewModel.takeAction(AddObjectiveAction.ObjLevelChanged(level))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showError(generalError: String) {
        binding.errorTextView.text = generalError
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun showMasteryCriteriaDialog(objective: Objective) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 0)
        }
        val coordinatorRadio = RadioButton(requireContext()).apply {
            text = "Coordinator decides"
            isChecked = true
        }
        val consecutiveRadio = RadioButton(requireContext()).apply {
            text = ""
        }
        val countEditText = EditText(requireContext()).apply {
            setText("3")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            isEnabled = false
            minEms = 2
        }
        val consecutiveRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(consecutiveRadio)
            addView(countEditText, LinearLayout.LayoutParams(160, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(TextView(requireContext()).apply {
                text = " consecutive Yes"
                setPadding(12, 0, 0, 0)
            })
        }

        fun selectCoordinator() {
            coordinatorRadio.isChecked = true
            consecutiveRadio.isChecked = false
            countEditText.isEnabled = false
        }

        fun selectConsecutive() {
            coordinatorRadio.isChecked = false
            consecutiveRadio.isChecked = true
            countEditText.isEnabled = true
            countEditText.requestFocus()
        }

        coordinatorRadio.setOnClickListener { selectCoordinator() }
        consecutiveRadio.setOnClickListener { selectConsecutive() }
        consecutiveRow.setOnClickListener { selectConsecutive() }

        container.addView(coordinatorRadio)
        container.addView(consecutiveRow)

        AlertDialog.Builder(requireContext())
            .setTitle("Mastery Criteria")
            .setMessage("Choose how mastery should be decided for Yes/No items in '${objective.name}'.")
            .setView(container)
            .setPositiveButton("Add Objective") { _, _ ->
                val consecutiveYesses = if (consecutiveRadio.isChecked) {
                    countEditText.text.toString().toIntOrNull()?.coerceIn(1, 20) ?: 3
                } else {
                    null
                }
                viewModel.takeAction(AddObjectiveAction.ObjectiveSelected(objective, consecutiveYesses))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun Objective.hasAnyYesNoItems(): Boolean {
        return itemsList.any { it.type == ObjItemType.YES_NO }
    }

    private fun navigateBack() {
        findNavController().popBackStack()
    }

    private fun showObjectiveAddedDialog(objectiveName: String) {
        // Update the success message text with the objective name
        binding.successMessageText.text = getString(R.string.objective_was_added, objectiveName)

        // Make the view visible and animate it down
        binding.successMessageView.apply {
            visibility = View.VISIBLE
            animate()
                .translationY(0f)
                .setDuration(300)
                .withEndAction {
                    // After animation completes, wait 2 seconds then animate back up
                    Handler(Looper.getMainLooper()).postDelayed({
                        animate()
                            .translationY(-100f)
                            .setDuration(300)
                            .withEndAction {
                                visibility = View.GONE
                            }
                    }, 1000)
                }
        }
    }

    private fun updateLevelTabs(level: ObjLevel) {
        val colorRes = when (level) {
            ObjLevel.BEGINNER -> R.color.objectiveLevelBeginner
            ObjLevel.INTERMEDIATE -> R.color.objectiveLevelIntermediate
            ObjLevel.ADVANCED -> R.color.objectiveLevelAdvanced
        }
        binding.levelTabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(requireContext(), colorRes))
        if (binding.levelTabLayout.selectedTabPosition != level.ordinal) {
            binding.levelTabLayout.selectTab(binding.levelTabLayout.getTabAt(level.ordinal))
        }
    }

    private fun updateObjectiveTypeDropdown(types: List<ObjectiveType>) {
        val displayNames = types.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, displayNames)
        binding.objectiveTypeTextView.setAdapter(adapter)

        if (displayNames.isNotEmpty()) {
            binding.objectiveTypeTextView.setText(displayNames[0], false)
        }

        // Handle objective type changes
        binding.objectiveTypeTextView.setOnItemClickListener { _, _, position, _ ->
            if (position < types.size) {
                val selectedType = types[position]
                viewModel.takeAction(AddObjectiveAction.ObjectiveTypeChanged(selectedType))
            }
        }
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}
