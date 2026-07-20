package app.springdata.coordinator.view.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.FragmentRegisterBinding

@AndroidEntryPoint
class RegisterFragment : BaseFragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        observeData(viewModel)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.isVisible = false

        configureViews()
    }

    private fun observeData(viewModel: RegisterViewModel) {
        val stateObserver = Observer<RegisterState?> {
            it ?: return@Observer

            if (it != RegisterState.Loading) {
                hideLoading()
            }

            when (it) {
                is RegisterState.Loading -> showLoading()
                is RegisterState.Error -> handleError(
                    it.generalError,
                    it.nameError,
                    it.emailError,
                    it.passwordError,
                    it.confirmPasswordError
                )

                is RegisterState.Idle -> {}
                is RegisterState.RegisterSuccess -> navigateToHome()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.takeAction(RegisterAction.Start)
    }

    private fun configureViews() {
        hideTopBar()
        hideBottomBar()

        binding.loginButton.setOnClickListener {
            navigateToLogin()
        }

        binding.signUpButton.setOnClickListener {
            viewModel.takeAction(
                RegisterAction.SignUpClicked(
                    binding.nameEditText.text.toString().trim(),
                    binding.emailEditText.text.toString().trim(),
                    binding.passwordEditText.text.toString().trim(),
                    binding.confirmPasswordEditText.text.toString().trim()
                )
            )
        }
    }

    private fun handleError(
        generalError: String,
        nameError: String,
        emailError: String,
        passwordError: String,
        confirmPasswordError: String
    ) {
        if (generalError.isNotEmpty()) {
            binding.errorTextView.text = generalError
            binding.errorTextView.visibility = View.VISIBLE
        } else {
            binding.errorTextView.visibility = View.GONE
        }

        if (nameError.isNotEmpty()) {
            binding.nameInputLayout.error = nameError
            binding.nameEditText.requestFocus()
        } else {
            binding.nameInputLayout.error = null
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

        if (confirmPasswordError.isNotEmpty()) {
            binding.confirmPasswordInputLayout.error = passwordError
            binding.confirmPasswordEditText.requestFocus()
        } else {
            binding.confirmPasswordInputLayout.error = null
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.loginFragment)
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.clinicsFragment)
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}
