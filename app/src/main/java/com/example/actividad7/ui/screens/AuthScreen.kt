package com.example.actividad7.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.actividad7.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onNavigateToMain: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState) {
        if (uiState is com.example.actividad7.viewmodel.UiState.Authenticated) {
            onNavigateToMain()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Crear Cuenta" else "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                )
            )
        }
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )
        
        if (uiState is com.example.actividad7.viewmodel.UiState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = (uiState as com.example.actividad7.viewmodel.UiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
        
        Button(
            onClick = {
                if (isSignUp) {
                    viewModel.signUp(email, password, name)
                } else {
                    viewModel.signIn(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && 
                     (!isSignUp || name.isNotBlank()) &&
                     uiState !is com.example.actividad7.viewmodel.UiState.Loading
        ) {
            if (uiState is com.example.actividad7.viewmodel.UiState.Loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Procesando...")
                }
            } else {
                Text(if (isSignUp) "Registrarse" else "Iniciar Sesión")
            }
        }
        
        TextButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                if (isSignUp) "¿Ya tienes cuenta? Inicia sesión" 
                else "¿No tienes cuenta? Regístrate"
            )
        }
        
        // Botón de prueba para crear usuario
        if (!isSignUp) {
            TextButton(
                onClick = { 
                    // Usar timestamp para hacer el email único
                    val timestamp = System.currentTimeMillis()
                    viewModel.signUp("test$timestamp@test.com", "123456", "Usuario Prueba")
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Crear Usuario de Prueba")
            }
            
            // Botón para iniciar sesión con usuario de prueba existente
            TextButton(
                onClick = { 
                    viewModel.signIn("test@test.com", "123456")
                },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Iniciar Sesión con Usuario de Prueba")
            }
        }
    }
}

