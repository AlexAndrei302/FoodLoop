package com.example.foodloop

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.foodloop.database.Food
import com.example.foodloop.database.FoodDatabase
import com.example.foodloop.model.Recipe
import com.example.foodloop.repository.AuthRepository
import com.example.foodloop.repository.RecipeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    private lateinit var recipeList: LinearLayout

    private val authRepository = AuthRepository()

    private val recipeRepository = RecipeRepository()

    private val foodDao by lazy {
        FoodDatabase.getDatabase(requireContext()).foodDao()
    }

    private data class Recommendation(
        val recipe: Recipe,
        val available: List<String>,
        val missing: List<String>,
        val expiring: List<String>,
        val percentage: Int
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        recipeList = view.findViewById(R.id.recipe_list)

        val userId = authRepository.getCurrentUserId()
            ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                foodDao.getAllFoods(userId).collect { foods ->

                    displayRecipes(foods)
                }
            }
        }
    }

    private fun normalize(value: String): String {

        return value.lowercase(Locale.GERMAN)
            .trim()
            .replace("ä", "a")
            .replace("ö", "o")
            .replace("ü", "u")
            .replace("ß", "ss")
    }

    private fun matchesIngredient(
        foodName: String,
        ingredient: String
    ): Boolean {

        val food = normalize(foodName)

        val required = normalize(ingredient)

        val singular = when (required) {

            "eier" -> "ei"
            "apfel" -> "apfel"
            "bananen" -> "banane"
            "tomaten" -> "tomate"
            "kartoffeln" -> "kartoffel"
            "karotten" -> "karotte"
            "zwiebeln" -> "zwiebel"
            "gurken" -> "gurke"
            "orangen" -> "orange"
            "nudeln" -> "nudel"

            else -> required
        }

        if (singular == "ei") {
            return food == "ei" ||
                    food == "eier" ||
                    food.contains("huhnerei") ||
                    food.contains("huhnereier")
        }

        return food.contains(singular)
    }

    private fun displayRecipes(foods: List<Food>) {

        recipeList.removeAllViews()

        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        val calendar = Calendar.getInstance()

        val today = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, 3)

        val limit = dateFormat.format(calendar.time)

        val usableFoods = foods.filter {

            it.quantity > 0 &&
                    it.expirationDate >= today
        }

        val recommendations = recipeRepository
            .getRecipes()
            .map { recipe ->

                val available = recipe.ingredients.filter {
                        ingredient ->

                    usableFoods.any { food ->

                        matchesIngredient(
                            food.name,
                            ingredient
                        )
                    }
                }

                val missing = recipe.ingredients.filter {
                    it !in available
                }

                val expiring = usableFoods.filter { food ->

                    food.expirationDate <= limit &&
                            recipe.ingredients.any { ingredient ->

                                matchesIngredient(
                                    food.name,
                                    ingredient
                                )
                            }

                }.map { it.name }.distinct()

                val percentage =
                    available.size * 100 /
                            recipe.ingredients.size

                Recommendation(
                    recipe,
                    available,
                    missing,
                    expiring,
                    percentage
                )

            }.sortedWith(

                compareByDescending<Recommendation> {
                    it.percentage
                }.thenByDescending {
                    it.expiring.size
                }
            )

        for (recommendation in recommendations) {

            addRecipeCard(recommendation)
        }
    }

    private fun addRecipeCard(
        recommendation: Recommendation
    ) {

        val context = requireContext()

        val card = LinearLayout(context)

        card.orientation = LinearLayout.VERTICAL

        card.setPadding(
            dp(16),
            dp(16),
            dp(16),
            dp(16)
        )

        val background = GradientDrawable()

        background.setColor(Color.WHITE)

        background.cornerRadius = dp(16).toFloat()

        background.setStroke(
            dp(1),
            Color.parseColor("#DDE8DD")
        )

        card.background = background

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = dp(14)

        card.layoutParams = params

        val title = TextView(context)

        title.text = recommendation.recipe.name

        title.textSize = 21f

        title.setTypeface(null, Typeface.BOLD)

        title.setTextColor(
            Color.parseColor("#2E7D32")
        )

        card.addView(title)

        val time = TextView(context)

        time.text =
            "Zubereitungszeit: " +
                    "${recommendation.recipe.preparationTime} Min."

        time.textSize = 14f

        card.addView(time)

        val percentage = TextView(context)

        percentage.text =
            "${recommendation.percentage}% der Zutaten vorhanden"

        percentage.textSize = 16f

        percentage.setTextColor(
            Color.parseColor("#2E7D32")
        )

        percentage.setPadding(0, dp(10), 0, 0)

        card.addView(percentage)

        val progressBar = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        )

        progressBar.max = 100

        progressBar.progress =
            recommendation.percentage

        progressBar.progressTintList =
            ColorStateList.valueOf(
                Color.parseColor("#388E3C")
            )

        card.addView(
            progressBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(12)
            )
        )

        val available = TextView(context)

        available.text =
            "Vorhanden: " +
                    recommendation.available
                        .joinToString(", ")
                        .ifEmpty { "Keine Zutaten" }

        available.setTextColor(
            Color.parseColor("#2E7D32")
        )

        available.setPadding(0, dp(8), 0, 0)

        card.addView(available)

        val missing = TextView(context)

        missing.text =
            "Fehlend: " +
                    recommendation.missing
                        .joinToString(", ")
                        .ifEmpty { "Keine" }

        missing.setTextColor(
            Color.parseColor("#B45309")
        )

        card.addView(missing)

        if (recommendation.expiring.isNotEmpty()) {

            val warning = TextView(context)

            warning.text =
                "Laufen bald ab: " +
                        recommendation.expiring.joinToString(", ")

            warning.setTextColor(
                Color.parseColor("#D97706")
            )

            warning.setTypeface(null, Typeface.BOLD)

            warning.setPadding(0, dp(8), 0, 0)

            card.addView(warning)
        }

        val button = Button(context)

        button.text = "Rezept anzeigen"

        button.backgroundTintList =
            ColorStateList.valueOf(
                Color.parseColor("#2E7D32")
            )

        button.setTextColor(Color.WHITE)

        button.setOnClickListener {

            showRecipeDialog(recommendation.recipe)
        }

        card.addView(button)

        recipeList.addView(card)
    }

    private fun showRecipeDialog(recipe: Recipe) {

        AlertDialog.Builder(requireContext())

            .setTitle(recipe.name)

            .setMessage(

                "Zubereitungszeit: " +
                        "${recipe.preparationTime} Minuten\n\n" +

                        "Zutaten:\n" +

                        recipe.ingredients.joinToString("\n") {
                            "• $it"
                        } +

                        "\n\nZubereitung:\n\n" +

                        recipe.instructions
            )

            .setPositiveButton("Schließen", null)

            .show()
    }

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}
