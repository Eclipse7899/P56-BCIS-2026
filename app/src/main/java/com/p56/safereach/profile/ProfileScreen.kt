package com.p56.safereach.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
@Preview(showBackground = true)
fun ProfileScreen() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            style = MaterialTheme.typography.headlineMedium,
            text = "Your Profile"
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
            }
        ) {
            Text(text = "Sign Out")
        }
    }
}