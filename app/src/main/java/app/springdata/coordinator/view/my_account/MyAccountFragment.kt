package app.springdata.coordinator.view.my_account

import android.os.Bundle
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.FragmentMyAccountBinding

@AndroidEntryPoint
class MyAccountFragment : BaseFragment() {

    private lateinit var binding: FragmentMyAccountBinding
    private val viewModel: MyAccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyAccountBinding.inflate(inflater, container, false)
        observeData(binding, viewModel)
        configureViews()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun configureViews() {
        val feedbackBottomSpacing = (binding.feedbackFab.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.feedbackFab) { view, insets ->
            val navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = feedbackBottomSpacing + navigationBottom
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.feedbackFab)

        binding.logoutButton.setOnClickListener {
            viewModel.takeAction(MyAccountAction.LogoutClicked)
        }
        binding.feedbackFab.setOnClickListener {
            showFeedbackInput()
        }
        binding.sendFeedbackButton.setOnClickListener {
            binding.feedbackInputLayout.error = null
            viewModel.takeAction(MyAccountAction.SendFeedbackClicked(binding.feedbackEditText.text.toString()))
        }
    }

    private fun observeData(binding: FragmentMyAccountBinding, viewModel: MyAccountViewModel) {
        val stateObserver = Observer<MyAccountState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != MyAccountState.Loading) {
                hideLoading()
            }

            when (it) {
                is MyAccountState.Loading -> showLoading()
                is MyAccountState.Error -> {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                is MyAccountState.Content -> {
                    handleContent(it.name, it.email)
                }
                is MyAccountState.FeedbackValidationError -> {
                    binding.feedbackInputLayout.error = it.messageError
                }

                MyAccountState.Idle -> {}
                MyAccountState.LogoutDone -> handleLogoutDone()
                MyAccountState.FeedbackSent -> handleFeedbackSent()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.takeAction(MyAccountAction.Start)
    }

    private fun handleContent(name: String, email: String) {
        binding.nameTextView.text = name
        binding.emailTextView.text = email
        binding.roleTextView.text = "Coordinator"
        binding.feedbackLabel.text = getFeedbackMessage(name)
    }

    private fun handleLogoutDone() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.main_nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, navOptions)
    }

    private fun showFeedbackInput() {
        binding.feedbackContainer.visibility = View.VISIBLE
        binding.feedbackFab.hide()
        binding.feedbackEditText.requestFocus()
        scrollToFeedback()

        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.feedbackEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun handleFeedbackSent() {
        binding.feedbackInputLayout.error = null
        binding.feedbackEditText.text?.clear()
        hideKeyboard()
        Toast.makeText(requireContext(), "Thanks, we really appreciate it.", Toast.LENGTH_SHORT).show()
    }

    private fun scrollToFeedback() {
        binding.accountScrollView.post {
            binding.accountScrollView.smoothScrollTo(0, binding.feedbackContainer.bottom)
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.feedbackEditText.windowToken, 0)
        binding.feedbackEditText.clearFocus()
    }

    private fun getFeedbackMessage(name: String): String {
        return "Hey $name, send us some feedback. We would love to hear what works, what feels confusing, and what would make coordinator work easier. We will reply by email."
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

}
