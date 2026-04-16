package com.readflow.app.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readflow.app.domain.model.Annotation
import com.readflow.app.domain.model.AnnotationType
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.ReadingMode
import com.readflow.app.domain.model.ReadingTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    documentId: String,
    onBack: () -> Unit,
    onAiClick: (String, Int, String) -> Unit = { _, _, _ -> },
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showAnnotationMenu by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var selectionPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    Scaffold(
        topBar = {
            if (uiState.isSearching) {
                SearchTopBar(
                    query = uiState.searchQuery,
                    resultCount = uiState.searchResults.size,
                    currentIndex = uiState.currentSearchIndex + 1,
                    onQueryChange = { viewModel.search(it) },
                    onNext = { viewModel.nextSearchResult() },
                    onPrevious = { viewModel.previousSearchResult() },
                    onClose = { viewModel.dismissSearch() }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.document?.title ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.document != null) {
                                Text(
                                    text = "Page ${uiState.currentPage + 1} of ${uiState.pageCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Reading mode toggle
                        IconButton(onClick = { viewModel.toggleReadingMode() }) {
                            Icon(
                                if (uiState.readingMode == ReadingMode.CONTINUOUS)
                                    Icons.Default.ViewDay
                                else
                                    Icons.Default.ViewWeek,
                                contentDescription = "Toggle view mode"
                            )
                        }

                        // Theme toggle
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(Icons.Default.Brightness6, contentDescription = "Theme")
                        }

                        // More menu
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Table of Contents") },
                                onClick = { 
                                    showMenu = false
                                    viewModel.toggleToc()
                                },
                                leadingIcon = { Icon(Icons.Default.List, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Search") },
                                onClick = { 
                                    showMenu = false
                                    viewModel.startSearch()
                                },
                                leadingIcon = { Icon(Icons.Default.Search, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Annotations") },
                                onClick = { showMenu = false },
                                leadingIcon = { Icon(Icons.Default.Bookmark, null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error ?: "Error loading document")
                        Button(onClick = { viewModel.loadDocument(documentId) }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.document != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Main content area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // PDF Page content
                            PdfPageView(
                                document = uiState.document!!,
                                pageIndex = uiState.currentPage,
                                zoomLevel = uiState.zoomLevel,
                                theme = uiState.theme,
                                annotations = uiState.annotations,
                                onLongPress = { offset ->
                                    selectedText = "Selected text sample"
                                    selectionPageIndex = uiState.currentPage
                                    showAnnotationMenu = true
                                },
                                onPageChange = { viewModel.goToPage(it) }
                            )
                        }

                        // Bottom toolbar
                        BottomToolbar(
                            currentPage = uiState.currentPage,
                            pageCount = uiState.pageCount,
                            onPageChange = { viewModel.goToPage(it) },
                            onPreviousPage = { viewModel.previousPage() },
                            onNextPage = { viewModel.nextPage() },
                            onTocClick = { viewModel.toggleToc() },
                            onSearchClick = { viewModel.startSearch() },
                            onAiClick = {
                                onAiClick(documentId, uiState.currentPage, selectedText)
                            },
                            onNotesClick = { }
                        )
                    }
                }
            }

            // Table of Contents Panel
            if (uiState.showToc) {
                TableOfContentsPanel(
                    tocItems = uiState.tableOfContents,
                    currentPage = uiState.currentPage,
                    onItemClick = { tocItem ->
                        viewModel.goToPage(tocItem.pageIndex)
                        viewModel.toggleToc()
                    },
                    onClose = { viewModel.toggleToc() }
                )
            }
        }
    }

    // Annotation Menu (Selection Action Sheet)
    if (showAnnotationMenu) {
        SelectionActionSheet(
            selectedText = selectedText,
            onDismiss = {
                showAnnotationMenu = false
                selectedText = ""
            },
            onHighlight = { color ->
                viewModel.addAnnotation(
                    documentId = documentId,
                    pageIndex = selectionPageIndex,
                    type = AnnotationType.HIGHLIGHT,
                    color = color,
                    quote = selectedText
                )
                showAnnotationMenu = false
            },
            onUnderline = { color ->
                viewModel.addAnnotation(
                    documentId = documentId,
                    pageIndex = selectionPageIndex,
                    type = AnnotationType.UNDERLINE,
                    color = color,
                    quote = selectedText
                )
                showAnnotationMenu = false
            },
            onNote = {
                viewModel.addNote(
                    documentId = documentId,
                    pageIndex = selectionPageIndex,
                    quote = selectedText,
                    noteText = ""
                )
                showAnnotationMenu = false
            },
            onAiTranslate = {
                onAiClick(documentId, selectionPageIndex, selectedText)
                showAnnotationMenu = false
            },
            onAiExplain = {
                onAiClick(documentId, selectionPageIndex, selectedText)
                showAnnotationMenu = false
            },
            onAiSummarize = {
                onAiClick(documentId, selectionPageIndex, selectedText)
                showAnnotationMenu = false
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(selectedText))
                showAnnotationMenu = false
            },
            onShare = {
                showAnnotationMenu = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    resultCount: Int,
    currentIndex: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    "Search in document...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close search")
            }
        },
        actions = {
            if (resultCount > 0) {
                Text(
                    "$currentIndex / $resultCount",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            IconButton(onClick = onPrevious, enabled = resultCount > 0) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
            }
            IconButton(onClick = onNext, enabled = resultCount > 0) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
            }
        }
    )
}

@Composable
fun TableOfContentsPanel(
    tocItems: List<TocItem>,
    currentPage: Int,
    onItemClick: (TocItem) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Table of Contents",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Divider()
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(tocItems) { index, item ->
                    val isCurrentPage = item.pageIndex == currentPage
                    ListItem(
                        headlineContent = {
                            Text(
                                item.title,
                                fontWeight = if (isCurrentPage) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item) }
                            .background(
                                if (isCurrentPage) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            ),
                        leadingContent = {
                            if (item.level > 0) {
                                Spacer(modifier = Modifier.width((item.level * 16).dp))
                            }
                            Text(
                                "${item.pageIndex + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PdfPageView(
    document: Document,
    pageIndex: Int,
    zoomLevel: Float,
    theme: ReadingTheme,
    annotations: List<Annotation>,
    onLongPress: (androidx.compose.ui.geometry.Offset) -> Unit,
    onPageChange: (Int) -> Unit
) {
    val context = LocalContext.current

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val backgroundColor = when (theme) {
        ReadingTheme.LIGHT -> Color.White
        ReadingTheme.DARK -> Color(0xFF1C1B1F)
        ReadingTheme.SEPIA -> Color(0xFFF4ECD8)
    }

    val textColor = when (theme) {
        ReadingTheme.LIGHT -> Color.Black
        ReadingTheme.DARK -> Color.White
        ReadingTheme.SEPIA -> Color(0xFF5D4037)
    }

    LaunchedEffect(pageIndex, document.filePath) {
        isLoading = true
        try {
            // Try to render the PDF page
            val file = File(document.filePath)
            if (file.exists()) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                if (pageIndex < renderer.pageCount) {
                    renderer.openPage(pageIndex).use { page ->
                        val width = (page.width * zoomLevel).toInt().coerceAtLeast(1)
                        val height = (page.height * zoomLevel).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmap = bitmap
                    }
                }
                renderer.close()
                fd.close()
            }
        } catch (e: Exception) {
            // Ignore errors, show demo content
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        onLongPress(offset)
                    },
                    onDoubleTap = {
                        // Toggle zoom
                    }
                )
            }
            .verticalScroll(rememberScrollState())
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (pageBitmap != null) {
            Image(
                bitmap = pageBitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        } else {
            // Demo mode - show placeholder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "This is a demo page.\nIn production, this would display the actual PDF content.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        // Annotation overlays
        annotations
            .filter { it.pageIndex == pageIndex }
            .forEach { annotation ->
                AnnotationOverlay(
                    annotation = annotation,
                    onClick = { }
                )
            }
    }
}

@Composable
fun AnnotationOverlay(
    annotation: Annotation,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(annotation.color))
    } catch (e: Exception) {
        Color.Yellow
    }

    val backgroundColor = when (annotation.type) {
        AnnotationType.HIGHLIGHT -> color.copy(alpha = 0.3f)
        AnnotationType.UNDERLINE -> Color.Transparent
        AnnotationType.STRIKEOUT -> color.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = annotation.quote,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun BottomToolbar(
    currentPage: Int,
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onTocClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAiClick: () -> Unit,
    onNotesClick: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Page slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }

                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onPageChange(it.toInt()) },
                    valueRange = 0f..(pageCount - 1).coerceAtLeast(0).toFloat(),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onNextPage,
                    enabled = currentPage < pageCount - 1
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }

                Text(
                    text = "${currentPage + 1}/$pageCount",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(60.dp)
                )
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolbarActionButton(
                    icon = Icons.Default.List,
                    label = "TOC",
                    onClick = onTocClick
                )
                ToolbarActionButton(
                    icon = Icons.Default.Search,
                    label = "Search",
                    onClick = onSearchClick
                )
                ToolbarActionButton(
                    icon = Icons.Default.Translate,
                    label = "AI",
                    onClick = onAiClick,
                    isPrimary = true
                )
                ToolbarActionButton(
                    icon = Icons.Default.StickyNote2,
                    label = "Notes",
                    onClick = onNotesClick
                )
                ToolbarActionButton(
                    icon = Icons.Default.Info,
                    label = "Info",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun ToolbarActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionActionSheet(
    selectedText: String,
    onDismiss: () -> Unit,
    onHighlight: (String) -> Unit,
    onUnderline: (String) -> Unit,
    onNote: () -> Unit,
    onAiTranslate: () -> Unit,
    onAiExplain: () -> Unit,
    onAiSummarize: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val highlightColors = listOf("#FFEB3B", "#8BC34A", "#03A9F4", "#E91E63", "#FF9800")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            // Selected text preview
            if (selectedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = selectedText.take(100) + if (selectedText.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Highlight colors
            Text(
                text = "Highlight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                highlightColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(android.graphics.Color.parseColor(color)))
                            .clickable { onHighlight(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Annotation actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChipButton(
                    icon = Icons.Default.FormatUnderlined,
                    label = "Underline",
                    modifier = Modifier.weight(1f),
                    onClick = { onUnderline("#03A9F4") }
                )
                ActionChipButton(
                    icon = Icons.Default.StickyNote2,
                    label = "Note",
                    modifier = Modifier.weight(1f),
                    onClick = onNote
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI actions
            Text(
                text = "AI Actions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChipButton(
                    icon = Icons.Default.Translate,
                    label = "Translate",
                    modifier = Modifier.weight(1f),
                    onClick = onAiTranslate
                )
                ActionChipButton(
                    icon = Icons.Default.Lightbulb,
                    label = "Explain",
                    modifier = Modifier.weight(1f),
                    onClick = onAiExplain
                )
                ActionChipButton(
                    icon = Icons.Default.Summarize,
                    label = "Summarize",
                    modifier = Modifier.weight(1f),
                    onClick = onAiSummarize
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copy & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChipButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    modifier = Modifier.weight(1f),
                    onClick = onCopy
                )
                ActionChipButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    modifier = Modifier.weight(1f),
                    onClick = onShare
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActionChipButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
