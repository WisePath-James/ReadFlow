package com.readflow.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var darkMode by remember { mutableStateOf(false) }
    var sepiaMode by remember { mutableStateOf(false) }
    var defaultZoom by remember { mutableFloatStateOf(1f) }
    var defaultPenColor by remember { mutableStateOf("#000000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SwitchSettingItem(
                    title = "Dark Mode",
                    description = "Use dark theme",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
                SwitchSettingItem(
                    title = "Sepia Mode",
                    description = "Use sepia theme for reading",
                    checked = sepiaMode,
                    onCheckedChange = { sepiaMode = it }
                )
            }

            // Reading Section
            SettingsSection(title = "Reading") {
                SliderSettingItem(
                    title = "Default Zoom",
                    value = defaultZoom,
                    valueRange = 0.5f..3f,
                    onValueChange = { defaultZoom = it },
                    valueLabel = "${(defaultZoom * 100).toInt()}%"
                )
            }

            // Handwriting Section (Tablet)
            SettingsSection(title = "Handwriting") {
                ColorSettingItem(
                    title = "Default Pen Color",
                    selectedColor = defaultPenColor,
                    colors = listOf("#000000", "#1565C0", "#C62828", "#2E7D32", "#F57C00", "#7B1FA2"),
                    onColorSelect = { defaultPenColor = it }
                )
            }

            // Storage Section
            SettingsSection(title = "Storage") {
                ClickableSettingItem(
                    title = "Clear Cache",
                    description = "Free up space by clearing cached data",
                    onClick = { /* Clear cache */ }
                )
            }

            // About Section
            SettingsSection(title = "About") {
                InfoSettingItem(
                    title = "Version",
                    value = "1.0.0"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun ColorSettingItem(
    title: String,
    selectedColor: String,
    colors: List<String>,
    onColorSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { color ->
                Surface(
                    onClick = { onColorSelect(color) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(color)),
                    border = if (selectedColor == color) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                    modifier = Modifier.size(36.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun ClickableSettingItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoSettingItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
