package com.p56.safereach.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.R
import com.p56.safereach.debug.DebugScreen
import com.p56.safereach.home.HomeScreen

@PreviewScreenSizes
@Composable
fun SafeReachApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    name = "Android",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                )

                AppDestinations.FAVORITES -> HomeScreen(
                    name = "Favorites",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                )

                AppDestinations.PROFILE -> HomeScreen(
                    name = "Profile",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                )

                AppDestinations.DEBUG -> {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        DebugScreen(
                            onUpdateAlert = { alert ->
                                AlertStateRepository.update(alert)
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
    DEBUG("Debug", R.drawable.ic_debug),
}