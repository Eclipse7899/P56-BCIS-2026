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
    var authScreen by remember { mutableStateOf(AuthScreen.SIGN_IN) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener {
            user = it.currentUser
            if (it.currentUser == null) {
                authScreen = AuthScreen.SIGN_IN
            }
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }
    if (user == null) {
        when (authScreen) {
            AuthScreen.SIGN_IN -> {
                SignInScreen(
                    context = context,
                    onForgotPasswordClick = { authScreen = AuthScreen.FORGOT_PASSWORD },
                    onRegisterClick = { authScreen = AuthScreen.REGISTER }
                )
            }

            AuthScreen.FORGOT_PASSWORD -> {
                ForgotPasswordScreen(
                    context = context,
                    onBackToSignIn = { authScreen = AuthScreen.SIGN_IN }
                )
            }

            AuthScreen.REGISTER -> {
                RegisterScreen(
                    context = context,
                    onBackToSignIn = { authScreen = AuthScreen.SIGN_IN },
                    onRegisterSuccess = {}
                )
            }
        }
    } else {
        SafeReachApp()
    }
}

private enum class AuthScreen {
    SIGN_IN,
    FORGOT_PASSWORD,
    REGISTER,
}