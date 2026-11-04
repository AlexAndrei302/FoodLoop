
package com.example.foodloop

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            openFragment(PantryFragment())
        }

        findViewById<android.widget.Button>(R.id.btn_pantry)
            .setOnClickListener {
                openFragment(PantryFragment())
            }

        findViewById<android.widget.Button>(R.id.btn_recipes)
            .setOnClickListener {
                openFragment(RecipesFragment())
            }

        findViewById<android.widget.Button>(R.id.btn_shopping)
            .setOnClickListener {
                openFragment(ShoppingListFragment())
            }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
