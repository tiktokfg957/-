package com.example.budgettracker

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etBudget: EditText
    private lateinit var btnSaveBudget: Button
    private lateinit var llCategoryLimits: LinearLayout
    private lateinit var btnAddCategory: Button

    private val categoryLimits = mutableMapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("budget_data", MODE_PRIVATE)

        etBudget = findViewById(R.id.etBudget)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        llCategoryLimits = findViewById(R.id.llCategoryLimits)
        btnAddCategory = findViewById(R.id.btnAddCategory)

        loadBudget()
        loadCategoryLimits()
        renderCategoryLimits()

        btnSaveBudget.setOnClickListener {
            val budget = etBudget.text.toString().toDoubleOrNull()
            if (budget != null && budget > 0) {
                prefs.edit().putFloat("budget", budget.toFloat()).apply()
                Toast.makeText(this, "Бюджет сохранён", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Введите корректную сумму", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun loadBudget() {
        val budget = prefs.getFloat("budget", 30000f)
        etBudget.setText(budget.toInt().toString())
    }

    private fun loadCategoryLimits() {
        val json = prefs.getString("category_limits", "{}")
        val obj = JSONObject(json)
        categoryLimits.clear()
        obj.keys().forEach { key ->
            categoryLimits[key] = obj.getDouble(key)
        }
    }

    private fun saveCategoryLimits() {
        val obj = JSONObject()
        categoryLimits.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("category_limits", obj.toString()).apply()
    }

    private fun renderCategoryLimits() {
        llCategoryLimits.removeAllViews()
        categoryLimits.forEach { (category, limit) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
            }
            val tvCategory = TextView(this).apply {
                text = "$category: "
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val etLimit = EditText(this).apply {
                setText(limit.toInt().toString())
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val btnSave = Button(this).apply {
                text = "Сохранить"
                setOnClickListener {
                    val newLimit = etLimit.text.toString().toDoubleOrNull()
                    if (newLimit != null && newLimit > 0) {
                        categoryLimits[category] = newLimit
                        saveCategoryLimits()
                        Toast.makeText(this@SettingsActivity, "Лимит обновлён", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Некорректная сумма", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            row.addView(tvCategory)
            row.addView(etLimit)
            row.addView(btnSave)
            llCategoryLimits.addView(row)
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etLimit = dialogView.findViewById<EditText>(R.id.etCategoryLimit)
        AlertDialog.Builder(this)
            .setTitle("Новый лимит категории")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val category = etCategory.text.toString().trim()
                val limit = etLimit.text.toString().toDoubleOrNull()
                if (category.isNotEmpty() && limit != null && limit > 0) {
                    categoryLimits[category] = limit
                    saveCategoryLimits()
                    renderCategoryLimits()
                    Toast.makeText(this, "Лимит добавлен", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
