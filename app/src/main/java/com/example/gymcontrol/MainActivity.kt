package com.example.gymcontrol

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.gymcontrol.navigation.GymNavGraph
import com.example.gymcontrol.ui.components.LocaleHelper
import com.example.gymcontrol.ui.theme.GymTheme

class MainActivity : ComponentActivity() {

    // Aplica el idioma guardado antes de crear la vista de la Activity
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymTheme {
                GymNavGraph()
            }
        }
    }
}