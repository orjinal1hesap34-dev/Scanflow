package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.camera.CameraScanScreen
import com.example.ui.screens.cloud.CloudSyncScreen
import com.example.ui.screens.crop.CropPerspectiveScreen
import com.example.ui.screens.editor.PdfEditorScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.management.FoldersScreen
import com.example.ui.screens.management.TrashScreen
import com.example.ui.screens.ocr.OcrScreen
import com.example.ui.screens.premium.PremiumScreen
import com.example.ui.screens.security.SecurityLockScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.signature.SignatureScreen
import com.example.ui.screens.tools.PdfToolsScreen
import com.example.ui.theme.ScanFlowTheme
import com.example.ui.viewmodel.ScanFlowViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ScanFlowViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

            ScanFlowTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                val startDestination = if (isAppLocked) Screen.SecurityLock.route else Screen.Home.route

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.SecurityLock.route) {
                        SecurityLockScreen(
                            viewModel = viewModel,
                            onUnlocked = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.SecurityLock.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToCamera = { navController.navigate(Screen.CameraScan.route) },
                            onNavigateToEditor = { docId -> navController.navigate(Screen.Editor.createRoute(docId)) },
                            onNavigateToSignature = { navController.navigate(Screen.Signature.route) },
                            onNavigateToPdfTools = { navController.navigate(Screen.PdfTools.route) },
                            onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                            onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                            onNavigateToCloudSync = { navController.navigate(Screen.CloudSync.route) },
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToOcr = { docId -> navController.navigate(Screen.Ocr.createRoute(docId)) }
                        )
                    }

                    composable(Screen.CameraScan.route) {
                        CameraScanScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEditor = { docId ->
                                navController.navigate(Screen.Editor.createRoute(docId)) {
                                    popUpTo(Screen.CameraScan.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.Editor.route,
                        arguments = listOf(navArgument("documentId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                        PdfEditorScreen(
                            documentId = docId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCrop = { pageId -> navController.navigate(Screen.Crop.createRoute(pageId)) },
                            onNavigateToSignature = { navController.navigate(Screen.Signature.route) },
                            onNavigateToOcr = { dId -> navController.navigate(Screen.Ocr.createRoute(dId)) }
                        )
                    }

                    composable(
                        route = Screen.Crop.route,
                        arguments = listOf(navArgument("pageId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L
                        CropPerspectiveScreen(
                            pageId = pageId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Signature.route) {
                        SignatureScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Ocr.route,
                        arguments = listOf(navArgument("documentId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                        OcrScreen(
                            documentId = docId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.PdfTools.route) {
                        PdfToolsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEditor = { docId -> navController.navigate(Screen.Editor.createRoute(docId)) }
                        )
                    }

                    composable(Screen.Folders.route) {
                        FoldersScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEditor = { docId -> navController.navigate(Screen.Editor.createRoute(docId)) }
                        )
                    }

                    composable(Screen.Trash.route) {
                        TrashScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.CloudSync.route) {
                        CloudSyncScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Premium.route) {
                        PremiumScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
                        )
                    }
                }
            }
        }
    }
}
