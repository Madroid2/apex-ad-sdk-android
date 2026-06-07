package com.apexads.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.apexads.demo.ui.appopen.AppOpenScreen
import com.apexads.demo.ui.banner.BannerScreen
import com.apexads.demo.ui.inappbidding.InAppBiddingScreen
import com.apexads.demo.ui.interstitial.InterstitialScreen
import com.apexads.demo.ui.native.NativeScreen
import com.apexads.demo.ui.theme.ApexTheme
import com.apexads.demo.ui.video.VideoScreen
import com.apexads.demo.ui.wallet.WalletScreen
import com.apexads.demo.viewmodel.AdViewModel

class MainActivity : ComponentActivity() {

    private val adViewModel: AdViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApexTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavDestination.entries.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavDestination.Banner.route,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(NavDestination.Banner.route) { BannerScreen(adViewModel) }
                        composable(NavDestination.Interstitial.route) { InterstitialScreen(adViewModel) }
                        composable(NavDestination.Native.route) { NativeScreen(adViewModel) }
                        composable(NavDestination.Video.route) { VideoScreen(adViewModel) }
                        composable(NavDestination.AppOpen.route) { AppOpenScreen() }
                        composable(NavDestination.Bidding.route) { InAppBiddingScreen(adViewModel) }
                        composable(NavDestination.Wallet.route) { WalletScreen() }
                    }
                }
            }
        }
    }
}

/** Bottom navigation destinations. Each entry maps to a composable route in the [NavHost]. */
enum class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Banner("banner", "Banner", Icons.Default.Image),
    Interstitial("interstitial", "Interst.", Icons.Default.Fullscreen),
    Native("native", "Native", Icons.AutoMirrored.Filled.Article),
    Video("video", "Video", Icons.Default.PlayCircle),
    AppOpen("appopen", "App Open", Icons.AutoMirrored.Filled.OpenInNew),
    Bidding("bidding", "Bidding", Icons.Default.Gavel),
    Wallet("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
}
