package com.example.budgettracker

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var btnPie: Button
    private lateinit var btnLine: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)
        btnPie = findViewById(R.id.btnPie)
        btnLine = findViewById(R.id.btnLine)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        btnPie.setOnClickListener {
            pieChart.visibility = android.view.View.VISIBLE
            lineChart.visibility = android.view.View.GONE
            loadPieChart()
        }
        btnLine.setOnClickListener {
            pieChart.visibility = android.view.View.GONE
            lineChart.visibility = android.view.View.VISIBLE
            loadLineChart()
        }
        loadPieChart()
    }

    private fun loadPieChart() {
        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        val categoryMap = mutableMapOf<String, Double>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("type") == "expense") {
                val category = obj.getString("category")
                val amount = obj.getDouble("amount")
                categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + amount
            }
        }
        val entries = categoryMap.map { PieEntry(it.value.toFloat(), it.key) }
        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("Нет данных о расходах")
            return
        }
        val dataSet = PieDataSet(entries, "Расходы по категориям")
        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.centerText = "Расходы"
        pieChart.invalidate()
    }

    private fun loadLineChart() {
        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        val expenseByDay = mutableMapOf<String, Float>()
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("type") == "expense") {
                val date = obj.getString("date")
                val amount = obj.getDouble("amount").toFloat()
                expenseByDay[date] = expenseByDay.getOrDefault(date, 0f) + amount
            }
        }
        val sortedDates = expenseByDay.keys.sortedWith(compareBy { parseDate(it) })
        val entries = sortedDates.mapIndexed { index, date ->
            Entry(index.toFloat(), expenseByDay[date]!!)
        }
        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("Нет данных о расходах")
            return
        }
        val dataSet = LineDataSet(entries, "Расходы по дням")
        dataSet.color = Color.parseColor("#F44336")
        dataSet.setCircleColor(Color.parseColor("#F44336"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(true)
        dataSet.valueTextSize = 10f
        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.xAxis.valueFormatter = object : com.github.mikephil.charting.components.IXAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: com.github.mikephil.charting.components.XAxis?): String {
                val index = value.toInt()
                return if (index in sortedDates.indices) sortedDates[index] else ""
            }
        }
        lineChart.xAxis.granularity = 1f
        lineChart.invalidate()
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr)?.time ?: 0
        } catch (e: Exception) { 0 }
    }
}
