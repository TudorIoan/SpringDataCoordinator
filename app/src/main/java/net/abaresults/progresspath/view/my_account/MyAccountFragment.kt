package net.abaresults.progresspath.view.my_account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.FragmentMyAccountBinding
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.util.capitalizeFirst
import java.util.Locale

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

    private fun configureViews() {
        binding.logoutButton.setOnClickListener {
            viewModel.takeAction(MyAccountAction.LogoutClicked)
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
                    handleContent(it.name, it.email, it.userType)
                }

                MyAccountState.Idle -> {}
                MyAccountState.LogoutDone -> handleLogoutDone()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.takeAction(MyAccountAction.Start)
    }

    private fun handleContent(name: String, email: String, userType: UserType) {
        binding.nameTextView.text = name
        binding.emailTextView.text = email
        binding.roleTextView.text = userType.name.lowercase(Locale.getDefault()).capitalizeFirst()
    }

    private fun handleLogoutDone() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.main_nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, navOptions)
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

}