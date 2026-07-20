package app.springdata.coordinator.view.obj_item_library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.DialogEditObjItemBinding
import app.springdata.coordinator.databinding.FragmentObjItemLibraryBinding
import app.springdata.coordinator.model.ObjItem
import app.springdata.coordinator.model.ObjItemType

@AndroidEntryPoint
class ObjItemLibraryFragment : BaseFragment() {

    private lateinit var binding: FragmentObjItemLibraryBinding
    private val viewModel: ObjItemLibraryViewModel by viewModels()

    private lateinit var objItemsAdapter: ObjItemLibraryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentObjItemLibraryBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(ObjItemLibraryAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: ObjItemLibraryViewModel) {
        val stateObserver = Observer<ObjItemLibraryState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != ObjItemLibraryState.Loading) {
                hideLoading()
            }

            when (it) {
                is ObjItemLibraryState.Loading -> showLoading()
                is ObjItemLibraryState.Error -> showError(it.generalError)
                is ObjItemLibraryState.Idle -> {}
                is ObjItemLibraryState.ContentLoaded -> handleContentLoaded(it.items)
                is ObjItemLibraryState.GoToObjLibrary -> navigateToObjLibrary()
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.subtitleInclude.subtitleTextView.isVisible = true
            binding.subtitleInclude.subtitleTextView.text = title
        }
    }

    private fun configureViews() {
        showBottomBar()

        objItemsAdapter = ObjItemLibraryAdapter(
            onItemEdit = { item ->
                showEditDialog(item)
            },
            onItemRemove = { item ->
                showRemoveDialog(item)
            }
        )

        binding.objItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = objItemsAdapter
        }

        binding.fabAddItem.setOnClickListener {
            findNavController().navigate(R.id.addObjItemFragment)
        }
    }

    private fun handleContentLoaded(items: List<ObjItem>) {
        objItemsAdapter.updateData(items)
    }

    private fun showError(generalError: String) {
        binding.subtitleInclude.errorTextView.text = generalError
        binding.subtitleInclude.errorTextView.visibility = View.VISIBLE
    }

    private fun navigateToObjLibrary() {
        findNavController().navigate(R.id.objLibraryFragment)
    }

    private fun showLoading() =
        binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() =
        binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showEditDialog(item: ObjItem) {
        val dialogBinding = DialogEditObjItemBinding.inflate(layoutInflater)

        // Item name
        dialogBinding.itemNameEditText.setText(item.name)

        // Item type dropdown
        val itemTypes = ObjItemType.getAllDisplayNames()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, itemTypes)
        dialogBinding.itemTypeTextView.setAdapter(adapter)
        dialogBinding.itemTypeTextView.setText(item.type.displayName, false)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Item")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newName = dialogBinding.itemNameEditText.text.toString().trim()
                val newType = ObjItemType.fromDisplayName(dialogBinding.itemTypeTextView.text.toString())
                if (newName.isNotEmpty()) {
                    val updatedItem = item.copy(name = newName, type = newType)
                    viewModel.takeAction(ObjItemLibraryAction.UpdateItem(updatedItem))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveDialog(item: ObjItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Item")
            .setMessage("Are you sure you want to remove '${item.name}'?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.takeAction(ObjItemLibraryAction.RemoveItem(item))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
