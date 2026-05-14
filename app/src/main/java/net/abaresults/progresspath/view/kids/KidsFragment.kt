package net.abaresults.progresspath.view.kids

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.FragmentKidsBinding
import net.abaresults.progresspath.model.Kid
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.view.objectives.ObjectivesAction
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class KidsFragment : BaseFragment() {

    private lateinit var binding: FragmentKidsBinding
    private val viewModel: KidsViewModel by viewModels()

    private lateinit var kidsAdapter: KidsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentKidsBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(KidsAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(viewModel: KidsViewModel) {
        val stateObserver = Observer<KidsState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != KidsState.Loading) {
                hideLoading()
            }

            when (it) {
                is KidsState.Loading -> showLoading()
                is KidsState.Error -> showError(it.generalError)
                is KidsState.Idle -> {}
                is KidsState.ContentLoaded -> handleContentLoaded(it.items, it.userType)
                is KidsState.GoToObjectives -> navigateToObjectives()
                is KidsState.GoToReport -> handleGoToReport(it.pdfData)
                is KidsState.InsufficientActiveItems -> showInsufficientActiveItemsDialog(it.objectiveNames)
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.subtitleInclude.subtitleTextView.isVisible = true
            binding.subtitleInclude.subtitleTextView.text = title
        }

        viewModel.userType.observe(viewLifecycleOwner) { userType ->
            kidsAdapter.userType = userType
            binding.fabAddKid.isVisible = userType == UserType.COORDINATOR
        }
    }

    private fun configureViews() {
        showBottomBar()

        kidsAdapter = KidsAdapter(
            onItemClicked = { kid ->
                viewModel.takeAction(KidsAction.KidClicked(kid))
            },
            onItemEdit = { kid ->
                showEditDialog(kid)
            },
            onItemRemove = { kid ->
                showRemoveDialog(kid)
            },
            onGenerateKidWorksheet = {
                viewModel.takeAction(KidsAction.GenerateKidWorksheet(it))
            },
        )

        binding.kidsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kidsAdapter
        }

        binding.fabAddKid.setOnClickListener {
            findNavController().navigate(R.id.addKidFragment)
        }
    }

    private fun handleContentLoaded(kids: List<Kid>, userType: UserType) {
        kidsAdapter.updateData(kids)
        addMenuButtons(userType)
    }

    private fun addMenuButtons(userType: UserType) {
        // Setup toolbar buttons
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.clinics_toolbar_menu, menu)

                val coordinatorItem = menu.findItem(R.id.action_coordinator)
                val therapistsItem = menu.findItem(R.id.action_therapists)

                when (userType) {
                    UserType.COORDINATOR -> {
                        coordinatorItem?.isVisible = false
                        therapistsItem?.isVisible = true
                    }
                    UserType.THERAPIST -> {
                        coordinatorItem?.isVisible = true
                        therapistsItem?.isVisible = false
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_coordinator -> {
                        findNavController().navigate(R.id.coordinatorFragment)
                        true
                    }
                    R.id.action_therapists -> {
                        findNavController().navigate(R.id.therapistsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showError(generalError: String) {
        binding.subtitleInclude.errorTextView.text = generalError
        binding.subtitleInclude.errorTextView.visibility = View.VISIBLE
    }

    private fun navigateToObjectives() {
        findNavController().navigate(R.id.objectivesFragment)
    }

    private fun handleGoToReport(pdfData: ByteArray) {
        try {
            val pdfFile = savePdfToFile(pdfData)
            openPdfWithExternalApp(pdfFile)
        } catch (e: Exception) {
            Log.e("KidsFragment", "Error handling PDF", e)
            showError("Error displaying PDF: ${e.message}")
        }
    }

    private fun savePdfToFile(pdfData: ByteArray): File {
        val file = File(requireContext().cacheDir, "kid_worksheet.pdf")
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
            Log.e("KidsFragment", "Error opening PDF", e)
            showError("No PDF viewer available. Please install a PDF reader app.")
        }
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showEditDialog(kid: Kid) {
        val editText = EditText(requireContext()).apply {
            setText(kid.name)
            hint = "Kid name"
        }
        val container = LinearLayout(requireContext()).apply {
            setPadding(48, 0, 48, 0)
            addView(editText)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Kid")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.takeAction(KidsAction.UpdateKidName(kid.copy(name = newName)))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showInsufficientActiveItemsDialog(objectiveNames: List<String>) {
        val list = objectiveNames.joinToString("\n") { "• $it" }
        AlertDialog.Builder(requireContext())
            .setTitle("Insufficient Active Items")
            .setMessage("These objectives have less than 4 active items. Please add more:\n\n$list")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showRemoveDialog(kid: Kid) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Kid")
            .setMessage("Are you sure you want to remove ${kid.name}?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(KidsAction.RemoveKid(kid))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}