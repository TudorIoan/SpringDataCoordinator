package net.abaresults.progresspath.view.coordinator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.FragmentCoordinatorBinding
import net.abaresults.progresspath.databinding.FragmentTherapistsBinding
import net.abaresults.progresspath.model.Clinic
import net.abaresults.progresspath.model.UserProfile

@AndroidEntryPoint
class CoordinatorFragment : BaseFragment() {

    private lateinit var binding: FragmentCoordinatorBinding
    private val viewModel: CoordinatorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentCoordinatorBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(CoordinatorAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(viewModel: CoordinatorViewModel) {
        val stateObserver = Observer<CoordinatorState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != CoordinatorState.Loading) {
                hideLoading()
            }

            when (it) {
                is CoordinatorState.Loading -> showLoading()
                is CoordinatorState.Error -> showError(it.generalError)
                is CoordinatorState.Idle -> {}
                is CoordinatorState.ContentLoaded -> handleContentLoaded(it.coordinatorUser)
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

    }

    private fun handleContentLoaded(coordinatorUser: UserProfile) {
        binding.nameTextView.text = coordinatorUser.name
        binding.emailTextView.text = coordinatorUser.email
    }

    private fun showError(generalError: String) {
        binding.subtitleInclude.errorTextView.text = generalError
        binding.subtitleInclude.errorTextView.visibility = View.VISIBLE
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}