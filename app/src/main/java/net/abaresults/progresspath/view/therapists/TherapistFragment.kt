package net.abaresults.progresspath.view.therapists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.FragmentTherapistsBinding

@AndroidEntryPoint
class TherapistFragment : BaseFragment() {

    private lateinit var binding: FragmentTherapistsBinding
    private val viewModel: TherapistViewModel by viewModels()

    private lateinit var therapistAdapter: TherapistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTherapistsBinding.inflate(inflater, container, false)
        observeData(viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.takeAction(TherapistAction.Start)
        configureViews()
    }

    private fun observeData(viewModel: TherapistViewModel) {
        val stateObserver = Observer<TherapistState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != TherapistState.Loading) {
                hideLoading()
            }

            when (it) {
                is TherapistState.Loading -> showLoading()
                is TherapistState.Error -> showError(it.generalError)
                is TherapistState.Idle -> {}
                is TherapistState.ContentLoaded -> handleContentLoaded(it.items, it.isClinic)
                is TherapistState.GoToObjectives -> navigateToObjectives()
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

        therapistAdapter = TherapistAdapter(
            onItemClicked = { therapist ->
                viewModel.takeAction(TherapistAction.TherapistClicked(therapist))
            },
            onItemRemove = { therapist ->
                showRemoveDialog(therapist)
            }
        )

        binding.therapistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = therapistAdapter
        }

        binding.fabAddTherapist.setOnClickListener {
            findNavController().navigate(R.id.addTherapistFragment)
        }
    }

    private fun handleContentLoaded(therapists: List<TherapistVM>, isClinic: Boolean) {
        therapistAdapter.updateData(therapists)
        binding.fabAddTherapist.isVisible = !isClinic
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

    private fun showRemoveDialog(therapist: TherapistVM) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Therapist")
            .setMessage("Are you sure you want to remove ${therapist.name}?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(TherapistAction.RemoveTherapist(therapist))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}