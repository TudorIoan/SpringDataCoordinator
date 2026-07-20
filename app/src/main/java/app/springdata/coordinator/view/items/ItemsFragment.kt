package app.springdata.coordinator.view.items

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import app.springdata.coordinator.BaseFragment
import app.springdata.coordinator.R
import app.springdata.coordinator.databinding.DialogFrequencyProgressBinding
import app.springdata.coordinator.databinding.FragmentItemsBinding
import app.springdata.coordinator.model.KidObjectiveItem
import app.springdata.coordinator.model.ObjItemType

@AndroidEntryPoint
class      ItemsFragment : BaseFragment() {

    private lateinit var binding: FragmentItemsBinding
    private val viewModel: ItemsViewModel by viewModels()

    private lateinit var itemsAdapter: ItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentItemsBinding.inflate(inflater, container, false)
        observeData(viewModel)
        viewModel.takeAction(ItemAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    override fun onResume() {
        super.onResume()
        // Set soft input mode to adjustPan for this fragment
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onPause() {
        super.onPause()
        // Restore original soft input mode (adjustResize)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun observeData(viewModel: ItemsViewModel) {
        val stateObserver = Observer<ItemState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != ItemState.Loading) {
                hideLoading()
            }

            when (it) {
                is ItemState.Loading -> showLoading()
                is ItemState.Error -> showError(it.generalError)
                is ItemState.Idle -> {}
                is ItemState.ContentLoaded -> handleContentLoaded(it.items)
                is ItemState.GoToObjectives -> navigateToObjectives()
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

        itemsAdapter = ItemsAdapter(
            onYesClicked = { item ->
                viewModel.takeAction(ItemAction.ItemYesClicked(item))
            },
            onNoClicked = { item ->
                viewModel.takeAction(ItemAction.ItemNoClicked(item))
            },
            onToggleClicked = { item ->
                viewModel.takeAction(ItemAction.ItemToggleClicked(item))
            },
            onFrequencySet = { item, freq ->
                viewModel.takeAction(ItemAction.FrequencySet(item, freq))
            },
            onProgressSet = { item, progress ->
                viewModel.takeAction(ItemAction.ProgressSet(item, progress))
            },
            onShowProgress = { item ->
                showProgressDialog(item)
            },
            onSetMastered = { item ->
                viewModel.takeAction(ItemAction.SetMastered(item))
            },
            onCheckmarkClicked = { item ->
                viewModel.takeAction(ItemAction.CheckmarkClicked(item))
            }
        )

        binding.itemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemsAdapter
        }
    }

    private fun handleContentLoaded(items: List<KidObjectiveItem>) {
        itemsAdapter.updateData(items)

        val allFrequency = items.isNotEmpty() && items.all { it.objItem.type == ObjItemType.FREQUENCY }

        if (allFrequency) {
            showChartMenu()
        }
    }

    private fun showChartMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.items_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_chart -> {
                        findNavController().navigate(R.id.chartFragment)
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

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }

    private fun showProgressDialog(item: KidObjectiveItem) {
        val dialogBinding = DialogFrequencyProgressBinding.inflate(layoutInflater)

        dialogBinding.itemNameText.text = item.objItem.name

        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        val entries: List<Pair<String, String>> = when (item.objItem.type) {
            ObjItemType.FREQUENCY -> item.frequencyList.map {
                it.frequency.toString() to (it.date?.let { d -> dateFormat.format(d) } ?: "No date")
            }
            ObjItemType.CHECKMARK -> item.checkmarkList.map {
                dateFormat.format(it) to ""
            }
            ObjItemType.PERCENTAGE -> item.percentageList.map {
                "${it.progress}%" to (it.date?.let { d -> dateFormat.format(d) } ?: "No date")
            }
            ObjItemType.YES_NO -> item.yesNoList.map {
                (if (it.yes) getString(R.string.yes) else getString(R.string.no)) to (it.date?.let { d -> dateFormat.format(d) } ?: "No date")
            }
        }

        val adapter: RecyclerView.Adapter<*>? = if (entries.isEmpty()) null else
            ItemProgressAdapter().apply { updateData(entries) }

        if (adapter != null) {
            dialogBinding.noProgressText.isVisible = false
            dialogBinding.frequencyRecyclerView.isVisible = true
            dialogBinding.frequencyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            dialogBinding.frequencyRecyclerView.adapter = adapter
        } else {
            dialogBinding.frequencyRecyclerView.isVisible = false
            dialogBinding.noProgressText.isVisible = true
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}
