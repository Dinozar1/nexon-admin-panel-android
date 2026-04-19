// app/src/main/java/com/example/nexonpayadminpanel/ui/login/LoginScreen.kt
package com.example.nexonpayadminpanel.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit // Callback triggered when the backend returns "OK"
) {
    // 1. STATE OBSERVATION: Listen to the ViewModel's state (Loading, Success, Error)
    val uiState by viewModel.uiState.collectAsState()

    // 2. LOCAL STATE: Store what the user is currently typing
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 3. SIDE EFFECTS: React to state changes (e.g., successful login)
    LaunchedEffect(uiState) {
        if (uiState is LoginState.Success) {
            onLoginSuccess() // Trigger navigation to the Dashboard
            viewModel.resetState() // Prevent accidental re-navigation if user presses 'Back'
        }
    }

    // 4. UI LAYOUT
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header text
            Text(
                text = "Panel Admina",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Zaloguj się, aby zarządzać systemem",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Dynamic error message display
            if (uiState is LoginState.Error) {
                Text(
                    text = (uiState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Email input field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Adres E-mail") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Email Icon") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                )
            )

            // Password input field (hides characters)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(), // Masks input as '***'
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary
                )
            )

            // Login submit button
            Button(
                onClick = { viewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState !is LoginState.Loading // Disable button while fetching
            ) {
                // Show loader if waiting for API, otherwise show text
                if (uiState is LoginState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Zaloguj się", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}