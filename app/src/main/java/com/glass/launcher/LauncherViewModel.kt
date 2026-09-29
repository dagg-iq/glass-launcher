package com.glass.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allApps = _allApps.asStateFlow()

    private val _dockApps = MutableStateFlow<List<AppItem>>(emptyList())
    val dockApps = _dockApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen = _isDrawerOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen = _isSettingsOpen.asStateFlow()

    private val _currentStyle = MutableStateFlow(GlassThemeStyle.LIQUID_FROSTED)
    val currentStyle = _currentStyle.asStateFlow()

    private val _showLabels = MutableStateFlow(true)
    val showLabels = _showLabels.asStateFlow()

    private val _gridColumns = MutableStateFlow(4)
    val gridColumns = _gridColumns.asStateFlow()

    private val _selectedAppForMenu = MutableStateFlow<AppItem?>(null)
    val selectedAppForMenu = _selectedAppForMenu.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                queryLaunchableApps()
            }
            _allApps.value = apps
            setupInitialDock(apps)
            _isLoading.value = false
        }
    }

    private fun queryLaunchableApps(): List<AppItem> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList = pm.queryIntentActivities(intent, 0)
        val myPackage = getApplication<Application>().packageName

        return resolveList
            .filter { it.activityInfo.packageName != myPackage }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val act = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = try { resolveInfo.loadIcon(pm) } catch (_: Exception) { null }
                AppItem(
                    packageName = pkg,
                    activityName = act,
                    label = label,
                    iconDrawable = icon
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun setupInitialDock(apps: List<AppItem>) {
        // Preferred dock apps: Phone, WhatsApp, Chrome, Camera, Messages
        val dockPriority = listOf(
            "dialer", "phone", "telecom",
            "whatsapp",
            "chrome", "browser",
            "camera",
            "mms", "messaging", "message"
        )
        val selected = mutableListOf<AppItem>()

        for (key in dockPriority) {
            val match = apps.firstOrNull { app ->
                (app.packageName.contains(key, ignoreCase = true) || app.label.contains(key, ignoreCase = true)) &&
                        !selected.contains(app)
            }
            if (match != null) {
                selected.add(match)
            }
            if (selected.size >= 5) break
        }

        // Fallback to first few apps if none matched
        if (selected.isEmpty() && apps.isNotEmpty()) {
            selected.addAll(apps.take(4))
        }

        _dockApps.value = selected
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDrawer(open: Boolean? = null) {
        _isDrawerOpen.value = open ?: !_isDrawerOpen.value
    }

    fun toggleSettings(open: Boolean? = null) {
        _isSettingsOpen.value = open ?: !_isSettingsOpen.value
    }

    fun setGlassStyle(style: GlassThemeStyle) {
        _currentStyle.value = style
        LiquidGlassIconEngine.clearCache()
    }

    fun toggleLabels() {
        _showLabels.value = !_showLabels.value
    }

    fun setGridColumns(cols: Int) {
        _gridColumns.value = cols
    }

    fun openAppMenu(app: AppItem?) {
        _selectedAppForMenu.value = app
    }

    fun togglePinDock(app: AppItem) {
        val current = _dockApps.value.toMutableList()
        if (current.any { it.packageName == app.packageName }) {
            current.removeAll { it.packageName == app.packageName }
        } else {
            if (current.size < 5) {
                current.add(app)
            }
        }
        _dockApps.value = current
        _selectedAppForMenu.value = null
    }

    fun launchApp(context: Context, app: AppItem) {
        try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    fun openAppInfo(context: Context, app: AppItem) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
        _selectedAppForMenu.value = null
    }

    fun uninstallApp(context: Context, app: AppItem) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
        _selectedAppForMenu.value = null
    }

    fun openDefaultHomeSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
