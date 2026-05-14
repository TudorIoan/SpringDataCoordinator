package net.abaresults.progresspath.view.objectives

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import net.abaresults.progresspath.databinding.FragmentObjectivesBinding
import net.abaresults.progresspath.model.KidObjective
import net.abaresults.progresspath.model.UserType
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class ObjectivesFragment : BaseFragment() {

    private lateinit var binding: FragmentObjectivesBinding
    private val viewModel: ObjectivesViewModel by viewModels()

    private lateinit var objectivesAdapter: ObjectivesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentObjectivesBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(ObjectivesAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun addMenuButtons() {
        // Setup toolbar buttons
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.objectives_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_therapists -> {
                        findNavController().navigate(R.id.therapistsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeData(viewModel: ObjectivesViewModel) {
        val stateObserver = Observer<ObjectivesState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != ObjectivesState.Loading) {
                hideLoading()
            }

            when (it) {
                is ObjectivesState.Loading -> showLoading()
                is ObjectivesState.Error -> showError(it.generalError)
                is ObjectivesState.Idle -> {}
                is ObjectivesState.ContentLoaded -> handleContentLoaded(it.items)
                is ObjectivesState.GoToItems -> navigateToItems()
                is ObjectivesState.GoToReport -> navigateToReport(it.pdfByteArray)
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.subtitleInclude.subtitleTextView.isVisible = true
            binding.subtitleInclude.subtitleTextView.text = title
        }

        viewModel.userType.observe(viewLifecycleOwner) { userType ->
            objectivesAdapter.userType = userType
            binding.fabAddObjective.isVisible = userType == UserType.COORDINATOR
        }
    }

    private fun configureViews() {
        showBottomBar()

        objectivesAdapter = ObjectivesAdapter(
            onItemClicked = { objectiveListItem ->
                viewModel.takeAction(ObjectivesAction.ObjectiveClicked(objectiveListItem))
            },
            onToggleClicked = { kidObjective, isActive ->
                viewModel.takeAction(ObjectivesAction.ToggleObjectiveActive(kidObjective, isActive))
            },
            onGenerateObjectiveReport = { kidObjective, objectiveName ->
                viewModel.takeAction(ObjectivesAction.GenerateObjectiveReport(kidObjective))
            },
            onItemRemove = { kidObjective ->
                showRemoveDialog(kidObjective)
            }
        )

        binding.objectivesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = objectivesAdapter
        }

        binding.fabAddObjective.setOnClickListener {
            findNavController().navigate(R.id.addObjectiveFragment)
        }

        addMenuButtons()
    }

    private fun navigateToItems() {
        findNavController().navigate(R.id.itemsFragment)
    }

    private fun handleContentLoaded(objectives: List<ObjectivesListItem>) {
        objectivesAdapter.set(objectives)
    }

    private fun showError(generalError: String) {
        binding.subtitleInclude.errorTextView.text = generalError
        binding.subtitleInclude.errorTextView.visibility = View.VISIBLE
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showRemoveDialog(kidObjective: KidObjective) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Objective")
            .setMessage("Are you sure you want to remove this objective from this kid?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(ObjectivesAction.RemoveObjective(kidObjective))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToReport(pdfByteArray: ByteArray) {
        //findNavController().navigate(R.id.reportFragment)

        val pdfFile = savePdfToFile(pdfByteArray)
        openPdfWithExternalApp(pdfFile)
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
}