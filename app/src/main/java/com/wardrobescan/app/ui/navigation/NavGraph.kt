package com.wardrobescan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wardrobescan.app.ui.screen.*
import com.wardrobescan.app.ui.viewmodel.AuthViewModel
import com.wardrobescan.app.ui.viewmodel.SettingsViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val HOME = "home"
    const val SCAN = "scan"
    const val WARDROBE = "wardrobe"
    const val ITEM_DETAIL = "item/{itemId}"
    const val OUTFITS = "outfits"
    const val SETTINGS = "settings"

    fun itemDetail(itemId: String) = "item/$itemId"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = when {
        !settingsState.onboardingComplete -> Routes.ONBOARDING
        !authState.isAuthenticated -> Routes.AUTH
        else -> Routes.HOME
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    settingsViewModel.completeOnboarding()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                onGoogleSignIn = {
                    // Google Sign-In flow is handled via Activity result in production
                    // Placeholder for the Google Sign-In intent flow
                }
            )

            // Use LaunchedEffect so navigation only fires once when auth state changes,
            // not on every recomposition (which caused the infinite loop).
            LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            }
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToWardrobe = {
                    navController.navigate(Routes.WARDROBE) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onNavigateToOutfits = {
                    navController.navigate(Routes.OUTFITS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.WARDROBE) {
            WardrobeScreen(
                onNavigateToItem = { itemId ->
                    navController.navigate(Routes.itemDetail(itemId))
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToOutfits = {
                    navController.navigate(Routes.OUTFITS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onNavigateToScan = { navController.navigate(Routes.SCAN) }
            )
        }

        composable(
            Routes.ITEM_DETAIL,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) {
            ItemDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.OUTFITS) {
            OutfitScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToWardrobe = {
                    navController.navigate(Routes.WARDROBE) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToWardrobe = {
                    navController.navigate(Routes.WARDROBE) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onNavigateToOutfits = {
                    navController.navigate(Routes.OUTFITS) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
