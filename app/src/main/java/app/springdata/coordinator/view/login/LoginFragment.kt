package app.springdata.coordinator.view.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.FragmentLoginBinding

@AndroidEntryPoint
class LoginFragment : BaseFragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentLoginBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(LoginAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: LoginViewModel) {
        val stateObserver = Observer<LoginState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != LoginState.Loading) {
                hideLoading()
            }

            when (it) {
                is LoginState.Loading -> showLoading()
                is LoginState.Error -> handleError(it.generalError, it.emailError, it.passwordError)
                is LoginState.Idle -> {}
                is LoginState.LoginSuccess -> handleLoginSuccess()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        hideTopBar()
        hideBottomBar()

        binding.signUpButton.setOnClickListener {
            navigateToSignUp()
        }

        binding.loginButton.setOnClickListener {
            viewModel.takeAction(LoginAction.LoginClicked(binding.emailEditText.text.toString().trim(), binding.passwordEditText.text.toString().trim()))
        }
    }

    private fun handleError(generalError: String, emailError: String, passwordError: String) {
            if (generalError.isNotEmpty()) {
                binding.errorTextView.text = generalError
                binding.errorTextView.visibility = View.VISIBLE
            } else {
                binding.errorTextView.visibility = View.GONE
            }

             if (emailError.isNotEmpty()) {
                binding.emailInputLayout.error = emailError
                binding.emailEditText.requestFocus()
            } else {
                binding.emailInputLayout.error = null
             }

            if (passwordError.isNotEmpty()) {
                binding.passwordInputLayout.error = passwordError
                binding.passwordEditText.requestFocus()
            } else {
                binding.passwordInputLayout.error = null
            }
    }

    private fun handleLoginSuccess() {
        updateBottomNav()
        navigateToHome()
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.clinicsFragment)
    }

    private fun navigateToSignUp() {
        findNavController().navigate(R.id.registerFragment)
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}