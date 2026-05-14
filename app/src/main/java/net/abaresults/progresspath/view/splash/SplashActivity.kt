package net.abaresults.progresspath.view.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.ActivitySplashBinding
import net.abaresults.progresspath.view.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel:SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,
            R.layout.activity_splash
        )
        observeData(viewModel)

        viewModel.takeAction(SplashAction.Start)
    }

    private fun observeData(viewModel: SplashViewModel) {
        val stateObserver = Observer<SplashState?> {
            it ?: return@Observer

            if (it != SplashState.Loading) {
                hideLoading()
            }

            when (it) {
                is SplashState.Loading -> showLoading()
                is SplashState.Error -> {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
                is SplashState.ContentLoaded -> {
                    navigateToMainScreen()
                }
                is SplashState.Idle -> {}
            }
        }
        viewModel.state.observe(this, stateObserver)
    }

    private fun navigateToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}