package app.springdata.coordinator.view.main

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private var currentDestinationId: Int? = null
    private var isBottomBarExpanded = true
    private var addOverrideVisible: Boolean? = null
    private var bottomBarTouchStartX = 0f
    private var bottomBarTouchStartY = 0f
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
        configureSystemBars()
        configureBottomNavigationInsets()
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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            addOverrideVisible = null
            isBottomBarExpanded = isBottomBarAlwaysExpanded(destination.id)
            binding.toolbar.post { tintToolbarNavigationIcon() }
            updateBottomNavSelection(destination.id)
            updateNetworkStatus()
        }

        currentDestinationId = startDestinationId
        tintToolbarNavigationIcon()
        updateBottomNavSelection(startDestinationId)
        updateNetworkStatus()

        binding.clinicsNavItem.setOnClickListener {
            navController.popBackStack(R.id.clinicsFragment, false)
        }

        binding.libraryNavItem.setOnClickListener {
            navController.popBackStack(R.id.clinicsFragment, false)
            if (navController.currentDestination?.id != R.id.objLibraryFragment) {
                navController.navigate(R.id.objLibraryFragment)
            }
        }

        binding.accountNavItem.setOnClickListener {
            navController.popBackStack(R.id.clinicsFragment, false)
            if (navController.currentDestination?.id != R.id.myAccountFragment) {
                navController.navigate(R.id.myAccountFragment)
            }
        }

        binding.addNavItem.setOnClickListener {
            when (navController.currentDestination?.id) {
                R.id.clinicsFragment -> navController.navigate(R.id.addClinicFragment)
                R.id.kidsFragment -> navController.navigate(R.id.addKidFragment)
                R.id.objectivesFragment -> navController.navigate(R.id.addObjectiveFragment)
                R.id.objLibraryFragment -> navController.navigate(R.id.addObjLibraryFragment)
                R.id.objItemLibraryFragment -> navController.navigate(R.id.addObjItemFragment)
                R.id.therapistsFragment -> navController.navigate(R.id.addTherapistFragment)
            }
        }

        binding.bottomNavigationCard.setOnClickListener {
            if (!isBottomBarExpanded && !isBottomBarAlwaysExpanded(currentDestinationId)) {
                isBottomBarExpanded = true
                currentDestinationId?.let { destinationId -> updateBottomNavSelection(destinationId) }
            }
        }

        configureBottomBarSwipeCollapse()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureBottomBarSwipeCollapse() {
        val swipeTouchListener = android.view.View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bottomBarTouchStartX = event.rawX
                    bottomBarTouchStartY = event.rawY
                    false
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - bottomBarTouchStartX
                    val deltaY = event.rawY - bottomBarTouchStartY
                    val swipeThreshold = 40 * resources.displayMetrics.density
                    if (
                        isBottomBarExpanded &&
                        !isBottomBarAlwaysExpanded(currentDestinationId) &&
                        deltaX > swipeThreshold &&
                        abs(deltaX) > abs(deltaY)
                    ) {
                        collapseBottomBar()
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
        }

        listOf(
            binding.bottomNavigationCard,
            binding.bottomNavigationContent,
            binding.clinicsNavItem,
            binding.libraryNavItem,
            binding.accountNavItem,
            binding.addNavItem
        ).forEach { view ->
            view.setOnTouchListener(swipeTouchListener)
        }
    }

    private fun isBottomBarAlwaysExpanded(destinationId: Int?): Boolean =
        destinationId == R.id.clinicsFragment ||
            destinationId == R.id.objLibraryFragment ||
            destinationId == R.id.objItemLibraryFragment ||
            destinationId == R.id.therapistsFragment ||
            destinationId == R.id.myAccountFragment

    private fun collapseBottomBar() {
        isBottomBarExpanded = false
        currentDestinationId?.let { destinationId -> updateBottomNavSelection(destinationId) }
    }

    private fun configureSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.slateIndigo)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
    }

    private fun configureBottomNavigationInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigationCard) { view, insets ->
            val navigationBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val spacing = (20 * resources.displayMetrics.density).toInt()
            val params = view.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.bottomMargin = navigationBottom + spacing
            view.layoutParams = params
            insets
        }
        ViewCompat.requestApplyInsets(binding.bottomNavigationCard)
    }

    private fun tintToolbarNavigationIcon() {
        val navigationIcon = binding.toolbar.navigationIcon ?: return
        val wrappedIcon = DrawableCompat.wrap(navigationIcon.mutate())
        DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, android.R.color.white))
        binding.toolbar.navigationIcon = wrappedIcon
    }

    private fun updateBottomNavSelection(destinationId: Int) {
        val selectedColor = ContextCompat.getColor(this, R.color.slateIndigo)
        val inactiveColor = ContextCompat.getColor(this, R.color.bottomNavInactive)
        val clinicsSelected = destinationId == R.id.clinicsFragment
        val librarySelected = destinationId == R.id.objLibraryFragment
        val addOnly = destinationId == R.id.objItemLibraryFragment ||
            destinationId == R.id.therapistsFragment
        val accountSelected = destinationId == R.id.myAccountFragment
        val addAvailable = destinationId == R.id.clinicsFragment ||
            destinationId == R.id.kidsFragment ||
            destinationId == R.id.objectivesFragment ||
            destinationId == R.id.objLibraryFragment ||
            destinationId == R.id.objItemLibraryFragment ||
            destinationId == R.id.therapistsFragment
        val addVisible = addOverrideVisible ?: addAvailable
        val showExpandedBar = isBottomBarAlwaysExpanded(destinationId) || isBottomBarExpanded

        binding.clinicsNavIcon.setColorFilter(if (clinicsSelected) selectedColor else inactiveColor)
        binding.clinicsNavText.setTextColor(if (clinicsSelected) selectedColor else inactiveColor)
        binding.libraryNavIcon.setColorFilter(if (librarySelected) selectedColor else inactiveColor)
        binding.libraryNavText.setTextColor(if (librarySelected) selectedColor else inactiveColor)
        binding.accountNavIcon.setColorFilter(if (accountSelected) selectedColor else inactiveColor)
        binding.accountNavText.setTextColor(if (accountSelected) selectedColor else inactiveColor)
        binding.addNavIcon.setColorFilter(selectedColor)
        binding.addNavText.setTextColor(selectedColor)
        binding.bottomNavigationContent.isVisible = showExpandedBar
        binding.collapsedNavDot.isVisible = !showExpandedBar
        binding.clinicsNavItem.isVisible = showExpandedBar && !addOnly
        binding.libraryNavItem.isVisible = showExpandedBar && !addOnly
        binding.accountNavItem.isVisible = showExpandedBar && (clinicsSelected || accountSelected)
        binding.addNavItem.isVisible = showExpandedBar && addVisible
        binding.addNavSeparator.isVisible = showExpandedBar && addVisible && !addOnly
        resizeBottomBar(expanded = showExpandedBar, addOnly = addOnly)
    }

    private fun resizeBottomBar(expanded: Boolean, addOnly: Boolean) {
        val targetWidth = when {
            !expanded -> 64
            addOnly -> 80
            else -> 312
        }
        val density = resources.displayMetrics.density
        val params = binding.bottomNavigationCard.layoutParams
        val targetWidthPx = (targetWidth * density).toInt()
        if (params.width != targetWidthPx) {
            params.width = targetWidthPx
            binding.bottomNavigationCard.layoutParams = params
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
    }

    fun updateBottomNav() {
        viewModel.takeAction(MainAction.UpdateAuthStatus)
    }

    fun setBottomAddVisible(isVisible: Boolean) {
        addOverrideVisible = isVisible
        currentDestinationId?.let { destinationId -> updateBottomNavSelection(destinationId) }
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
            val isOffline = !hasValidatedInternet
            val isAuthDestination = currentDestinationId == R.id.loginFragment ||
                currentDestinationId == R.id.registerFragment
            binding.offlineInfoTextView.isVisible = isOffline && !isAuthDestination
            binding.authOfflineInfoTextView.isVisible = isOffline && isAuthDestination
        }
    }
}
