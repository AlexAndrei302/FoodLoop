
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

class PantryFragment : Fragment(R.layout.fragment_pantry) {

    private lateinit var foodList: LinearLayout

    private val foodDao by lazy {
        FoodDatabase.getDatabase(requireContext()).foodDao()
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        foodList = view.findViewById(R.id.food_list)

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

            startActivity(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                foodDao.getAllFoods().collect { foods ->
                    displayFoods(foods)
                }
            }
        }
    }


    private fun displayFoods(foods: List<Food>) {

        foodList.removeAllViews()

        if (foods.isEmpty()) {
            val emptyText = TextView(requireContext())
            emptyText.text = "Noch keine Lebensmittel vorhanden"
            emptyText.textSize = 16f
            emptyText.setPadding(12, 20, 12, 20)
            foodList.addView(emptyText)
            return
        }

        for (food in foods) {

            val item = LinearLayout(requireContext())
            item.orientation = LinearLayout.VERTICAL
            item.setPadding(16, 16, 16, 16)

            val nameText = TextView(requireContext())
            nameText.text = food.name
            nameText.textSize = 20f

            val infoText = TextView(requireContext())
            infoText.text =
                "${food.quantity} ${food.unit}\n" +
                        "Kategorie: ${food.category}\n" +
                        "Lagerort: ${food.location}\n" +
                        "Ablaufdatum: ${food.expirationDate}"

            val editButton = Button(requireContext())
            editButton.text = "Bearbeiten"

            editButton.setOnClickListener {
                showFoodDialog(food)
            }

            val deleteButton = Button(requireContext())
            deleteButton.text = "Löschen"

            deleteButton.setOnClickListener {
                deleteFood(food)
            }

            item.addView(nameText)
            item.addView(infoText)
            item.addView(editButton)
            item.addView(deleteButton)

            foodList.addView(item)
        }
    }

    private fun showFoodDialog(food: Food? = null) {

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
        dateInput.setText(food?.expirationDate ?: "")

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
            .setNegativeButton("Abbrechen", null)
            .create()

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

                    val newFood = Food(
                        id = food?.id ?: 0,
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        category = category,
                        location = location,
                        expirationDate = date
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            if (food == null) {
                                foodDao.insert(newFood)
                            } else {
                                foodDao.update(newFood)
                            }
                        }
                    }

                    dialog.dismiss()
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
                        foodDao.delete(food)
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}
