package com.glass.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassHomeScreen(
    viewModel: LauncherViewModel
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val currentStyle by viewModel.currentStyle.collectAsState()
    val showLabels by viewModel.showLabels.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    val selectedAppForMenu by viewModel.selectedAppForMenu.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredApps = if (searchQuery.isBlank()) {
        allApps
    } else {
        allApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Ambient semi-translucent dark vignette so icons and clock pop gorgeously over any wallpaper
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x30000000),
                        Color(0x15000000),
                        Color(0x50000000)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Live Frosted Glass Clock
            GlassClockWidget(
                onSettingsClick = { viewModel.toggleSettings(true) }
            )

            // Main Desktop Apps Grid
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00B4D8))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    items(allApps) { app ->
                        GlassIconButton(
                            app = app,
                            style = currentStyle,
                            showLabel = showLabels,
                            iconSizeDp = if (gridColumns == 4) 62 else 54,
                            onClick = { viewModel.launchApp(context, app) },
                            onLongClick = { viewModel.openAppMenu(app) }
                        )
                    }
                }
            }
        }

        // Floating Bottom Glass Dock
        GlassDock(
            dockApps = dockApps,
            style = currentStyle,
            onAppClick = { viewModel.launchApp(context, it) },
            onAppLongClick = { viewModel.openAppMenu(it) },
            onDrawerClick = { viewModel.toggleDrawer(true) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Glass App Drawer Overlay
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD90D1117))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("ابحث عن أي تطبيق...", color = Color(0x90FFFFFF), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White) },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (searchQuery.isNotBlank()) viewModel.setSearchQuery("")
                                else viewModel.toggleDrawer(false)
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color(0x40FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x30FFFFFF),
                            unfocusedContainerColor = Color(0x20FFFFFF)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Drawer Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                    ) {
                        items(filteredApps) { app ->
                            GlassIconButton(
                                app = app,
                                style = currentStyle,
                                showLabel = true,
                                iconSizeDp = 60,
                                onClick = {
                                    viewModel.launchApp(context, app)
                                    viewModel.toggleDrawer(false)
                                },
                                onLongClick = { viewModel.openAppMenu(app) }
                            )
                        }
                    }
                }
            }
        }

        // Long Press Context Menu
        selectedAppForMenu?.let { app ->
            GlassContextMenu(
                app = app,
                isPinnedToDock = dockApps.any { it.packageName == app.packageName },
                onDismiss = { viewModel.openAppMenu(null) },
                onLaunch = {
                    viewModel.launchApp(context, app)
                    viewModel.openAppMenu(null)
                },
                onTogglePin = { viewModel.togglePinDock(app) },
                onAppInfo = { viewModel.openAppInfo(context, app) },
                onUninstall = { viewModel.uninstallApp(context, app) }
            )
        }

        // Settings Dialog
        if (isSettingsOpen) {
            GlassSettingsDialog(
                currentStyle = currentStyle,
                showLabels = showLabels,
                gridColumns = gridColumns,
                onDismiss = { viewModel.toggleSettings(false) },
                onStyleSelected = { viewModel.setGlassStyle(it) },
                onToggleLabels = { viewModel.toggleLabels() },
                onColumnsSelected = { viewModel.setGridColumns(it) },
                onSetDefaultLauncher = {
                    viewModel.openDefaultHomeSettings(context)
                    viewModel.toggleSettings(false)
                }
            )
        }
    }
}
