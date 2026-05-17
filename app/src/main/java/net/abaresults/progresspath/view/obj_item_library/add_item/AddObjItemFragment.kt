package net.abaresults.progresspath.view.obj_item_library.add_item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import android.os.Handler
import android.os.Looper
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.databinding.FragmentAddObjItemBinding
import net.abaresults.progresspath.model.ObjItemType

@AndroidEntryPoint
class AddObjItemFragment : BaseFragment() {

    private lateinit var binding: FragmentAddObjItemBinding
    private val viewModel: AddObjItemViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddObjItemBinding.inflate(inflater, container, false)
        observeData(viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViews()
    }

    private fun observeData(viewModel: AddObjItemViewModel) {
        val stateObserver = Observer<AddObjItemState?> {
            it ?: return@Observer

            if (it != AddObjItemState.Loading) {
                hideLoading()
            }

            when (it) {
                is AddObjItemState.Loading -> showLoading()
                is AddObjItemState.Error -> showError(it.error)
                is AddObjItemState.ItemAdded -> showItemAddedMessage(it.itemName)
                is AddObjItemState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
    }

    private fun configureViews() {
        hideBottomBar()

        // Setup item type dropdown
        val itemTypes = ObjItemType.getAllDisplayNames()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, itemTypes)
        binding.itemTypeTextView.setAdapter(adapter)
        binding.itemTypeTextView.setText(itemTypes[0], false)

        binding.addItemButton.setOnClickListener {
            val itemName = binding.itemNameEditText.text.toString()
            val itemTypeStr = binding.itemTypeTextView.text.toString()
            val itemType = ObjItemType.fromDisplayName(itemTypeStr)
            viewModel.takeAction(AddObjItemAction.AddItemClicked(itemName, itemType))
        }
    }

    private fun showError(generalError: String) {
        binding.errorTextView.text = generalError
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun showItemAddedMessage(itemName: String) {
        // Update the success message text with the item name
        binding.successMessageText.text = "Item $itemName was added."

        // Make the view visible and animate it down
        binding.successMessageView.apply {
            visibility = View.VISIBLE
            animate()
                .translationY(0f)
                .setDuration(300)
                .withEndAction {
                    // After animation completes, wait 2 seconds then animate back up
                    Handler(Looper.getMainLooper()).postDelayed({
                        animate()
                            .translationY(-100f)
                            .setDuration(300)
                            .withEndAction {
                                visibility = View.GONE
                                binding.itemNameEditText.text?.clear()
                            }
                    }, 1000)
                }
        }
    }

    private fun navigateBack() {
        findNavController().popBackStack()
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}
