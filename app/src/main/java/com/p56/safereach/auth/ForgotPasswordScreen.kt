package com.p56.safereach.auth

// Shello Yson
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.p56.safereach.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun ForgotPasswordScreen(
    context: Context,
    onSignInClick: () -> Unit
){
    var email by rememberSaveable {mutableStateOf("")}
    var loading by rememberSaveable {mutableStateOf(false)}
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember {MutableInteractionSource()}
            ){focusManager.clearFocus()}
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(
            id = R.drawable.img_safereach_logo),
            contentDescription = "SafeReach Logo",
            modifier = Modifier
                .width(110.dp)
                .height(110.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your email address and we will send you a password reset link.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = email, onValueChange = {email = it},
            label = {Text("Email")},
            placeholder = {Text("Email")},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && email.isNotBlank(),
            onClick = {
                loading = true
                FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(email.trim())
                    .addOnCompleteListener { task -> loading =false
                    if (task.isSuccessful){
                        Toast.makeText(context, "Password reset link sent", Toast.LENGTH_LONG).show()}
                        else{Toast.makeText(context, task.exception?.message
                        ?: "Unable to send reset link", Toast.LENGTH_LONG).show()}
                    }
            }
        ){
            Text(
                text = if (loading){"Sending..."}
                else {"Send Reset Link"}
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        //TODO: Add to navigation later
        TextButton(onClick = {
            onSignInClick()
        }) {
            Text(
                text = "Back to Sign In"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview(){
    ForgotPasswordScreen(
        context = LocalContext.current,
        onSignInClick = {}
    )
}