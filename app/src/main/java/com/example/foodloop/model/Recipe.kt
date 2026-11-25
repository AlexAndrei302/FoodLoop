
package com.example.foodloop.model

data class Recipe(
    val name: String,
    val ingredients: List<String>,
    val instructions: String,
    val preparationTime: Int
)
