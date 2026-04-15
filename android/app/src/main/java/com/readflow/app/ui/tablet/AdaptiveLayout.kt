package com.readflow.app.ui.tablet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.readflow.app.ui.library.LibraryScreen
import com.readflow.app.ui.reader.ReaderScreen

/**
 * 平板适配主容器
 * 根据屏幕尺寸和方向切换不同的布局策略
 */
@Composable
fun AdaptiveLayout(
    windowSizeClass: WindowSizeClass,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpandedScreen) {
        // Tablet/Large screen: Two-pane layout
        TabletLayout(
            currentRoute = currentRoute,
            onNavigate = onNavigate
        )
    } else {
        // Phone: Single-pane layout
        content()
    }
}

@Composable
fun TabletLayout(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf(
        NavigationItem("Library", Icons.Default.LibraryBooks, "library"),
        NavigationItem("Archive", Icons.Default.Archive, "archive"),
        NavigationItem("Notes", Icons.Default.StickyNote2, "notes"),
        NavigationItem("Settings", Icons.Default.Settings, "settings")
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail for large screens
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Spacer(Modifier.weight(1f))
            items.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = selectedItem == index,
                    onClick = {
                        selectedItem = index
                        onNavigate(item.route)
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) }
                )
            }
            Spacer(Modifier.weight(1f))
        }

        // Main content
        when (currentRoute) {
            "library" -> {
                LibraryScreen(
                    onDocumentClick = { docId -> onNavigate("reader/$docId") },
                    onSettingsClick = { onNavigate("settings") }
                )
            }
            "reader" -> {
                // Reader with side panel
                TabletReaderLayout()
            }
            else -> {
                LibraryScreen(
                    onDocumentClick = { docId -> onNavigate("reader/$docId") },
                    onSettingsClick = { onNavigate("settings") }
                )
            }
        }
    }
}

@Composable
fun TabletReaderLayout() {
    // This would be the reader with side panels for tablet
    // Including: TOC, AI panel, Notes, Annotations side by side
    Row(modifier = Modifier.fillMaxSize()) {
        // Table of Contents sidebar
        Surface(
            modifier = Modifier.width(280.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // TOC items would go here
            }
        }

        // Main reader content
        Box(modifier = Modifier.weight(1f)) {
            // Reader content
        }

        // AI Panel sidebar
        Surface(
            modifier = Modifier.width(360.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // AI panel content
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
