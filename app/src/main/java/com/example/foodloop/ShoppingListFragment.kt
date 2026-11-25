
package com.example.foodloop

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.foodloop.database.FoodDatabase
import com.example.foodloop.database.ShoppingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.foodloop.repository.AuthRepository
import com.example.foodloop.repository.ShoppingCloudRepository
import com.example.foodloop.repository.CloudIdProvider
import android.widget.Toast

class ShoppingListFragment :
    Fragment(R.layout.fragment_shopping_list) {

    private lateinit var shoppingList: LinearLayout

    private val authRepository = AuthRepository()

    private val cloudRepository = ShoppingCloudRepository()

    private lateinit var currentUserId: String

    private val shoppingDao by lazy {
        FoodDatabase.getDatabase(requireContext())
            .shoppingItemDao()
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

        shoppingList = view.findViewById(R.id.shopping_list)

        val addButton: Button =
            view.findViewById(R.id.btn_add_shopping)


        addButton.setOnClickListener {
            showAddDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                shoppingDao.getAllItems(currentUserId).collect { items ->
                    displayItems(items)
                }
            }
        }
    }

    private fun displayItems(items: List<ShoppingItem>) {

        shoppingList.removeAllViews()

        if (items.isEmpty()) {
            val emptyText = TextView(requireContext())
            emptyText.text = "Die Einkaufsliste ist leer"
            emptyText.textSize = 16f
            emptyText.setPadding(12, 20, 12, 20)
            shoppingList.addView(emptyText)
            return
        }

        for (item in items) {

            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER_VERTICAL


            val checkBox = CheckBox(requireContext())
            checkBox.text = item.name
            checkBox.textSize = 18f
            checkBox.isChecked = item.checked

            checkBox.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )


            checkBox.setOnCheckedChangeListener { _, checked ->

                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val updatedItem = item.copy(
                            checked = checked
                        )

                        shoppingDao.update(updatedItem)

                        withContext(Dispatchers.Main) {
                            saveItemToCloud(updatedItem)
                        }
                    }
                }
            }


            val deleteButton = Button(requireContext())
            deleteButton.text = "Löschen"

            deleteButton.setOnClickListener {
                deleteItem(item)
            }

            row.addView(checkBox)
            row.addView(deleteButton)

            shoppingList.addView(row)
        }
    }


    private fun showAddDialog() {

        val input = EditText(requireContext())
        input.hint = "Produktname"
        input.setSingleLine(true)

        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 12, 32, 12)
        container.addView(input)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Produkt hinzufügen")
            .setView(container)
            .setPositiveButton("Speichern", null)
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name = input.text.toString().trim()

                if (name.isEmpty()) {
                    input.error = "Bitte Produktname eingeben"
                } else {

                    val item = ShoppingItem(
                        name = name,
                        userId = currentUserId
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val generatedId = shoppingDao.insert(item)

                            val savedItem = item.copy(
                                id = generatedId.toInt()
                            )

                            withContext(Dispatchers.Main) {
                                saveItemToCloud(savedItem)
                            }
                        }
                    }

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }



    private fun deleteItem(item: ShoppingItem) {

        val context = requireContext()

        AlertDialog.Builder(context)
            .setTitle("Produkt löschen")
            .setMessage("${item.name} wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->

                viewLifecycleOwner.lifecycleScope.launch {

                    try {

                        val affectedRows = withContext(Dispatchers.IO) {

                            shoppingDao.deleteItem(
                                item.id,
                                currentUserId
                            )
                        }

                        if (affectedRows > 0) {

                            deleteItemFromCloud(item)

                            Toast.makeText(
                                context,
                                "Produkt erfolgreich gelöscht",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            Toast.makeText(
                                context,
                                "Produkt nicht gefunden",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: kotlinx.coroutines.CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            "Fehler: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }



    private fun saveItemToCloud(item: ShoppingItem) {

        val context = requireContext().applicationContext

        if (authRepository.getCurrentUserId() != item.userId) {
            return
        }

        val cloudId = CloudIdProvider.getId(
            context,
            item.id
        )

        cloudRepository.saveItem(
            cloudId,
            item
        ) { success, message ->

            if (!success) {
                android.util.Log.e(
                    "FoodLoopCloud",
                    message
                )
            }
        }
    }

    private fun deleteItemFromCloud(item: ShoppingItem) {

        val context = requireContext().applicationContext

        if (authRepository.getCurrentUserId() != item.userId) {
            return
        }

        val cloudId = CloudIdProvider.getId(
            context,
            item.id
        )

        cloudRepository.deleteItem(
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
