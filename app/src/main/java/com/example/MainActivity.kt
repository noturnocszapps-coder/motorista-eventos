package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.RoxouAppPortal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RoxouViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as RoxouApplication).repository

        setContent {
            MyApplicationTheme {
                val viewModel: RoxouViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return RoxouViewModel(repository) as T
                        }
                    }
                )

                RoxouAppPortal(viewModel = viewModel)
            }
        }
    }
}
