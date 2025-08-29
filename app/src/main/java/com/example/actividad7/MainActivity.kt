package com.example.actividad7

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.example.actividad7.ui.screens.*
import com.example.actividad7.ui.theme.Actividad7Theme
import com.example.actividad7.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Usuario denegó el permiso, podrías mostrar un diálogo explicativo
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Solicitar permisos de notificación para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            Actividad7Theme {
                AppointmentApp()
            }
        }
    }
}

@Composable
fun AppointmentApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Configurar el contexto en el ViewModel
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "auth",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pantalla de autenticación
            composable("auth") {
                AuthScreen(
                    viewModel = viewModel,
                    onNavigateToMain = {
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            
            // Pantalla principal
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToBookAppointment = {
                        navController.navigate("book_appointment")
                    },
                    onNavigateToHistory = {
                        navController.navigate("history")
                    },
                    onSignOut = {
                        viewModel.signOut()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            
            // Pantalla de reserva de citas
            composable("book_appointment") {
                BookAppointmentScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Pantalla de historial
            composable("history") {
                AppointmentsHistoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}