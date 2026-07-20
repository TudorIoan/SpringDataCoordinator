package app.springdata.coordinator.view.therapists.add_therapist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.databinding.FragmentAddTherapistBinding

@AndroidEntryPoint
class AddTherapistFragment : BaseFragment() {

    private lateinit var binding: FragmentAddTherapistBinding
    private val viewModel: AddTherapistViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddTherapistBinding.inflate(inflater, container, false)
        observeData(viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: AddTherapistViewModel) {
        val stateObserver = Observer<AddTherapistState?> {
            it ?: return@Observer

            if (it != AddTherapistState.Loading) {
                hideLoading()
            }

            when (it) {
                is AddTherapistState.Loading -> showLoading()
                is AddTherapistState.Error -> showError(it.error)
                is AddTherapistState.TherapistAdded -> navigateBack()
                is AddTherapistState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        hideBottomBar()

        binding.addTherapistButton.setOnClickListener {
            val therapistEmail = binding.therapistEmailEditText.text.toString()
            viewModel.takeAction(AddTherapistAction.AddTherapistClicked(therapistEmail))
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