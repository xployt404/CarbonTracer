package com.example.carbontracerrevised.statistics

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.carbontracerrevised.MainActivity
import com.example.carbontracerrevised.R
import com.example.carbontracerrevised.tracer.CONSUMER_PRODUCTS
import com.example.carbontracerrevised.tracer.ELECTRONICS
import com.example.carbontracerrevised.tracer.GROCERIES
import com.example.carbontracerrevised.tracer.TRANSPORT
import com.example.carbontracerrevised.tracer.Traceable
import com.example.carbontracerrevised.tracer.TraceableAdapter
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class StatisticsFragment(private val traceableAdapter: TraceableAdapter) : Fragment() {
    private fun updateListFromDatabase() = (activity as MainActivity).updateListFromDatabase()
    private lateinit var pieChart: PieChart
    private lateinit var barChart: HorizontalBarChart
    private var pieEntries = mutableListOf<PieEntry>()
    private var barEntries = mutableListOf<BarEntry>()
    private val traceablesWithCo2e = mutableListOf<Traceable>()

    companion object{
        const val TAG = "StatisticsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val statisticsFragment = inflater.inflate(R.layout.statistics_fragment, container, false)

        setupPieChart(statisticsFragment)
        setupBarChart(statisticsFragment)
        updateListFromDatabase()
        return statisticsFragment
    }

    override fun onResume() {
        super.onResume()
        updateStatistic()
    }

    @Suppress("DEPRECATION")
    private fun setupPieChart(view: View) {
        pieChart = view.findViewById(R.id.pieChart)
        pieChart.setDrawSliceText(false)
        pieChart.apply {
            elevation = 8f
            extraBottomOffset = 15f
            isDrawHoleEnabled = false
            description.text = "CO2 per category"
            description.textSize = 20f
            description.textColor = Color.WHITE
            description.textAlign = Paint.Align.LEFT
            description.setPosition(400f, 850f)
            legend.apply {
                textColor = Color.WHITE
                orientation = Legend.LegendOrientation.VERTICAL
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                setDrawInside(false)
            }

            setUsePercentValues(true)
            animateY(1000)
            isHighlightPerTapEnabled = true
        }
    }

    private fun setupBarChart(view: View) {
        barChart = view.findViewById(R.id.barChart)
        barChart.apply {
            description.apply {
                text = "Top yearly Emitters(kg)"
                textSize = 20f
                xOffset = -100f
                textColor = Color.WHITE
                yOffset = -20f
            }
            extraBottomOffset = 10f
            legend.textColor = Color.WHITE
            xAxis.apply {
                textColor = Color.WHITE
                setGranularity(1f)
                isGranularityEnabled = true
            }
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            animateY(1200)
        }
    }

    private fun updatePieChart(){
        var co2eGroceries = 0F
        var co2eTransport= 0F
        var co2eConsumer = 0F
        var co2eElectronics = 0F
        var co2eMisc = 0F
        for (t in traceablesWithCo2e){
            val co2e = t.co2e.replace(Regex("[^\\d.]"), "").toFloat()
            Log.d(TAG, "updatePieChart: $co2e")
            when(t.category){
                GROCERIES -> co2eGroceries += co2e
                CONSUMER_PRODUCTS -> co2eConsumer += co2e
                ELECTRONICS -> co2eElectronics += co2e
                TRANSPORT -> co2eTransport += co2e
                else -> co2eMisc += co2e
            }

        }
        val total = co2eGroceries + co2eTransport + co2eConsumer + co2eElectronics + co2eMisc
        Log.i(TAG, "updatePieChart total: $total")
        pieEntries.clear()
        pieEntries.addAll(
            listOf(
            PieEntry((co2eGroceries/total)*100, "Groceries"),
            PieEntry((co2eConsumer/total)*100, "Consumer Products"),
            PieEntry((co2eElectronics/total)*100, "Electronics"),
            PieEntry((co2eTransport/total)*100, "Transport"),
            PieEntry((co2eMisc/total)*100, "Misc")
             )
        )
        pieEntries.forEach {
            Log.i("entries", it.toString())
        }
        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
                return String.format(Locale.US,"%.1f%%", value) // Format as percentage
            }
        }
        pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        pieDataSet.colors.add(Color.LTGRAY)
        val pieData = PieData(pieDataSet)

        pieChart.data = pieData
        pieChart.data.setDrawValues(false)
        // Create custom legend entries
        val legendEntries = pieEntries.map { pieEntry ->
            LegendEntry(
                "${pieEntry.label}: ${String.format(Locale.US,"%.1f%%", pieEntry.value)}",
                Legend.LegendForm.DEFAULT,
                12f, // Text size
                5f,  // Form size
                null, // Form color (null for default)
                pieDataSet.getColor(pieEntries.indexOf(pieEntry)) // Get color from dataset
            )
        }.toMutableList()

        // Set custom legend entries
        pieChart.legend.setCustom(legendEntries)
    }

    private fun updateBarChart(){

        val sortedTraceables = traceablesWithCo2e.sortedBy { it.co2e.replace(Regex("[^\\d.]"), "").toFloat() }
        barEntries.clear()

        sortedTraceables.forEachIndexed { index, traceable ->
            println(traceable.co2e.replace(Regex("[^\\d.]"), "").toFloat())
            barEntries.add(
                BarEntry(
                    index.toFloat(), traceable.co2e.replace(Regex("[^\\d.]"), "").toFloat()
                )
            )
        }

        val labels = sortedTraceables.map { it.objectName }
        val barDataSet = BarDataSet(barEntries, "Data Set")
        barDataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()
        val barData = BarData(barDataSet)

        barChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            data = barData
            invalidate()
        }
    }

    private fun updateStatistic() {
        traceablesWithCo2e.clear()
        for (t in traceableAdapter.traceableList){
            if (t.co2e.isNotEmpty()){
                traceablesWithCo2e.add(t)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            updatePieChart()
        }
         updateBarChart()
    }
}