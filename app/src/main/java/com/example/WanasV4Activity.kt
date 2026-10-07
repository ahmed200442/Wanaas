package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.screens.WanasV4HubScreen
import com.example.ui.theme.MyApplicationTheme

class WanasV4Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val userName = intent.getStringExtra("user_name").orEmpty().ifBlank { "صديقي" }
        setContent {
            MyApplicationTheme {
                WanasV4HubScreen(
                    currentUserName = userName,
                    onBack = { finish() }
                )
            }
        }
    }
}
