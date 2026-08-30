package com.perkz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.perkz.ui.screen.PerkScreen
import com.perkz.ui.theme.PerkzTheme
import com.perkz.viewmodel.PerkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            PerkViewModel.Factory(application)
        )[PerkViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            PerkzTheme(themeMode = uiState.themeMode) {
                PerkScreen(viewModel = viewModel)
            }
        }
    }
}
