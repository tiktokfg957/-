package com.example.budgettracker.ui.add

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgettracker.databinding.ActivityAddBinding
import java.text.SimpleDateFormat
import java.util.*

class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private val viewModel: AddViewModel by viewModels {
        AddViewModel.AddViewModelFactory((application as com.example.budgettracker.BudgetApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.add)

        setupDatePicker()

        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Некорректная сумма", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val shop = binding.etShop.text.toString().trim()
            if (shop.isEmpty()) {
                Toast.makeText(this, "Укажите магазин", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val category = binding.etCategory.text.toString().trim()
            if (category.isEmpty()) {
                Toast.makeText(this, "Укажите категорию", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dateStr = binding.etDate.text.toString()
            val date = try {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateStr) ?: Date()
            } catch (e: Exception) { Date() }
            val timestamp = date.time

            viewModel.addTransaction(amount, shop, category, timestamp, null)
            Toast.makeText(this, "Добавлено", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        binding.etDate.setText(currentDate)
        binding.etDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year)
                binding.etDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}
