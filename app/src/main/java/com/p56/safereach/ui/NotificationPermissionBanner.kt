package com.p56.safereach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.p56.safereach.R
import com.p56.safereach.ui.theme.SafeReachTheme

@Composable
fun NotificationPermissionBanner(
    modifier: Modifier = Modifier,
    onEnableClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = "Notifications Disabled Warning",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Notifications Are Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "The emergency mesh runs as a foreground service, which needs to show a " +
                    "notification. Notifications are disabled, so SafeReach can't keep the mesh " +
                    "running in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = "Enable Notifications")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationPermissionBannerPreview() {
    SafeReachTheme {
        NotificationPermissionBanner(
            onEnableClick = {},
        )
    }
}
