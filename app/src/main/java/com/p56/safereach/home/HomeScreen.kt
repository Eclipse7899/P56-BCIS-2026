package com.p56.safereach.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.p56.safereach.ui.theme.SafeReachTheme

@Composable
fun HomeScreen(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
    Button(
        onClick = {
            FirebaseAuth.getInstance().signOut()
        },
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text("Sign Out")
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SafeReachTheme {
        HomeScreen("Android")
    }
}
