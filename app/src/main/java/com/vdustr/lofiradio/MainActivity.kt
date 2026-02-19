package com.vdustr.lofiradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vdustr.lofiradio.ui.MainScreen
import com.vdustr.lofiradio.ui.theme.LofiRadioTheme
import com.vdustr.lofiradio.viewmodel.RadioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LofiRadioTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
