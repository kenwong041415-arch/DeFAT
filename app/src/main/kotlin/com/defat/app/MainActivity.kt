package com.defat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.defat.app.navigation.DefatNavHost
import com.defat.app.ui.theme.DefatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DefatApp(viewModel)
                }
            }
        }
    }
}

@Composable
private fun DefatApp(viewModel: MainViewModel) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

    // Latch the first non-null value. `startDestination` keeps emitting as the
    // profile changes, and feeding a live value into NavHost would rebuild the
    // graph mid-session — NavController.setGraph resets the back stack, racing
    // with the explicit navigate(HOME) after onboarding.
    var latched by rememberSaveable { mutableStateOf<StartDestination?>(null) }
    LaunchedEffect(startDestination) {
        if (latched == null && startDestination != null) latched = startDestination
    }

    val destination = latched
    if (destination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val navController = rememberNavController()
        DefatNavHost(navController = navController, startDestination = destination)
    }
}
