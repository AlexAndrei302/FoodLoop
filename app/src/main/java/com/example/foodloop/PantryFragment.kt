
package com.example.foodloop

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.example.foodloop.database.Food
import com.example.foodloop.database.FoodDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Intent
import com.example.foodloop.scanner.ScanActivity
import android.app.Activity
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.UUID
import com.example.foodloop.repository.AuthRepository
import com.example.foodloop.repository.FoodCloudRepository
import com.example.foodloop.repository.CloudIdProvider
import com.example.foodloop.notifications.NotificationScheduler
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.widget.doAfterTextChanged
import java.util.Calendar
import androidx.core.content.ContextCompat


class PantryFragment : Fragment(R.layout.fragment_pantry) {

    private lateinit var foodList: LinearLayout


    private lateinit var searchInput: EditText

    private lateinit var categorySpinner: Spinner
    private lateinit var statusSpinner: Spinner
    private lateinit var sortSpinner: Spinner

    private lateinit var totalFoodsText: TextView
    private lateinit var expiringFoodsText: TextView
    private lateinit var expiredFoodsText: TextView
    private lateinit var resultsText: TextView

    private var allFoods: List<Food> = emptyList()

    private var categories: List<String> = listOf("Alle Kategorien")

    private val statuses = listOf(
        "Alle Produkte",
        "Bald ablaufend",
        "Abgelaufen",
        "Haltbar"
    )

    private val sortingOptions = listOf(
        "Ablaufdatum (aufsteigend)",
        "Ablaufdatum (absteigend)",
        "Name (A-Z)",
        "Name (Z-A)",
        "Kategorie"
    )

    private val authRepository = AuthRepository()

    private val cloudRepository = FoodCloudRepository()

    private lateinit var currentUserId: String

    private val scanLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val photoPath =
                    result.data?.getStringExtra("photoPath")

                val expirationDate =
                    result.data?.getStringExtra("expirationDate")

                if (photoPath != null) {

                    showProductSelectionDialog(
                        photoPath,
                        expirationDate
                    )
                }
            }
        }


    private fun showProductSelectionDialog(
        photoPath: String,
        expirationDate: String? = null
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            val foods = foodDao.getAllFoods(currentUserId).first()

            val options = foods.map { food ->

                "${food.name} - " +
                        "${food.quantity} ${food.unit} " +
                        "(#${food.id})"

            } + "+ Neues Lebensmittel"

            AlertDialog.Builder(requireContext())

                .setTitle("Produkt auswählen")

                .setItems(options.toTypedArray()) { _, position ->

                    if (position == foods.size) {

                        showFoodDialog(
                            photoPath = photoPath,
                            detectedDate = expirationDate
                        )

                    } else {

                        val selectedFood = foods[position]

                        if (expirationDate != null) {

                            showDateConfirmationDialog(
                                selectedFood,
                                photoPath,
                                expirationDate
                            )

                        } else {

                            savePhotoToProduct(
                                selectedFood,
                                photoPath
                            )
                        }
                    }
                }

                .setNegativeButton("Abbrechen") { _, _ ->

                    File(photoPath).delete()
                }

                .setOnCancelListener {

                    File(photoPath).delete()
                }

                .show()
        }
    }


    private val foodDao by lazy {
        FoodDatabase.getDatabase(requireContext()).foodDao()
    }




    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        val uid = authRepository.getCurrentUserId()

        if (uid == null) {
            requireActivity().finish()
            return
        }

        currentUserId = uid

        foodList = view.findViewById(R.id.food_list)

        searchInput = view.findViewById(R.id.et_search_food)

        categorySpinner = view.findViewById(R.id.spinner_category)

        statusSpinner = view.findViewById(R.id.spinner_status)

        sortSpinner = view.findViewById(R.id.spinner_sort)

        totalFoodsText = view.findViewById(R.id.txt_total_foods)

        expiringFoodsText = view.findViewById(R.id.txt_expiring_foods)

        expiredFoodsText = view.findViewById(R.id.txt_expired_foods)

        resultsText = view.findViewById(R.id.txt_food_results)

        allFoods = emptyList()

        categories = listOf("Alle Kategorien")

        setupFilters()


        val filterPanel: LinearLayout =
            view.findViewById(R.id.filter_panel)

        val toggleFilters: View =
            view.findViewById(R.id.btn_toggle_filters)

        val resetFilters: Button =
            view.findViewById(R.id.btn_reset_filters)

        toggleFilters.setOnClickListener {

            if (filterPanel.visibility == View.VISIBLE) {

                filterPanel.visibility = View.GONE

            } else {

                filterPanel.visibility = View.VISIBLE
            }
        }

        resetFilters.setOnClickListener {

            searchInput.setText("")

            categorySpinner.setSelection(0)

            statusSpinner.setSelection(0)

            sortSpinner.setSelection(0)

            applyFilters()

            filterPanel.visibility = View.GONE
        }


        val addButton: Button =
            view.findViewById(R.id.btn_add_food)

        val scanButton: Button =
            view.findViewById(R.id.btn_scan_food)

        addButton.setOnClickListener {

            showFoodDialog()
        }

        scanButton.setOnClickListener {

            val intent = Intent(
                requireContext(),
                ScanActivity::class.java
            )

            scanLauncher.launch(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                foodDao.getAllFoods(currentUserId).collect { foods ->

                    allFoods = foods

                    updateCategories()

                    updateStatistics()

                    applyFilters()
                }
            }
        }
    }


    private fun setupFilters() {

        setSpinnerOptions(categorySpinner, categories)

        setSpinnerOptions(statusSpinner, statuses)

        setSpinnerOptions(sortSpinner, sortingOptions)

        searchInput.doAfterTextChanged {

            applyFilters()
        }

        val selectionListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    applyFilters()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        categorySpinner.onItemSelectedListener =
            selectionListener

        statusSpinner.onItemSelectedListener =
            selectionListener

        sortSpinner.onItemSelectedListener =
            selectionListener
    }

    private fun setSpinnerOptions(
        spinner: Spinner,
        options: List<String>
    ) {

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter
    }

    private fun updateCategories() {

        val previousCategory =
            categorySpinner.selectedItem?.toString()
                ?: "Alle Kategorien"

        val newCategories = listOf("Alle Kategorien") +
                allFoods.map {
                    it.category.trim()
                }.filter {
                    it.isNotEmpty()
                }.distinct().sorted()

        if (categories == newCategories) {
            return
        }

        categories = newCategories

        setSpinnerOptions(
            categorySpinner,
            categories
        )

        val selectedIndex =
            categories.indexOf(previousCategory)

        categorySpinner.setSelection(
            if (selectedIndex >= 0) selectedIndex else 0
        )
    }

    private fun getDateLimits(): Pair<String, String> {

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        val calendar = Calendar.getInstance()

        val today = formatter.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, 3)

        val limit = formatter.format(calendar.time)

        return Pair(today, limit)
    }

    private fun updateStatistics() {

        val (today, limit) = getDateLimits()

        val expired = allFoods.count {

            it.expirationDate < today
        }

        val expiring = allFoods.count {

            it.expirationDate >= today &&
                    it.expirationDate <= limit
        }

        totalFoodsText.text =
            allFoods.size.toString()

        expiringFoodsText.text =
            expiring.toString()

        expiredFoodsText.text =
            expired.toString()
    }

    private fun applyFilters() {

        val searchText = searchInput.text
            .toString()
            .trim()
            .lowercase(Locale.GERMAN)

        val selectedCategory =
            categorySpinner.selectedItem?.toString()
                ?: "Alle Kategorien"

        val selectedStatus =
            statusSpinner.selectedItem?.toString()
                ?: "Alle Produkte"

        val selectedSort =
            sortSpinner.selectedItemPosition

        val (today, limit) = getDateLimits()

        var filteredFoods = allFoods.filter { food ->

            val matchesSearch =
                food.name.lowercase(Locale.GERMAN)
                    .contains(searchText) ||

                        food.category.lowercase(Locale.GERMAN)
                            .contains(searchText) ||

                        food.location.lowercase(Locale.GERMAN)
                            .contains(searchText)

            val matchesCategory =
                selectedCategory == "Alle Kategorien" ||
                        food.category.trim() == selectedCategory

            val matchesStatus = when (selectedStatus) {

                "Bald ablaufend" ->

                    food.expirationDate >= today &&
                            food.expirationDate <= limit

                "Abgelaufen" ->

                    food.expirationDate < today

                "Haltbar" ->

                    food.expirationDate > limit

                else -> true
            }

            matchesSearch &&
                    matchesCategory &&
                    matchesStatus
        }

        filteredFoods = when (selectedSort) {

            1 -> filteredFoods.sortedByDescending {
                it.expirationDate
            }

            2 -> filteredFoods.sortedBy {
                it.name.lowercase(Locale.GERMAN)
            }

            3 -> filteredFoods.sortedByDescending {
                it.name.lowercase(Locale.GERMAN)
            }

            4 -> filteredFoods.sortedBy {
                it.category.lowercase(Locale.GERMAN)
            }

            else -> filteredFoods.sortedBy {
                it.expirationDate
            }
        }

        resultsText.text =
            "${filteredFoods.size} von " +
                    "${allFoods.size} Lebensmittel gefunden"

        displayFoods(filteredFoods)
    }





    private fun displayFoods(foods: List<Food>) {

        foodList.removeAllViews()

        val context = requireContext()

        if (foods.isEmpty()) {

            val emptyText = TextView(context)

            emptyText.text = if (allFoods.isEmpty()) {
                "Noch keine Lebensmittel vorhanden"
            } else {
                "Keine passenden Lebensmittel gefunden"
            }

            emptyText.textSize = 15f

            emptyText.gravity = android.view.Gravity.CENTER

            emptyText.setTextColor(
                themeColor(R.color.foodloop_text_secondary)
            )

            emptyText.setPadding(
                dp(12),
                dp(24),
                dp(12),
                dp(24)
            )

            foodList.addView(emptyText)

            return
        }

        val (today, limit) = getDateLimits()

        for (food in foods) {

            val item = LinearLayout(context)

            item.orientation = LinearLayout.HORIZONTAL

            item.gravity = android.view.Gravity.CENTER_VERTICAL

            item.setPadding(
                dp(10),
                dp(10),
                dp(8),
                dp(10)
            )

            val background = GradientDrawable()

            background.setColor(
                themeColor(R.color.foodloop_surface)
            )
            background.cornerRadius = dp(12).toFloat()

            background.setStroke(
                dp(1),
                themeColor(R.color.foodloop_border)
            )

            item.background = background

            val itemParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            itemParams.bottomMargin = dp(8)

            item.layoutParams = itemParams

            // PRODUKTFOTO

            val photoPath = food.photoPath

            if (photoPath != null &&
                File(photoPath).exists()
            ) {

                val imageView = ImageView(context)

                val imageParams = LinearLayout.LayoutParams(
                    dp(58),
                    dp(58)
                )

                imageParams.marginEnd = dp(12)

                imageView.layoutParams = imageParams

                imageView.scaleType =
                    ImageView.ScaleType.CENTER_CROP

                imageView.setImageURI(
                    Uri.fromFile(File(photoPath))
                )

                imageView.contentDescription = food.name

                imageView.setOnClickListener {

                    showFullPhoto(photoPath)
                }

                item.addView(imageView)

            } else {

                val placeholder = ImageView(context)

                val placeholderParams =
                    LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                    )

                placeholderParams.marginEnd = dp(12)

                placeholder.layoutParams =
                    placeholderParams

                placeholder.setImageResource(
                    R.drawable.ic_pantry
                )

                placeholder.setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )

                val placeholderBackground =
                    GradientDrawable()

                placeholderBackground.setColor(
                    themeColor(R.color.foodloop_primary_light)
                )

                placeholderBackground.cornerRadius =
                    dp(10).toFloat()

                placeholder.background =
                    placeholderBackground

                item.addView(placeholder)
            }

            // PRODUKTINFORMATIONEN

            val information = LinearLayout(context)

            information.orientation =
                LinearLayout.VERTICAL

            information.layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )

            val nameText = TextView(context)

            nameText.text = food.name

            nameText.textSize = 17f

            nameText.setTypeface(
                null,
                Typeface.BOLD
            )

            nameText.setTextColor(
                themeColor(R.color.foodloop_text_primary)
            )

            nameText.maxLines = 1

            nameText.ellipsize =
                android.text.TextUtils.TruncateAt.END

            val quantityText = TextView(context)

            quantityText.text =
                "${food.quantity} ${food.unit} · ${food.location}"

            quantityText.textSize = 13f

            quantityText.setTextColor(
                themeColor(R.color.foodloop_text_secondary)
            )

            quantityText.maxLines = 1

            quantityText.ellipsize =
                android.text.TextUtils.TruncateAt.END

            val statusText = TextView(context)

            statusText.textSize = 12f

            statusText.setTypeface(
                null,
                Typeface.BOLD
            )

            when {

                food.expirationDate < today -> {

                    statusText.text =
                        "Abgelaufen · ${food.expirationDate}"

                    statusText.setTextColor(
                        themeColor(R.color.foodloop_error)
                    )
                }

                food.expirationDate == today -> {

                    statusText.text = "Läuft heute ab!"

                    statusText.setTextColor(
                        themeColor(R.color.foodloop_warning)
                    )
                }

                food.expirationDate <= limit -> {

                    statusText.text =
                        "Bald · ${food.expirationDate}"

                    statusText.setTextColor(
                        themeColor(R.color.foodloop_warning)
                    )
                }

                else -> {

                    statusText.text =
                        "Haltbar · ${food.expirationDate}"

                    statusText.setTextColor(
                        themeColor(R.color.foodloop_primary)
                    )
                }
            }

            information.addView(nameText)

            information.addView(quantityText)

            information.addView(statusText)

            item.addView(information)

            // DETAILBUTTON

            val detailsButton = TextView(context)

            detailsButton.text = "⋮"

            detailsButton.textSize = 26f

            detailsButton.gravity =
                android.view.Gravity.CENTER

            detailsButton.setTextColor(
                themeColor(R.color.foodloop_text_secondary)
            )

            detailsButton.layoutParams =
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48)
                )

            detailsButton.contentDescription =
                "Produktdetails: ${food.name}"

            detailsButton.setOnClickListener {

                showFoodDetails(food)
            }

            item.addView(detailsButton)

            item.setOnClickListener {

                showFoodDetails(food)
            }

            foodList.addView(item)
        }
    }



    private fun showFoodDetails(food: Food) {

        val context = requireContext()

        val options = mutableListOf(
            "Bearbeiten"
        )

        val photoPath = food.photoPath

        if (photoPath != null &&
            File(photoPath).exists()
        ) {

            options.add("Foto anzeigen")
        }

        if (photoPath != null) {

            options.add("Foto entfernen")
        }

        options.add("Löschen")

        val information =
            "Menge: ${food.quantity} ${food.unit}\n\n" +
                    "Kategorie: ${food.category}\n\n" +
                    "Lagerort: ${food.location}\n\n" +
                    "Ablaufdatum: ${food.expirationDate}"

        AlertDialog.Builder(context)

            .setTitle(food.name)

            .setMessage(information)

            .setPositiveButton("Aktionen") { _, _ ->

                AlertDialog.Builder(context)

                    .setTitle("Produktaktionen")

                    .setItems(
                        options.toTypedArray()
                    ) { _, position ->

                        when (options[position]) {

                            "Bearbeiten" -> {

                                showFoodDialog(food)
                            }

                            "Foto anzeigen" -> {

                                photoPath?.let {

                                    showFullPhoto(it)
                                }
                            }

                            "Foto entfernen" -> {

                                removeFoodPhoto(food)
                            }

                            "Löschen" -> {

                                deleteFood(food)
                            }
                        }
                    }

                    .setNegativeButton(
                        "Abbrechen",
                        null
                    )

                    .show()
            }

            .setNegativeButton(
                "Schließen",
                null
            )

            .show()
    }



    private fun showFoodDialog(
        food: Food? = null,
        photoPath: String? = null,
        detectedDate: String? = null) {

        val context = requireContext()

        val form = LinearLayout(context)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(32, 16, 32, 16)

        val nameInput = EditText(context)
        nameInput.hint = "Name"
        nameInput.setText(food?.name ?: "")

        val quantityInput = EditText(context)
        quantityInput.hint = "Menge"
        quantityInput.inputType =
            InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        quantityInput.setText(
            food?.quantity?.toString() ?: ""
        )

        val unitInput = EditText(context)
        unitInput.hint = "Einheit (z.B. kg, Liter)"
        unitInput.setText(food?.unit ?: "")

        val categoryInput = EditText(context)
        categoryInput.hint = "Kategorie"
        categoryInput.setText(food?.category ?: "")

        val locationInput = EditText(context)
        locationInput.hint = "Lagerort"
        locationInput.setText(food?.location ?: "")



        val dateInput = EditText(context)

        dateInput.hint = "Ablaufdatum (YYYY-MM-DD)"

        dateInput.setText(detectedDate ?: food?.expirationDate ?: "")


        val displayedPhoto = photoPath ?: food?.photoPath

        if (displayedPhoto != null &&
            File(displayedPhoto).exists()
        ) {

            val imageView = ImageView(context)

            imageView.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (200 * resources.displayMetrics.density).toInt()
                )

            imageView.scaleType = ImageView.ScaleType.FIT_CENTER

            imageView.setImageURI(
                Uri.fromFile(File(displayedPhoto))
            )

            form.addView(imageView)
        }

        form.addView(nameInput)
        form.addView(quantityInput)
        form.addView(unitInput)
        form.addView(categoryInput)
        form.addView(locationInput)
        form.addView(dateInput)

        val scrollView = ScrollView(context)
        scrollView.addView(form)

        val dialog = AlertDialog.Builder(context)
            .setTitle(
                if (food == null)
                    "Lebensmittel hinzufügen"
                else
                    "Lebensmittel bearbeiten"
            )
            .setView(scrollView)
            .setPositiveButton("Speichern", null)
            .setNegativeButton("Abbrechen") { _, _ ->
                photoPath?.let {
                    File(it).delete()
                }
            }
            .setOnCancelListener {
                photoPath?.let {
                    File(it).delete()
                }
            }            .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name = nameInput.text.toString().trim()
                val quantity = quantityInput.text.toString()
                    .replace(',', '.')
                    .toDoubleOrNull()
                val unit = unitInput.text.toString().trim()
                val category = categoryInput.text.toString().trim()
                val location = locationInput.text.toString().trim()
                val date = dateInput.text.toString().trim()

                val dateFormat =
                    SimpleDateFormat("yyyy-MM-dd", Locale.US)
                dateFormat.isLenient = false

                val validDate = try {
                    date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                            dateFormat.parse(date) != null
                } catch (e: Exception) {
                    false
                }

                if (name.isEmpty()) {
                    nameInput.error = "Name eingeben"
                } else if (quantity == null || quantity <= 0) {
                    quantityInput.error = "Ungültige Menge"
                } else if (unit.isEmpty()) {
                    unitInput.error = "Einheit eingeben"
                } else if (!validDate) {
                    dateInput.error = "Datum: YYYY-MM-DD"
                } else {


                    val directory = context.filesDir

                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = false

                    viewLifecycleOwner.lifecycleScope.launch {

                        try {

                            withContext(Dispatchers.IO) {

                                val savedPhotoPath =
                                    if (photoPath != null) {

                                        savePhotoPermanently(
                                            photoPath,
                                            directory
                                        )

                                    } else {
                                        food?.photoPath
                                    }

                                val newFood = Food(
                                    id = food?.id ?: 0,
                                    name = name,
                                    quantity = quantity,
                                    unit = unit,
                                    category = category,
                                    location = location,
                                    expirationDate = date,
                                    photoPath = savedPhotoPath,
                                    userId = currentUserId
                                )

                                try {


                                    val savedFood = if (food == null) {

                                        val generatedId = foodDao.insert(newFood)

                                        newFood.copy(
                                            id = generatedId.toInt()
                                        )

                                    } else {

                                        foodDao.update(newFood)

                                        newFood
                                    }

                                    withContext(Dispatchers.Main) {
                                        saveFoodToCloud(savedFood)
                                    }


                                } catch (e: Exception) {

                                    if (photoPath != null) {
                                        savedPhotoPath?.let {
                                            File(it).delete()
                                        }
                                    }

                                    throw e
                                }

                                if (photoPath != null) {
                                    File(photoPath).delete()

                                    food?.photoPath?.let {
                                        File(it).delete()
                                    }
                                }
                            }

                            NotificationScheduler.testNow(context)
                            dialog.dismiss()

                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {

                            dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                            ).isEnabled = true

                            Toast.makeText(
                                context,
                                "Fehler beim Speichern",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            }
        }

        dialog.show()
    }

    private fun deleteFood(food: Food) {

        AlertDialog.Builder(requireContext())
            .setTitle("Lebensmittel löschen")
            .setMessage("${food.name} wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->

                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val currentFood = foodDao.getFoodById(
                            food.id,
                            currentUserId
                        )

                        if (currentFood != null) {

                            foodDao.delete(currentFood)
                            withContext(Dispatchers.Main) {
                                deleteFoodFromCloud(currentFood)
                            }

                            currentFood.photoPath?.let {
                                File(it).delete()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }


    private fun showDateConfirmationDialog(
        food: Food,
        photoPath: String,
        detectedDate: String
    ) {

        AlertDialog.Builder(requireContext())

            .setTitle("Ablaufdatum aktualisieren?")

            .setMessage(
                "Produkt: ${food.name}\n\n" +
                        "Aktuelles Datum: ${food.expirationDate}\n" +
                        "Erkanntes Datum: $detectedDate\n\n" +
                        "Möchtest du das Ablaufdatum aktualisieren?"
            )

            .setPositiveButton("Datum überprüfen") { _, _ ->

                showFoodDialog(
                    food = food,
                    photoPath = photoPath,
                    detectedDate = detectedDate
                )
            }

            .setNegativeButton("Nur Foto speichern") { _, _ ->

                savePhotoToProduct(
                    food,
                    photoPath
                )
            }

            .setNeutralButton("Abbrechen") { _, _ ->

                File(photoPath).delete()
            }

            .setOnCancelListener {

                File(photoPath).delete()
            }

            .show()
    }



    private fun savePhotoPermanently(
        photoPath: String,
        directory: File
    ): String {

        val photoDirectory = File(
            directory,
            "food_photos"
        )

        photoDirectory.mkdirs()

        val destination = File(
            photoDirectory,
            "food_${UUID.randomUUID()}.jpg"
        )

        try {
            File(photoPath).copyTo(destination)
        } catch (e: Exception) {
            destination.delete()
            throw e
        }

        return destination.absolutePath
    }


    private fun savePhotoToProduct(
        food: Food,
        photoPath: String
    ) {

        val directory = requireContext().filesDir

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                withContext(Dispatchers.IO) {

                    val currentFood =
                        foodDao.getFoodById(
                            food.id,
                            currentUserId
                        )
                            ?: error("Produkt nicht gefunden")

                    val savedPath = savePhotoPermanently(
                        photoPath,
                        directory
                    )

                    try {

                        val updatedFood = currentFood.copy(
                            photoPath = savedPath
                        )

                        foodDao.update(updatedFood)

                    } catch (e: Exception) {

                        File(savedPath).delete()
                        throw e
                    }

                    currentFood.photoPath?.let {
                        File(it).delete()
                    }

                    File(photoPath).delete()
                }

                Toast.makeText(
                    requireContext(),
                    "Foto erfolgreich gespeichert",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "Fehler beim Speichern",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun showFullPhoto(photoPath: String) {

        val imageView = ImageView(requireContext())

        imageView.adjustViewBounds = true

        imageView.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imageView.setImageURI(
            Uri.fromFile(File(photoPath))
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Produktfotografie")
            .setView(imageView)
            .setPositiveButton("Schließen", null)
            .show()
    }


    private fun removeFoodPhoto(food: Food) {

        AlertDialog.Builder(requireContext())
            .setTitle("Foto entfernen")
            .setMessage("Möchtest du das Foto entfernen?")
            .setPositiveButton("Entfernen") { _, _ ->

                viewLifecycleOwner.lifecycleScope.launch {

                    withContext(Dispatchers.IO) {

                        val currentFood =
                            foodDao.getFoodById(
                                food.id,
                                currentUserId
                            )

                        if (currentFood != null) {

                            foodDao.update(
                                currentFood.copy(
                                    photoPath = null
                                )
                            )

                            currentFood.photoPath?.let {
                                File(it).delete()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }


    private fun saveFoodToCloud(food: Food) {

        val context = requireContext().applicationContext

        if (authRepository.getCurrentUserId() != food.userId) {
            return
        }

        val cloudId = CloudIdProvider.getId(
            context,
            food.id
        )

        cloudRepository.saveFood(
            cloudId,
            food
        ) { success, message ->

            if (!success) {
                android.util.Log.e(
                    "FoodLoopCloud",
                    message
                )
            }
        }
    }

    private fun deleteFoodFromCloud(food: Food) {

        val context = requireContext().applicationContext

        if (authRepository.getCurrentUserId() != food.userId) {
            return
        }

        val cloudId = CloudIdProvider.getId(
            context,
            food.id
        )

        cloudRepository.deleteFood(
            cloudId
        ) { success, message ->

            if (!success) {
                android.util.Log.e(
                    "FoodLoopCloud",
                    message
                )
            }
        }
    }

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    private fun themeColor(colorId: Int): Int {
        return ContextCompat.getColor(
            requireContext(),
            colorId
        )
    }
}
