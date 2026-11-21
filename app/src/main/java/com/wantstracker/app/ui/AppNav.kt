package com.wantstracker.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wantstracker.app.ui.screens.CategoriesScreen
import com.wantstracker.app.ui.screens.DetailScreen
import com.wantstracker.app.ui.screens.HomeScreen
import com.wantstracker.app.ui.screens.ImportScreen
import com.wantstracker.app.ui.screens.InsightsScreen
import com.wantstracker.app.ui.screens.SettingsScreen
import com.wantstracker.app.ui.screens.WantFormScreen

object Routes {
    const val HOME = "home"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val IMPORT = "import"
    const val FORM = "form/{id}"
    const val DETAIL = "detail/{id}"
    fun form(id: Long) = "form/$id"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val idArg = listOf(navArgument("id") { type = NavType.LongType })
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(vm, nav) }
        composable(Routes.INSIGHTS) { InsightsScreen(vm, nav) }
        composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
        composable(Routes.CATEGORIES) { CategoriesScreen(vm, nav) }
        composable(Routes.IMPORT) { ImportScreen(vm, nav) }
        composable(Routes.FORM, arguments = idArg) { e ->
            WantFormScreen(vm, nav, e.arguments?.getLong("id") ?: 0L)
        }
        composable(Routes.DETAIL, arguments = idArg) { e ->
            DetailScreen(vm, nav, e.arguments?.getLong("id") ?: 0L)
        }
    }
}
