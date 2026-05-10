package com.example.chit_chat.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()
    
    LoginContent(
        authState = authState,
        onLoginClick = { email, pass -> viewModel.signIn(email, pass) },
        onNavigateToRegister = onNavigateToRegister,
        onLoginSuccess = onLoginSuccess
    )
}

@Composable
fun LoginContent(
    authState: AuthState,
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login ChitChat",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Masuk")
            }
            
            TextButton(onClick = onNavigateToRegister) {
                Text("Belum punya akun? Daftar di sini")
            }
        }

        if (authState is AuthState.Error) {
            Text(
                text = authState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Login Screen - Idle")
@Composable
fun LoginPreviewIdle() {
    MaterialTheme {
        LoginContent(
            authState = AuthState.Idle,
            onLoginClick = { _, _ -> },
            onNavigateToRegister = {},
            onLoginSuccess = {}
        )
    }
}

@Preview(showBackground = true, name = "Login Screen - Loading")
@Composable
fun LoginPreviewLoading() {
    MaterialTheme {
        LoginContent(
            authState = AuthState.Loading,
            onLoginClick = { _, _ -> },
            onNavigateToRegister = {},
            onLoginSuccess = {}
        )
    }
}

@Preview(showBackground = true, name = "Login Screen - Error")
@Composable
fun LoginPreviewError() {
    MaterialTheme {
        LoginContent(
            authState = AuthState.Error("Invalid credentials"),
            onLoginClick = { _, _ -> },
            onNavigateToRegister = {},
            onLoginSuccess = {}
        )
    }
}
