package com.example.budgettracker.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class StatsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var tvShopStats: TextView
    private val viewModel: StatsViewModel by viewModels {
        StatsViewModel.StatsViewModelFactory((application as com.example.budgettracker.BudgetApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.statistics)

        pieChart = findViewById(R.id.pieChart)
        tvShopStats = findViewById(R.id.tvShopStats)

        viewModel.shopStats.observe(this) { stats ->
            setupPieChart(stats)
            updateShopList(stats)
        }
    }

    private fun setupPieChart(stats: List<com.example.budgettracker.data.model.ShopStat>) {
        val entries = stats.map { PieEntry(it.total.toFloat(), it.shop) }
        if (entries.isEmpty()) return
        val dataSet = PieDataSet(entries, getString(R.string.shop_stats))
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = getString(R.string.shop_stats)
        pieChart.invalidate()
    }

    private fun updateShopList(stats: List<com.example.budgettracker.data.model.ShopStat>) {
        val builder = StringBuilder()
        stats.forEach {
            builder.append("${it.shop}: ${String.format("%.2f", it.total)} ₽\n")
        }
        tvShopStats.text = builder.toString()
    }
}
