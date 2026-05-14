package net.abaresults.progresspath.view.chart

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import net.abaresults.progresspath.BaseFragment
import net.abaresults.progresspath.R
import net.abaresults.progresspath.databinding.FragmentChartBinding
import net.abaresults.progresspath.model.KidObjectiveItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class ChartFragment : BaseFragment() {

    private lateinit var binding: FragmentChartBinding
    private val viewModel: ChartViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChartBinding.inflate(inflater, container, false)
        observeData()
        viewModel.takeAction(ChartAction.Start)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.chart_toolbar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_share -> {
                        shareChart()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun shareChart() {
        val bitmap = binding.lineChart.chartBitmap
        val file = File(requireContext().cacheDir, "chart.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Chart"))
    }

    private fun observeData() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            when (it) {
                is ChartState.Idle -> {}
                is ChartState.ContentLoaded -> setupChart(it.items)
            }
        })
    }

    private fun setupChart(items: List<KidObjectiveItem>) {
        val lineColors = listOf(
            Color.rgb(66, 133, 244),   // Blue
            Color.rgb(234, 67, 53),    // Red
            Color.rgb(52, 168, 83),    // Green
            Color.rgb(251, 188, 4),    // Yellow
            Color.rgb(171, 71, 188),   // Purple
            Color.rgb(255, 112, 67),   // Orange
            Color.rgb(0, 172, 193),    // Teal
            Color.rgb(124, 179, 66),   // Light Green
            Color.rgb(233, 30, 99),    // Pink
            Color.rgb(63, 81, 181)     // Indigo
        )

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val longestFrequencyList = items.maxByOrNull { it.frequencyList.size }
            ?.frequencyList.orEmpty()
        val xLabels = longestFrequencyList.mapIndexed { i, freqItem ->
            freqItem.date?.let { dateFormat.format(it) } ?: "$i"
        }

        val dataSets = items.mapIndexed { index, item ->
            val entries = item.frequencyList.mapIndexed { i, freqItem ->
                Entry(i.toFloat(), freqItem.frequency.toFloat())
            }

            val color = lineColors[index % lineColors.size]
            LineDataSet(entries, item.objItem.name).apply {
                this.color = color
                setCircleColor(color)
                lineWidth = 2f
                circleRadius = 4f
                valueTextSize = 10f
                setDrawValues(true)
            }
        }

        binding.lineChart.apply {
            data = LineData(dataSets)
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)

            axisLeft.granularity = 1f
            axisRight.isEnabled = false

            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            animateX(500)
            invalidate()
        }
    }
}
