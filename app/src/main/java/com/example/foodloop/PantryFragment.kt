
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

class PantryFragment : Fragment(R.layout.fragment_pantry) {

    private lateinit var foodList: LinearLayout

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

            val photoPath = food.photoPath

            if (photoPath != null && File(photoPath).exists()) {

                val imageView = ImageView(requireContext())

                imageView.layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (200 * resources.displayMetrics.density).toInt()
                    )

                imageView.scaleType =
                    ImageView.ScaleType.FIT_CENTER

                imageView.setImageURI(
                    Uri.fromFile(File(photoPath))
                )

                imageView.setOnClickListener {
                    showFullPhoto(photoPath)
                }

                item.addView(imageView)
            }


            item.addView(nameText)
            item.addView(infoText)
            item.addView(editButton)
            if (food.photoPath != null) {

                val removePhotoButton = Button(requireContext())

                removePhotoButton.text = "Foto entfernen"

                removePhotoButton.setOnClickListener {
                    removeFoodPhoto(food)
                }

                item.addView(removePhotoButton)
            }
            item.addView(deleteButton)

            foodList.addView(item)
        }
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


}
