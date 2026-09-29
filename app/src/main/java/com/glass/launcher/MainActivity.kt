package com.glass.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
            val selectedAppForMenu by viewModel.selectedAppForMenu.collectAsState()
            val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

            // Back button handling: Close modals/drawer rather than leaving home screen
            BackHandler(enabled = isDrawerOpen || selectedAppForMenu != null || isSettingsOpen) {
                if (selectedAppForMenu != null) {
                    viewModel.openAppMenu(null)
                } else if (isSettingsOpen) {
                    viewModel.toggleSettings(false)
                } else if (isDrawerOpen) {
                    viewModel.toggleDrawer(false)
                }
            }

            GlassHomeScreen(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh apps list when returning to home in case apps were installed or removed
        viewModel.loadInstalledApps()
    }
}
