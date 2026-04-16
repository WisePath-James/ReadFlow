package com.readflow.app.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.FileType
import com.readflow.app.domain.model.Folder
import com.readflow.app.domain.model.ProcessingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onDocumentClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDocumentMenu by remember { mutableStateOf<String?>(null) }
    var documentToDelete by remember { mutableStateOf<Document?>(null) }
    var documentToMove by remember { mutableStateOf<Document?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderForMove by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // File picker launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "document.pdf"

            // Copy to app storage
            val destFile = java.io.File(context.filesDir, "documents/$fileName")
            destFile.parentFile?.mkdirs()
            inputStream?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            viewModel.importDocument(
                filePath = destFile.absolutePath,
                fileName = fileName,
                fileType = FileType.PDF
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pdfLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "text/plain", "text/markdown"))
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "上传文档")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar
            if (showSearchBar) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.search(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索文档...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.search("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Upload progress
            if (uiState.isUploading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("导入文档中...", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = uiState.uploadProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Continue Reading
            if (uiState.recentDocuments.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    SectionHeader(title = "Continue Reading", icon = Icons.Default.MenuBook)
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recentDocuments) { document ->
                            DocumentCard(
                                document = document,
                                onClick = { onDocumentClick(document.id) },
                                onLongClick = { showDocumentMenu = document.id }
                            )
                        }
                    }
                }
            }

            // Pinned Documents
            if (uiState.pinnedDocuments.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    SectionHeader(title = "Pinned", icon = Icons.Default.PushPin)
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.pinnedDocuments) { document ->
                            DocumentCard(
                                document = document,
                                onClick = { onDocumentClick(document.id) },
                                onLongClick = { showDocumentMenu = document.id },
                                isPinned = true
                            )
                        }
                    }
                }
            }

            // Folders
            if (searchQuery.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "Folders", icon = Icons.Default.Folder)
                        TextButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New")
                        }
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Root folder
                        item {
                            FolderChip(
                                folder = null,
                                onClick = { viewModel.selectFolder(null) },
                                isSelected = uiState.selectedFolderId == null
                            )
                        }

                        items(uiState.folders) { folder ->
                            FolderChip(
                                folder = folder,
                                onClick = { viewModel.selectFolder(folder.id) },
                                isSelected = uiState.selectedFolderId == folder.id
                            )
                        }
                    }
                }

                // Documents
                item {
                    SectionHeader(
                        title = if (uiState.selectedFolderId == null) "All Documents" else "Documents",
                        icon = Icons.Default.Description
                    )
                }

                if (uiState.documents.isEmpty()) {
                    item {
                        EmptyState(onImportClick = {
                            pdfLauncher.launch(arrayOf("application/pdf"))
                        })
                    }
                } else {
                    items(uiState.documents) { document ->
                        DocumentListItem(
                            document = document,
                            onClick = { onDocumentClick(document.id) },
                            onDelete = {
                                documentToDelete = document
                                showDeleteConfirmDialog = true
                            },
                            onMove = {
                                documentToMove = document
                            },
                            onTogglePin = { viewModel.togglePin(document.id) }
                        )
                    }
                }
            } else {
                // Search results
                item {
                    SectionHeader(
                        title = "Search Results (${uiState.searchResults.size})",
                        icon = Icons.Default.Search
                    )
                }

                if (uiState.searchResults.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No documents found",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.searchResults) { document ->
                        DocumentListItem(
                            document = document,
                            onClick = { onDocumentClick(document.id) }
                        )
                    }
                }
            }
        }
    }

    // Create folder dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName)
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && documentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                documentToDelete = null
            },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete \"${documentToDelete?.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        documentToDelete?.let { viewModel.deleteDocument(it.id) }
                        showDeleteConfirmDialog = false
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    documentToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move document dialog
    if (documentToMove != null) {
        AlertDialog(
            onDismissRequest = { documentToMove = null },
            title = { Text("Move to Folder") },
            text = {
                Column {
                    Text("Select destination folder:")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Root option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.moveDocument(documentToMove!!.id, null)
                                documentToMove = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Root (No Folder)")
                    }
                    
                    uiState.folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveDocument(documentToMove!!.id, folder.id)
                                    documentToMove = null
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = try {
                                Color(android.graphics.Color.parseColor(folder.color))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(folder.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { documentToMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderChip(
    folder: Folder?,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(if (folder == null) "All" else folder.name)
        },
        leadingIcon = {
            Icon(
                if (folder == null) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isPinned: Boolean = false
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        when (document.processingStatus) {
                            ProcessingStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            ProcessingStatus.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (document.processingStatus) {
                    ProcessingStatus.COMPLETED -> {
                        Icon(
                            when (document.fileType) {
                                FileType.PDF -> Icons.Default.PictureAsPdf
                                FileType.EPUB -> Icons.Default.Book
                                FileType.MARKDOWN -> Icons.Default.Description
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    ProcessingStatus.PROCESSING -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                    else -> {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${document.pageCount} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DocumentListItem(
    document: Document,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onMove: () -> Unit = {},
    onTogglePin: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Icon(
                when (document.fileType) {
                    FileType.PDF -> Icons.Default.PictureAsPdf
                    FileType.EPUB -> Icons.Default.Book
                    FileType.MARKDOWN -> Icons.Default.Description
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = when (document.fileType) {
                    FileType.PDF -> Color(0xFFE53935)
                    FileType.EPUB -> Color(0xFF43A047)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Document info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    document.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${document.pageCount} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    when (document.processingStatus) {
                        ProcessingStatus.PROCESSING -> {
                            Text(
                                "Processing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        ProcessingStatus.FAILED -> {
                            Text(
                                "Failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            }

            // Actions
            if (document.isPinned) {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Unpin",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (document.isPinned) "Unpin" else "Pin") },
                        onClick = {
                            onTogglePin()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (document.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move") },
                        onClick = {
                            onMove()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(onImportClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No documents yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Import your first document to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onImportClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Document")
            }
        }
    }
}
