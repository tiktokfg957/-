package com.example.budgettracker

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvAdvice: TextView

    private val transactions = mutableListOf<Transaction>()
    private var totalExpense = 0.0
    private var totalIncome = 0.0
    private var budget = 0.0

    // Для фото чека
    private var currentPhotoPath: String? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Регистрация для камеры
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoPath != null) {
            val photoFile = File(currentPhotoPath)
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            recognizeReceipt(uri)
        } else {
            Toast.makeText(requireContext(), "Не удалось сделать фото", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireActivity().getSharedPreferences("budget_data", Context.MODE_PRIVATE)

        recyclerView = view.findViewById(R.id.rvTransactions)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvAdvice = view.findViewById(R.id.tvAdvice)
        val btnAdd = view.findViewById<FloatingActionButton>(R.id.btnAdd)

        adapter = TransactionAdapter(transactions) { position ->
            removeTransaction(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadData()

        val budgetSet = prefs.getBoolean("budget_set", false)
        if (!budgetSet) {
            showBudgetDialog()
        } else {
            budget = prefs.getFloat("budget", 30000f).toDouble()
            updateUI()
        }

        btnAdd.setOnClickListener {
            showAddDialog()
        }
    }

    fun refreshData() {
        loadData()
        updateUI()
        adapter.notifyDataSetChanged()
    }

    private fun showBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget, null)
        val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveBudget)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Введите ваш бюджет на месяц")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnSave.setOnClickListener {
            val budgetStr = etBudget.text.toString().trim()
            if (budgetStr.isNotEmpty()) {
                val newBudget = budgetStr.toDoubleOrNull()
                if (newBudget != null && newBudget > 0) {
                    budget = newBudget
                    prefs.edit().putFloat("budget", budget.toFloat()).apply()
                    prefs.edit().putBoolean("budget_set", true).apply()
                    updateUI()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Введите корректную сумму", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Поле не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val etShop = dialogView.findViewById<EditText>(R.id.etShop)
        val etCategory = dialogView.findViewById<EditText>(R.id.etCategory)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbExpense)
        val btnPhoto = dialogView.findViewById<Button>(R.id.btnPhoto)

        etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        // Кнопка фото чека – только для расходов
        btnPhoto.visibility = if (rbExpense.isChecked) View.VISIBLE else View.GONE

        rbExpense.setOnCheckedChangeListener { _, isChecked ->
            btnPhoto.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnPhoto.setOnClickListener {
            // Проверяем разрешение на камеру
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
            } else {
                dispatchTakePictureIntent()
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Новая операция")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull()
                val shop = etShop.text.toString().trim()
                val category = etCategory.text.toString().trim()
                val date = etDate.text.toString()
                val type = if (rbExpense.isChecked) "expense" else "income"

                if (amount != null && amount > 0 && shop.isNotEmpty() && category.isNotEmpty()) {
                    addTransaction(amount, shop, category, date, type)
                } else {
                    Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "Ошибка создания файла: ${ex.message}", Toast.LENGTH_SHORT).show()
            return
        }
        photoFile?.let {
            currentPhotoPath = it.absolutePath
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            takePictureLauncher.launch(uri)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun recognizeReceipt(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text
                    val (amount, shop) = parseReceiptText(recognizedText)
                    if (amount != null) {
                        Toast.makeText(requireContext(), "Распознано: $amount ₽\nМагазин: ${shop ?: "не определён"}", Toast.LENGTH_LONG).show()
                        // Здесь можно было бы автоматически заполнить поля, но для простоты покажем тост
                    } else {
                        Toast.makeText(requireContext(), "Не удалось распознать сумму", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка распознавания", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseReceiptText(text: String): Pair<Double?, String?> {
        val amountPattern = Regex("""(\d+[.,]\d{2})""")
        val amountMatch = amountPattern.find(text)
        val amount = amountMatch?.value?.replace(',', '.')?.toDoubleOrNull()

        val lines = text.lines()
        val shop = lines.firstOrNull { line ->
            line.length in 3..30 && !line.any { it.isDigit() }
        }?.trim()

        return Pair(amount, shop)
    }

    private fun addTransaction(amount: Double, shop: String, category: String, date: String, type: String) {
        val transaction = Transaction(amount, shop, category, date, type)
        transactions.add(0, transaction)
        if (type == "expense") {
            totalExpense += amount
        } else {
            totalIncome += amount
        }
        saveData()
        adapter.notifyItemInserted(0)
        updateUI()
    }

    private fun removeTransaction(position: Int) {
        val transaction = transactions[position]
        if (transaction.type == "expense") {
            totalExpense -= transaction.amount
        } else {
            totalIncome -= transaction.amount
        }
        transactions.removeAt(position)
        saveData()
        adapter.notifyItemRemoved(position)
        updateUI()
    }

    private fun updateUI() {
        val balance = budget - totalExpense + totalIncome
        tvBalance.text = String.format("%.2f ₽", balance)
        tvTotalExpense.text = String.format("Расходы: %.2f ₽ | Доходы: %.2f ₽", totalExpense, totalIncome)

        val advice = when {
            balance < 0 -> "⚠️ Вы превысили бюджет на ${String.format("%.2f", -balance)} ₽"
            balance < 5000 -> "❗ Осталось меньше 5000 ₽. Будьте внимательны."
            else -> "✅ Вы укладываетесь в бюджет. Отлично!"
        }
        tvAdvice.text = advice
    }

    private fun saveData() {
        val jsonArray = JSONArray()
        transactions.forEach {
            val obj = JSONObject()
            obj.put("amount", it.amount)
            obj.put("shop", it.shop)
            obj.put("category", it.category)
            obj.put("date", it.date)
            obj.put("type", it.type)
            jsonArray.put(obj)
        }
        prefs.edit().putString("transactions", jsonArray.toString()).apply()
        prefs.edit().putFloat("totalExpense", totalExpense.toFloat()).apply()
        prefs.edit().putFloat("totalIncome", totalIncome.toFloat()).apply()
    }

    private fun loadData() {
        val jsonStr = prefs.getString("transactions", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonStr)
        transactions.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val transaction = Transaction(
                obj.getDouble("amount"),
                obj.getString("shop"),
                obj.getString("category"),
                obj.getString("date"),
                obj.getString("type")
            )
            transactions.add(transaction)
        }
        totalExpense = prefs.getFloat("totalExpense", 0f).toDouble()
        totalIncome = prefs.getFloat("totalIncome", 0f).toDouble()
    }

    data class Transaction(
        val amount: Double,
        val shop: String,
        val category: String,
        val date: String,
        val type: String
    )

    inner class TransactionAdapter(
        private val items: List<Transaction>,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvShop.text = item.shop
            holder.tvCategory.text = item.category
            holder.tvAmount.text = String.format("%.2f ₽", item.amount)
            holder.tvDate.text = item.date
            holder.btnDelete.setOnClickListener { onDelete(position) }
            if (item.type == "income") {
                holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                holder.tvAmount.setTextColor(android.graphics.Color.parseColor("#F44336"))
            }
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvShop: TextView = itemView.findViewById(R.id.tvShop)
            val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
            val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(requireContext(), "Нет доступа к камере", Toast.LENGTH_SHORT).show()
        }
    }
}
