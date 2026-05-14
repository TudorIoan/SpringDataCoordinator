package net.abaresults.progresspath.view.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.abaresults.progresspath.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        observeData(binding, viewModel)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureViews()
    }

    private fun observeData(binding: FragmentHomeBinding, viewModel: HomeViewModel) {
        val stateObserver = Observer<HomeState?> {
            // null state indicates there is no action needed
            it ?: return@Observer

            // Hide the loading state
            if (it != HomeState.Loading) {
                hideLoading()
            }

            when (it) {
                is HomeState.Loading -> showLoading()
                is HomeState.Error -> {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                is HomeState.Content -> {
                    handleContent(it.stateData)
                }

                HomeState.Idle -> {}
            }
        }
        viewModel.state.observe(viewLifecycleOwner, stateObserver)
        viewModel.takeAction(HomeAction.Start)
    }

    private fun configureViews() {
        showBottomBar()
        binding.carousel.adapter = CarouselPhotosAdapter(this)
        TabLayoutMediator(binding.carouselIndicator, binding.carousel) { _, _ -> }.attach()
        binding.carousel.offscreenPageLimit = 3
    }

    private fun handleContent(stateData: HomeStateData) {
        (binding.carousel.adapter as CarouselPhotosAdapter).set(stateData.photos.subList(0, 10))
    }

    private fun showLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.VISIBLE }

    private fun hideLoading() = binding.loadingViewInclude.loadingView.apply { visibility = View.GONE }
}