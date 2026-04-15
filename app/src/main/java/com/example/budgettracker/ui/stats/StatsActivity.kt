package com.example.budgettracker.ui.stats

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.R
import com.example.budgettracker.databinding.ActivityStatsBinding
import com.example.budgettracker.data.model.ShopStat
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private val viewModel: StatsViewModel by viewModels {
        StatsViewModel.StatsViewModelFactory((application as com.example.budgettracker.BudgetApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.statistics)

        viewModel.shopStats.observe(this) { stats ->
            setupPieChart(stats)
            updateShopList(stats)
        }
    }

    private fun setupPieChart(stats: List<ShopStat>) {
        val entries = stats.map { PieEntry(it.total.toFloat(), it.shop) }
        if (entries.isEmpty()) return
        val dataSet = PieDataSet(entries, getString(R.string.shop_stats))
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        val pieData = PieData(dataSet)
        binding.pieChart.data = pieData
        binding.pieChart.description.isEnabled = false
        binding.pieChart.centerText = getString(R.string.shop_stats)
        binding.pieChart.invalidate()
    }

    private fun updateShopList(stats: List<ShopStat>) {
        val builder = StringBuilder()
        stats.forEach {
            builder.append("${it.shop}: ${String.format("%.2f", it.total)} ₽\n")
        }
        binding.tvShopStats.text = builder.toString()
    }
}
