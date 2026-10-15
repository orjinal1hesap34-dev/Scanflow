package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CameraScan : Screen("camera_scan")
    object Editor : Screen("editor/{documentId}") {
        fun createRoute(documentId: Long) = "editor/$documentId"
    }
    object Crop : Screen("crop/{pageId}") {
        fun createRoute(pageId: Long) = "crop/$pageId"
    }
    object Signature : Screen("signature")
    object Ocr : Screen("ocr/{documentId}") {
        fun createRoute(documentId: Long) = "ocr/$documentId"
    }
    object PdfTools : Screen("pdf_tools")
    object Folders : Screen("folders")
    object Trash : Screen("trash")
    object CloudSync : Screen("cloud_sync")
    object Premium : Screen("premium")
    object Settings : Screen("settings")
    object SecurityLock : Screen("security_lock")
}
