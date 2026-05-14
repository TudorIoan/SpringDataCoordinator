package net.abaresults.progresspath.view.report

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.databinding.FragmentReportBinding
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class ReportFragment : BaseFragment() {

    private lateinit var binding: FragmentReportBinding
    private val viewModel: ReportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(ReportAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(viewModel: ReportViewModel) {
        val stateObserver = Observer<ReportState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            Log.d("ReportFragment", "State changed to: $it")

            // Hide the loading state
            if (it != ReportState.Loading) {
                hideLoading()
            }

            when (it) {
                is ReportState.Loading -> {
                    Log.d("ReportFragment", "Showing loading state")
                    showLoading()
                }
                is ReportState.Error -> {
                    Log.e("ReportFragment", "Error state: ${it.generalError}")
                    showError(it.generalError)
                }
                is ReportState.Idle -> {
                    Log.d("ReportFragment", "Idle state")
                    // Show a test message to confirm fragment is working
                    binding.pdfPlaceholder.isVisible = true
                    binding.pdfPlaceholder.text = "Report fragment loaded. Waiting for report data..."
                }
                is ReportState.ContentLoaded -> {
                    Log.d("ReportFragment", "Content loaded state")
                    handleContentLoaded(it.pdfData)
                }
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        showTopBar()
        showBottomBar()

        // Configure WebView for PDF display
        binding.pdfWebView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }
    }

    private fun handleContentLoaded(pdfData: ByteArray) {
        Log.d("ReportFragment", "handleContentLoaded called with ${pdfData.size} bytes")

        try {
            // Save PDF to a temporary file
            val pdfFile = savePdfToFile(pdfData)
            Log.d("ReportFragment", "PDF saved to file: ${pdfFile.absolutePath}")

            // Since Android WebViews generally cannot display PDFs natively,
            // show a user-friendly message with options
            binding.pdfWebView.isVisible = false
            binding.pdfPlaceholder.isVisible = true
            binding.pdfPlaceholder.text = "✅ PDF Report Generated Successfully!\n\n📄 Tap to open with PDF viewer\n\n📊 Report contains:\n• Objective details\n• Items progress table\n• Kid and clinic information"
            binding.pdfPlaceholder.setOnClickListener {
                openPdfWithExternalApp(pdfFile)
            }

        } catch (e: Exception) {
            Log.e("ReportFragment", "Error handling PDF", e)
            showError("Error displaying PDF: ${e.message}")
        }
    }

    private fun savePdfToFile(pdfData: ByteArray): File {
        val file = File(requireContext().cacheDir, "objective_report.pdf")
        FileOutputStream(file).use { output ->
            output.write(pdfData)
        }
        return file
    }

    private fun openPdfWithExternalApp(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ReportFragment", "Error opening PDF", e)
            showError("No PDF viewer available. Please install a PDF reader app.")
        }
    }

    private fun displayPdfInWebView(pdfData: ByteArray, pdfFile: File) {
        Log.d("ReportFragment", "Attempting to display PDF in WebView")

        // Enhanced WebView client to detect PDF loading issues
        binding.pdfWebView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("ReportFragment", "WebView page finished loading: $url")

                // Check if WebView actually shows content or just a blank/error page
                binding.pdfWebView.postDelayed({
                    // Most Android WebViews can't display PDFs natively, so show fallback
                    Log.d("ReportFragment", "WebView likely cannot display PDF natively, showing fallback")
                    showPdfFallback(pdfFile)
                }, 2000)
            }

            override fun onReceivedError(
                view: android.webkit.WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                Log.e("ReportFragment", "WebView error: $description (code: $errorCode)")
                showPdfFallback(pdfFile)
            }
        }

        // Try multiple approaches to display PDF in WebView

        // Approach 1: Try Google Docs Viewer (most reliable for PDFs)
        try {
            val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=" +
                Uri.encode("file://${pdfFile.absolutePath}")
            Log.d("ReportFragment", "Trying Google Docs viewer approach: $googleDocsUrl")
            binding.pdfWebView.loadUrl(googleDocsUrl)

            // If Google Docs approach fails, try other methods
            binding.pdfWebView.postDelayed({
                tryDataUrlApproach(pdfData, pdfFile)
            }, 4000)

        } catch (e: Exception) {
            Log.e("ReportFragment", "Error with Google Docs approach", e)
            tryDataUrlApproach(pdfData, pdfFile)
        }
    }

    private fun tryDataUrlApproach(pdfData: ByteArray, pdfFile: File) {
        // Approach 2: Try data URL with base64
        try {
            val base64Pdf = Base64.encodeToString(pdfData, Base64.DEFAULT)
            val dataUrl = "data:application/pdf;base64,$base64Pdf"

            Log.d("ReportFragment", "Trying data URL approach")
            binding.pdfWebView.loadUrl(dataUrl)

            // Most Android WebViews don't support PDF display, so show fallback after a short delay
            binding.pdfWebView.postDelayed({
                Log.d("ReportFragment", "Data URL approach completed, showing fallback as WebView likely doesn't support PDF")
                showPdfFallback(pdfFile)
            }, 3000)

        } catch (e: Exception) {
            Log.e("ReportFragment", "Error with data URL approach", e)
            showPdfFallback(pdfFile)
        }
    }

    private fun showPdfFallback(pdfFile: File) {
        binding.pdfWebView.isVisible = false
        binding.pdfPlaceholder.isVisible = true
        binding.pdfPlaceholder.text = "PDF generated successfully!\nTap here to open with PDF viewer"
        binding.pdfPlaceholder.setOnClickListener {
            openPdfWithExternalApp(pdfFile)
        }
    }

    private fun showError(generalError: String) {
        binding.errorTextView.text = generalError
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun showLoading() =
        binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() =
        binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}