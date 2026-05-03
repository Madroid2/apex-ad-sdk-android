package com.velora.app.navigation

/** Type-safe route definitions for the Velora navigation graph. */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Cart : Screen("cart")
    data object Profile : Screen("profile")

    data class ProductDetail(val productId: String = "{productId}") :
        Screen("product/{productId}") {
        companion object {
            fun routeFor(id: String) = "product/$id"
        }
    }
}
