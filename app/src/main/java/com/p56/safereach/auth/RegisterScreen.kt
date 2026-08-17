package com.p56.safereach.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.p56.safereach.R

@Composable
fun RegisterScreen(
    context: Context,
    onBackToSignIn: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_safereach_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .width(128.dp)
                .height(128.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to SafeReach",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Register to create a profile",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                placeholder = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val painter = if (passwordVisible) {
                        painterResource(id = R.drawable.ic_visibility_24px)
                    } else {
                        painterResource(id = R.drawable.ic_visibility_off_24px)
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painter, contentDescription =
                                if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                placeholder = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation =
                    if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val painter = if (confirmPasswordVisible) {
                        painterResource(id = R.drawable.ic_visibility_24px)
                    } else {
                        painterResource(id = R.drawable.ic_visibility_off_24px)
                    }
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            painter = painter,
                            contentDescription = if (confirmPasswordVisible) {
                                "Hide password"
                            } else {
                                "Show password"
                            }
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            val buttonDisabled =
                loading || email.isBlank() || password.isBlank() || confirmPassword.isBlank()
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !buttonDisabled,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 12.dp),
                onClick = {
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.length < 6) {
                        Toast.makeText(
                            context,
                            "Password must be at least 6 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    loading = true
                    val auth = FirebaseAuth.getInstance()
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Profile created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onRegisterSuccess()
                            } else {
                                Toast.makeText(
                                    context, task.exception?.message ?: "Registration failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            ) {
                AnimatedContent(targetState = loading, label = "registerLoading") { isLoading ->
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onBackToSignIn() })
            {
                Text(
                    text = "Already have an profile? Sign In"
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun RegisterScreenPreview() {
    RegisterScreen(
        context = LocalContext.current,
        onBackToSignIn = {},
        onRegisterSuccess = {}
    )
}