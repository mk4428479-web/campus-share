package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Listing
import com.example.data.repository.MarketplaceRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

// Clear navigation routes
sealed class Screen {
    object Splash : Screen()
    object Home : Screen()
    object Profile : Screen()
    object AddListing : Screen()
    data class Detail(val listing: Listing) : Screen()
    data class Chat(val listing: Listing) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core room dependencies initialization
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MarketplaceRepository(database.listingDao(), database.chatMessageDao())
        val viewModelFactory = MainViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                // Keep track of dynamic backstack
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                val backstack = remember { mutableStateListOf<Screen>() }

                fun navigateTo(screen: Screen) {
                    backstack.add(currentScreen)
                    currentScreen = screen
                }

                fun navigateBack() {
                    if (backstack.isNotEmpty()) {
                        currentScreen = backstack.removeAt(backstack.size - 1)
                    } else {
                        currentScreen = Screen.Home
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Show bottom nav ONLY on core browse screens (Home and Profile)
                        if (currentScreen is Screen.Home || currentScreen is Screen.Profile) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Home,
                                    onClick = {
                                        if (currentScreen !is Screen.Home) {
                                            backstack.clear()
                                            currentScreen = Screen.Home
                                        }
                                    },
                                    label = { Text("Explore") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen is Screen.Home) Icons.Default.Home else Icons.Outlined.Home,
                                            contentDescription = "Explore"
                                        )
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen is Screen.Profile,
                                    onClick = {
                                        if (currentScreen !is Screen.Profile) {
                                            backstack.clear()
                                            currentScreen = Screen.Profile
                                        }
                                    },
                                    label = { Text("Profile") },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentScreen is Screen.Profile) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle,
                                            contentDescription = "Profile"
                                        )
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Page transition crossfading container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = if (currentScreen is Screen.Home || currentScreen is Screen.Profile) {
                                    innerPadding.calculateBottomPadding()
                                } else {
                                    0.dp // No padding if bottom bar hidden
                                }
                            )
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "PageTransition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Splash -> {
                                    SplashScreen(
                                        onExploreClick = {
                                            currentScreen = Screen.Home
                                        }
                                    )
                                }

                                is Screen.Home -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onListingClick = { listing ->
                                            viewModel.selectListing(listing)
                                            navigateTo(Screen.Detail(listing))
                                        },
                                        onProfileClick = {
                                            backstack.clear()
                                            currentScreen = Screen.Profile
                                        },
                                        onAddListingClick = {
                                            navigateTo(Screen.AddListing)
                                        },
                                        onMessageClick = { listing ->
                                            viewModel.selectChatListing(listing)
                                            navigateTo(Screen.Chat(listing))
                                        }
                                    )
                                }

                                is Screen.Detail -> {
                                    DetailScreen(
                                        viewModel = viewModel,
                                        onBackClick = { navigateBack() },
                                        onChatClick = { listing ->
                                            viewModel.selectChatListing(listing)
                                            navigateTo(Screen.Chat(listing))
                                        }
                                    )
                                }

                                is Screen.Chat -> {
                                    ChatScreen(
                                        viewModel = viewModel,
                                        onBackClick = { navigateBack() }
                                    )
                                }

                                is Screen.AddListing -> {
                                    AddListingScreen(
                                        viewModel = viewModel,
                                        onBackClick = { navigateBack() },
                                        onPublishSuccess = {
                                            // Back to home
                                            backstack.clear()
                                            currentScreen = Screen.Home
                                        }
                                    )
                                }

                                is Screen.Profile -> {
                                    ProfileScreen(
                                        viewModel = viewModel,
                                        onListingClick = { listing ->
                                            viewModel.selectListing(listing)
                                            navigateTo(Screen.Detail(listing))
                                        },
                                        onLogoutClick = {
                                            // Simple return to splash onboarding feel
                                            backstack.clear()
                                            currentScreen = Screen.Splash
                                        },
                                        onMessageClick = { listing ->
                                            viewModel.selectChatListing(listing)
                                            navigateTo(Screen.Chat(listing))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
