package com.p56.safereach.auth

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.p56.safereach.navigation.SafeReachApp

@Composable
fun MainScreen(context: Context) {
    val auth = FirebaseAuth.getInstance()
    var user by remember { mutableStateOf(auth.currentUser) }
    var authScreen by remember { mutableStateOf("signin") }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            user = it.currentUser
            if (it.currentUser == null) {
                authScreen = "signin"
            }
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    if (user == null) {
        when (authScreen) {
            "signin" -> {
                SignIn(
                    context = context,
                    onForgotPasswordClick = { authScreen = "forgotPassword" },
                    onRegisterClick = { authScreen = "register" }
                )
            }

            "forgotPassword" -> {
                ForgotPasswordScreen(
                    context = context,
                    onBackToSignIn = { authScreen = "signin" }
                )
            }

            "register" -> {
                RegisterScreen(
                    context = context,
                    onBackToSignIn = { authScreen = "signin" },
                    onRegisterSuccess = {}
                )
            }
        }
    } else {
        SafeReachApp()
    }
}