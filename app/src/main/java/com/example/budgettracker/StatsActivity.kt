package com.example.budgettracker

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONArray

class StatsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        val categoryMap = mutableMapOf<String, Double>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val type = obj.optString("type", "expense")
            if (type == "expense") {
                val category = obj.getString("category")
                val amount = obj.getDouble("amount")
                categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + amount
            }
        }

        val pieChart = findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.pieChart)
        val entries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }
        if (entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "Расходы по категориям")
            dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK
            val pieData = PieData(dataSet)
            pieChart.data = pieData
            pieChart.description.isEnabled = false
            pieChart.centerText = "Расходы"
            pieChart.setCenterTextSize(16f)
            pieChart.invalidate()
        }
    }
}
