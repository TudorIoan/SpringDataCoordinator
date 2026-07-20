package app.springdata.coordinator.view.kids.add_kid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.databinding.FragmentAddKidBinding

@AndroidEntryPoint
class AddKidFragment : BaseFragment() {

    private lateinit var binding: FragmentAddKidBinding
    private val viewModel: AddKidViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddKidBinding.inflate(inflater, container, false)
        observeData(viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: AddKidViewModel) {
        val stateObserver = Observer<AddKidState?> {
            it ?: return@Observer

            if (it != AddKidState.Loading) {
                hideLoading()
            }

            when (it) {
                is AddKidState.Loading -> showLoading()
                is AddKidState.Error -> showError(it.error)
                is AddKidState.KidAdded -> navigateBack()
                is AddKidState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        hideBottomBar()

        binding.addKidButton.setOnClickListener {
            val clinicName = binding.kidNameEditText.text.toString()
            viewModel.takeAction(AddKidAction.AddKidClicked(clinicName))
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