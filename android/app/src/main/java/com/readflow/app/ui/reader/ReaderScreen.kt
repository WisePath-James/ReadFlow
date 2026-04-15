package com.readflow.app.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalScrollGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.readflow.app.domain.model.Annotation
import com.readflow.app.domain.model.AnnotationType
import com.readflow.app.domain.model.Document
import com.readflow.app.domain.model.ReadingMode
import com.readflow.app.domain.model.ReadingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val scope = rememberCoroutineScope()

    var showAnnotationMenu by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var selectionPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    Scaffold(
        topBar = {
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
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.List, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Annotations") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Bookmark, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Search") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Search, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                        // Page content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { offset ->
                                            // Simulate text selection
                                            selectedText = "Selected text sample"
                                            selectionPageIndex = uiState.currentPage
                                            showAnnotationMenu = true
                                        },
                                        onDoubleTap = {
                                            // Zoom toggle
                                            viewModel.toggleZoom()
                                        }
                                    )
                                }
                        ) {
                            // PDF Page content
                            PdfPageView(
                                document = uiState.document!!,
                                pageIndex = uiState.currentPage,
                                zoomLevel = uiState.zoomLevel,
                                theme = uiState.theme,
                                annotations = uiState.annotations,
                                onPageChange = { viewModel.goToPage(it) },
                                onAnnotationClick = { annotation ->
                                    // Handle annotation click
                                }
                            )
                        }

                        // Bottom toolbar
                        BottomToolbar(
                            currentPage = uiState.currentPage,
                            pageCount = uiState.pageCount,
                            onPageChange = { viewModel.goToPage(it) },
                            onPreviousPage = { viewModel.previousPage() },
                            onNextPage = { viewModel.nextPage() },
                            onAiClick = {
                                onAiClick(documentId, uiState.currentPage, selectedText)
                            }
                        )
                    }
                }
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
                // Copy to clipboard
                showAnnotationMenu = false
            },
            onShare = {
                // Share text
                showAnnotationMenu = false
            }
        )
    }
}

@Composable
fun PdfPageView(
    document: Document,
    pageIndex: Int,
    zoomLevel: Float,
    theme: ReadingTheme,
    annotations: List<Annotation>,
    onPageChange: (Int) -> Unit,
    onAnnotationClick: (Annotation) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val backgroundColor = when (theme) {
        ReadingTheme.LIGHT -> Color.White
        ReadingTheme.DARK -> Color(0xFF1C1B1F)
        ReadingTheme.SEPIA -> Color(0xFFF4ECD8)
    }

    val textColor = when (theme) {
        ReadingTheme.LIGHT -> Color.Black
        ReadingTheme.DARK -> Color(0xFFE6E1E5)
        ReadingTheme.SEPIA -> Color(0xFF5B4636)
    }

    LaunchedEffect(pageIndex, document) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // Load PDF page using PdfRenderer
                val file = File(document.filePath)
                if (file.exists()) {
                    val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    if (pageIndex < renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val width = (page.width * 2).coerceAtMost(1200)
                            val height = (page.height * 2).coerceAtMost(1600)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageBitmap = bitmap
                        }
                    }
                    fd.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .graphicsLayer {
                scaleX = zoomLevel
                scaleY = zoomLevel
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
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
                    text = "This is a demo page.\nIn production, this would display the actual PDF content using Android's native PdfRenderer API.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        // Annotation overlays would be drawn here
        annotations
            .filter { it.pageIndex == pageIndex }
            .forEach { annotation ->
                AnnotationOverlay(
                    annotation = annotation,
                    onClick = { onAnnotationClick(annotation) }
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
    onAiClick: () -> Unit
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
                ActionButton(
                    icon = Icons.Default.LibraryBooks,
                    label = "TOC",
                    onClick = {}
                )
                ActionButton(
                    icon = Icons.Default.Search,
                    label = "Search",
                    onClick = {}
                )
                ActionButton(
                    icon = Icons.Default.Translate,
                    label = "AI",
                    onClick = onAiClick,
                    isPrimary = true
                )
                ActionButton(
                    icon = Icons.Default.Bookmark,
                    label = "Notes",
                    onClick = {}
                )
                ActionButton(
                    icon = Icons.Default.Info,
                    label = "Info",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

            // Annotation tools
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

            // More actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip(
                    icon = Icons.Default.FormatUnderlined,
                    label = "Underline",
                    modifier = Modifier.weight(1f),
                    onClick = { onUnderline("#03A9F4") }
                )
                ActionChip(
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
                ActionChip(
                    icon = Icons.Default.Translate,
                    label = "Translate",
                    modifier = Modifier.weight(1f),
                    onClick = onAiTranslate
                )
                ActionChip(
                    icon = Icons.Default.Lightbulb,
                    label = "Explain",
                    modifier = Modifier.weight(1f),
                    onClick = onAiExplain
                )
                ActionChip(
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
                ActionChip(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    modifier = Modifier.weight(1f),
                    onClick = onCopy
                )
                ActionChip(
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
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
