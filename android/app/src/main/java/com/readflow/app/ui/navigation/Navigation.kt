package com.readflow.app.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.readflow.app.ui.library.LibraryScreen
import com.readflow.app.ui.reader.ReaderScreen
import com.readflow.app.ui.settings.SettingsScreen
import com.readflow.app.ui.archive.ArchiveScreen

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Reader : Screen("reader/{documentId}") {
        fun createRoute(documentId: String) = "reader/$documentId"
    }
    data object Settings : Screen("settings")
    data object Archive : Screen("archive")
    data object Notes : Screen("notes")
}

@Composable
fun ReadFlowNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route,
        modifier = modifier
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable

            ReaderScreen(
                documentId = documentId,
                onBack = { navController.popBackStack() },
                onAiClick = { docId, pageIndex, selectedText ->
                    // Navigate to AI panel or show bottom sheet
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Archive.route) {
            ArchiveScreen(
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.Reader.createRoute(documentId))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
