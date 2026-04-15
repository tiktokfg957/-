package com.example.budgettracker.ui.add

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.R
import java.text.SimpleDateFormat
import java.util.*

class AddActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etShop: EditText
    private lateinit var etCategory: EditText
    private lateinit var etDate: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private val viewModel: AddViewModel by viewModels {
        AddViewModel.AddViewModelFactory((application as com.example.budgettracker.BudgetApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add)

        etAmount = findViewById(R.id.etAmount)
        etShop = findViewById(R.id.etShop)
        etCategory = findViewById(R.id.etCategory)
        etDate = findViewById(R.id.etDate)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        setupDatePicker()

        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString()
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Некорректная сумма", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val shop = etShop.text.toString().trim()
            if (shop.isEmpty()) {
                Toast.makeText(this, "Укажите магазин", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val category = etCategory.text.toString().trim()
            if (category.isEmpty()) {
                Toast.makeText(this, "Укажите категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dateStr = etDate.text.toString()
            val date = try {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr) ?: Date()
            } catch (e: Exception) { Date() }
            val timestamp = date.time

            viewModel.addTransaction(amount, shop, category, timestamp, null)
            Toast.makeText(this, "Добавлено", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        etDate.setText(currentDate)
        etDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year)
                etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}
