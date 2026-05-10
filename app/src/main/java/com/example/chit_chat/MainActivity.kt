package com.example.chit_chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chit_chat.ui.auth.LoginScreen
import com.example.chit_chat.ui.auth.RegisterScreen
import com.example.chit_chat.ui.chat.ChatRoomScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

import com.example.chit_chat.ui.matchmaking.MatchmakingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        FirebaseApp.initializeApp(this)
        
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val currentUser = FirebaseAuth.getInstance().currentUser
                val startDestination = if (currentUser != null) "matchmaking" else "login"

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("matchmaking") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("matchmaking") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("matchmaking") {
                            MatchmakingScreen(
                                onMatched = { roomId ->
                                    navController.navigate("chat/$roomId") {
                                        popUpTo("matchmaking") { inclusive = false }
                                    }
                                }
                            )
                        }
                        composable("chat/{roomId}") { backStackEntry ->
                            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                            ChatRoomScreen(
                                roomId = roomId,
                                onLeaveRoom = {
                                    navController.navigate("matchmaking") {
                                        popUpTo("matchmaking") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
