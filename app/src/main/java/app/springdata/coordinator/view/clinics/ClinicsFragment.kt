package app.springdata.coordinator.view.clinics

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
import app.springdata.coordinator.databinding.FragmentClinicsBinding
import app.springdata.coordinator.model.Clinic

@AndroidEntryPoint
class ClinicsFragment : BaseFragment() {

    private lateinit var binding: FragmentClinicsBinding
    private val viewModel: ClinicsViewModel by viewModels()

    private lateinit var clinicsAdapter: ClinicsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClinicsBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(ClinicsAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: ClinicsViewModel) {
        val stateObserver = Observer<ClinicsState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != ClinicsState.Loading) {
                hideLoading()
            }

            when (it) {
                is ClinicsState.Loading -> showLoading()
                is ClinicsState.Error -> showError(it.generalError)
                is ClinicsState.Idle -> {}
                is ClinicsState.ContentLoaded -> handleContentLoaded(it.clinics)
                is ClinicsState.GoToKids -> navigateToKids()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        showTopBar()
        showBottomBar()

        clinicsAdapter = ClinicsAdapter(
            onItemClicked = { clinic ->
                viewModel.takeAction(ClinicsAction.ClinicClicked(clinic))
            },
            onItemEdit = { clinic ->
                showEditDialog(clinic)
            },
            onItemRemove = { clinic ->
                showRemoveDialog(clinic)
            }
        )

        binding.clinicsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = clinicsAdapter
        }

        binding.fabAddClinic.setOnClickListener {
            findNavController().navigate(R.id.addClinicFragment)
        }
        binding.fabAddClinic.isVisible = true
    }

    private fun handleContentLoaded(clinics: List<Clinic>) {
        clinicsAdapter.updateData(clinics)
    }

    private fun showError(generalError: String) {
        binding.errorTextView.text = generalError
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun navigateToKids() {
        findNavController().navigate(R.id.kidsFragment)
    }

    private fun showLoading() =
        binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() =
        binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showEditDialog(clinic: Clinic) {
        val editText = EditText(requireContext()).apply {
            setText(clinic.name)
            hint = "Clinic name"
        }

        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 0, 48, 0)
            addView(editText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Clinic")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.takeAction(ClinicsAction.UpdateClinicName(clinic.copy(name = newName)))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveDialog(clinic: Clinic) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Clinic")
            .setMessage("Are you sure you want to remove '${clinic.name}'?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(ClinicsAction.RemoveClinic(clinic))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
