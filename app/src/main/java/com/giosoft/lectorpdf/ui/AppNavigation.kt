package com.giosoft.lectorpdf.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.giosoft.lectorpdf.ui.library.LibraryScreen
import com.giosoft.lectorpdf.ui.viewer.ViewerScreen
import com.giosoft.lectorpdf.ui.viewer.ViewerViewModel

object Routes {
    const val LIBRARY = "library"
    const val VIEWER = "viewer/{${ViewerViewModel.ARG_URI}}"

    fun viewer(documentUri: String) = "viewer/${Uri.encode(documentUri)}"
}

@Composable
fun AppNavigation(
    /** PDF recibido por intent desde otra app; se abre nada mas arrancar. */
    initialDocumentUri: String? = null,
    onInitialDocumentConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(initialDocumentUri) {
        if (initialDocumentUri != null) {
            navController.navigate(Routes.viewer(initialDocumentUri))
            onInitialDocumentConsumed()
        }
    }

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenDocument = { uri -> navController.navigate(Routes.viewer(uri)) },
            )
        }
        composable(
            route = Routes.VIEWER,
            arguments = listOf(navArgument(ViewerViewModel.ARG_URI) { type = NavType.StringType }),
        ) {
            ViewerScreen(onBack = { navController.popBackStack() })
        }
    }
}
