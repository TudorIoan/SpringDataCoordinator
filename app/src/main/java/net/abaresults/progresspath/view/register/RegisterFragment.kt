package net.abaresults.progresspath.view.register

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.BuildConfig
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.FragmentRegisterBinding
import net.abaresults.progresspath.model.UserType

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
                is RegisterState.InvitedTherapistFound -> handleInvitedTherapistFound(it.coordinatorName)
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.takeAction(RegisterAction.Start)
    }

    private fun configureViews() {
        hideTopBar()
        hideBottomBar()
        setupUserTypeSwitch()

        binding.loginButton.setOnClickListener {
            navigateToLogin()
        }

        binding.emailEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val emailText = binding.emailEditText.text.toString().trim()
            if (!hasFocus && emailText.isNotEmpty()) {
                viewModel.takeAction(RegisterAction.EmailTyped(emailText))
            }
        }

        binding.signUpButton.setOnClickListener {
            viewModel.takeAction(
                RegisterAction.SignUpClicked(
                    binding.nameEditText.text.toString().trim(),
                    binding.emailEditText.text.toString().trim(),
                    binding.passwordEditText.text.toString().trim(),
                    binding.confirmPasswordEditText.text.toString().trim(),
                    appUserType()
                )
            )
        }
    }

    private fun setupUserTypeSwitch() {
        val userType = appUserType()
        binding.userTypeSwitch.isChecked = userType == UserType.COORDINATOR
        binding.userTypeSwitch.text = when (userType) {
            UserType.COORDINATOR -> getString(R.string.register_user_type_coordinator_label)
            UserType.THERAPIST -> getString(R.string.register_user_type_therapist_label)
        }
        binding.userTypeSwitchContainer.isVisible = false
    }

    private fun appUserType(): UserType {
        return when (BuildConfig.SPRINGDATA_APP_ROLE) {
            "coordinator" -> UserType.COORDINATOR
            "therapist" -> UserType.THERAPIST
            else -> UserType.THERAPIST
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

    private fun handleInvitedTherapistFound(coordinatorName: String?) {
        if (appUserType() != UserType.THERAPIST) {
            binding.userTypeSwitchContainer.isVisible = false
            binding.invitedTextView.isVisible = false
            return
        }

        coordinatorName?.let {
            binding.userTypeSwitch.isChecked = false
            binding.userTypeSwitchContainer.isVisible = false
            binding.invitedTextView.isVisible = true
            binding.invitedTextView.text = Html.fromHtml(getString(R.string.register_invited_therapist, it), Html.FROM_HTML_MODE_LEGACY)
        } ?: run {
            binding.userTypeSwitchContainer.isVisible = false
            binding.invitedTextView.isVisible = false
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
