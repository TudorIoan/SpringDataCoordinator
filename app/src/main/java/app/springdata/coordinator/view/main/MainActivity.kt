package app.springdata.coordinator.view.main

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private val viewModel: MainViewModel by viewModels()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus()
        }

        override fun onLost(network: Network) {
            updateNetworkStatus()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkStatus()
        }
    }

    private val navHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        observeData(viewModel)
        setupNetworkStatus()
        viewModel.takeAction(MainAction.Start)
    }

    override fun onDestroy() {
        if (::connectivityManager.isInitialized) {
            runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        }
        super.onDestroy()
    }

    private fun setupNavigation(isLoggedIn: Boolean) {

        val navController = navHostFragment.navController

        val navGraph = navController.navInflater.inflate(R.navigation.main_nav_graph)
        val startDestinationId = if (isLoggedIn) R.id.clinicsFragment else R.id.loginFragment
        navGraph.setStartDestination(startDestinationId)
        navController.graph = navGraph

        setSupportActionBar(binding.toolbar)

        val appBarConfig = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfig)

        binding.bottomNavigationView.setupWithNavController(navController)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.clinicsFragment -> {
                    navController.popBackStack(R.id.clinicsFragment, false)
                    true
                }
                R.id.objLibraryFragment -> {
                    navController.popBackStack(R.id.clinicsFragment, false)
                    navController.navigate(R.id.objLibraryFragment)
                    true
                }
                R.id.myAccountFragment -> {
                    navController.popBackStack(R.id.clinicsFragment, false)
                    navController.navigate(R.id.myAccountFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navHostFragment.navController.navigateUp(AppBarConfiguration(navHostFragment.navController.graph)) || super.onSupportNavigateUp()
    }

    private fun observeData(viewModel: MainViewModel) {
        val stateObserver = Observer<MainState?> {
            it ?: return@Observer

            when (it) {
                MainState.Idle -> {}
                is MainState.AuthStatus -> handleAuthStatusUpdated(it.isLoggedIn)
            }
        }
        viewModel.state.observe(this, stateObserver)
    }

    private fun handleAuthStatusUpdated(isLoggedIn: Boolean) {
        setupNavigation(isLoggedIn)
        binding.bottomNavigationView.menu.findItem(R.id.objLibraryFragment).isVisible = true
    }

    fun updateBottomNav() {
        viewModel.takeAction(MainAction.UpdateAuthStatus)
    }

    private fun setupNetworkStatus() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateNetworkStatus()
    }

    private fun updateNetworkStatus() {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val hasValidatedInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        runOnUiThread {
            binding.offlineInfoTextView.isVisible = !hasValidatedInternet
        }
    }
}
