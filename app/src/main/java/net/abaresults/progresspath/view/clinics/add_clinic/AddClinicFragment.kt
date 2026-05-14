package net.abaresults.progresspath.view.clinics.add_clinic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.databinding.FragmentAddClinicBinding

@AndroidEntryPoint
class AddClinicFragment : BaseFragment() {

    private lateinit var binding: FragmentAddClinicBinding
    private val viewModel: AddClinicViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddClinicBinding.inflate(inflater, container, false)
        observeData(viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: AddClinicViewModel) {
        val stateObserver = Observer<AddClinicState?> {
            it ?: return@Observer

            if (it != AddClinicState.Loading) {
                hideLoading()
            }

            when (it) {
                is AddClinicState.Loading -> showLoading()
                is AddClinicState.Error -> showError(it.error)
                is AddClinicState.ClinicAdded -> navigateBack()
                is AddClinicState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        hideBottomBar()

        binding.addClinicButton.setOnClickListener {
            val clinicName = binding.clinicNameEditText.text.toString()
            viewModel.takeAction(AddClinicAction.AddClinicClicked(clinicName))
        }
    }

    private fun showError(generalError: String) {
        binding.errorTextView.text = generalError
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun navigateBack() {
        findNavController().popBackStack()
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}