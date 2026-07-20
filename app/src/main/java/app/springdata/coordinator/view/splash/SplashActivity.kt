package app.springdata.coordinator.view.splash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.ActivitySplashBinding
import app.springdata.coordinator.view.main.MainActivity
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
                is SplashState.UpdateRequired -> {
                    showUpdateRequiredDialog(it.message)
                }
                is SplashState.Idle -> {}
            }
        }
        viewModel.state.observe(this, stateObserver)
    }

    private fun showUpdateRequiredDialog(message: String) {
        val updateMessage = message.ifBlank { "Please update the app." }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Update Required")
            .setMessage(updateMessage)
            .setCancelable(false)
            .setPositiveButton("Update", null)
            .show()

        dialog.findViewById<TextView>(android.R.id.message)?.apply {
            Linkify.addLinks(this, Linkify.WEB_URLS)
            movementMethod = LinkMovementMethod.getInstance()
            linksClickable = true
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                openUpdateLink(updateMessage)
        }
    }

    private fun openUpdateLink(message: String) {
        val url = Regex("""https?://\S+""").find(message)?.value ?: return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun navigateToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}
