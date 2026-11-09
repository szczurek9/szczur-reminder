package com.szczurek9.reminder.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val ROUTE_LIST = "list"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_DETAIL = "detail/{id}"
private const val ROUTE_EDITOR = "editor/{id}"

@Composable
fun AppRoot(openId: Long, onOpenConsumed: () -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val remindersVm: RemindersViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    // Android 13+: uprawnienie do powiadomień nie jest przyznane automatycznie – prosimy raz przy starcie.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(openId) {
        if (openId >= 0) {
            nav.navigate("detail/$openId") { launchSingleTop = true }
            onOpenConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            if (route == ROUTE_LIST || route == ROUTE_SETTINGS) {
                BottomBar(
                    route = route,
                    onList = { if (route != ROUTE_LIST) nav.navigate(ROUTE_LIST) { popUpTo(ROUTE_LIST) { inclusive = true } } },
                    onAdd = { nav.navigate("editor/-1") },
                    onSettings = { if (route != ROUTE_SETTINGS) nav.navigate(ROUTE_SETTINGS) { launchSingleTop = true } },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = ROUTE_LIST, modifier = Modifier.padding(padding)) {
            composable(ROUTE_LIST) {
                ReminderListScreen(remindersVm, onOpen = { nav.navigate("detail/$it") }, onEdit = { nav.navigate("editor/$it") })
            }
            composable(ROUTE_SETTINGS) { SettingsScreen(settingsVm) }
            composable(ROUTE_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ReminderDetailScreen(
                    id = entry.arguments?.getLong("id") ?: -1L,
                    vm = remindersVm,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("editor/$it") },
                )
            }
            composable(ROUTE_EDITOR, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ReminderEditorScreen(
                    id = (entry.arguments?.getLong("id") ?: -1L).takeIf { it >= 0 },
                    vm = remindersVm,
                    onClose = { nav.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(route: String?, onList: () -> Unit, onAdd: () -> Unit, onSettings: () -> Unit) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(76.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BarItem(Icons.Filled.Notifications, "Przypomnienia", route == ROUTE_LIST, onList, Modifier.weight(1f))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(
                    onClick = onAdd,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.size(60.dp),
                ) { Icon(Icons.Filled.Add, contentDescription = "Dodaj przypomnienie", modifier = Modifier.size(30.dp)) }
            }
            BarItem(Icons.Filled.Settings, "Ustawienia", route == ROUTE_SETTINGS, onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BarItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "barItemTint",
    )
    Column(
        modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, color = tint, style = MaterialTheme.typography.labelMedium)
    }
}
