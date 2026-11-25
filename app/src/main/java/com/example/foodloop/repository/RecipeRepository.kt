
package com.example.foodloop.repository

import com.example.foodloop.model.Recipe

class RecipeRepository {

    fun getRecipes(): List<Recipe> {

        return listOf(

            Recipe(
                "Pfannkuchen",
                listOf("Milch", "Eier", "Mehl"),
                "1. Milch, Eier und Mehl vermischen.\n" +
                        "2. Eine Pfanne erhitzen.\n" +
                        "3. Den Teig portionsweise ausbacken.",
                20
            ),

            Recipe(
                "Pasta mit Tomatensoße",
                listOf("Nudeln", "Tomaten", "Zwiebeln"),
                "1. Nudeln kochen.\n" +
                        "2. Zwiebeln und Tomaten anbraten.\n" +
                        "3. Die Soße mit den Nudeln vermischen.",
                25
            ),

            Recipe(
                "Käseomelett",
                listOf("Eier", "Käse", "Milch"),
                "1. Eier und Milch vermischen.\n" +
                        "2. Die Mischung in eine Pfanne geben.\n" +
                        "3. Käse hinzufügen und vollständig garen.",
                15
            ),

            Recipe(
                "Kartoffelsuppe",
                listOf("Kartoffeln", "Karotten", "Zwiebeln"),
                "1. Gemüse schälen und schneiden.\n" +
                        "2. In Wasser weich kochen.\n" +
                        "3. Pürieren und abschmecken.",
                35
            ),

            Recipe(
                "Tomatensalat",
                listOf("Tomaten", "Gurken", "Zwiebeln"),
                "1. Gemüse waschen.\n" +
                        "2. Alles klein schneiden.\n" +
                        "3. Vermischen und abschmecken.",
                10
            ),

            Recipe(
                "Käsesandwich",
                listOf("Brot", "Käse", "Butter"),
                "1. Brot mit Butter bestreichen.\n" +
                        "2. Käse hinzufügen.\n" +
                        "3. Nach Wunsch toasten.",
                10
            ),

            Recipe(
                "Gebratener Reis",
                listOf("Reis", "Eier", "Karotten"),
                "1. Reis kochen.\n" +
                        "2. Karotten anbraten.\n" +
                        "3. Reis und Eier hinzufügen und vollständig garen.",
                25
            ),

            Recipe(
                "Obstsalat",
                listOf("Äpfel", "Bananen", "Orangen"),
                "1. Obst waschen und schälen.\n" +
                        "2. Obst klein schneiden.\n" +
                        "3. Alles vermischen.",
                10
            ),

            Recipe(
                "Joghurt mit Banane",
                listOf("Joghurt", "Bananen"),
                "1. Banane klein schneiden.\n" +
                        "2. Mit Joghurt vermischen.\n" +
                        "3. Kühl servieren.",
                5
            ),

            Recipe(
                "Hähnchen mit Reis",
                listOf("Hähnchen", "Reis", "Karotten"),
                "1. Reis kochen.\n" +
                        "2. Hähnchen vollständig durchgaren.\n" +
                        "3. Karotten garen und alles servieren.",
                35
            )
        )
    }
}
