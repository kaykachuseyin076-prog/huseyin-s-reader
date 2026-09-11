package com.example.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.EditNote
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.NotebookEntity
import com.example.ui.notebook.NotebooksListScreen
import com.example.ui.theme.HighDensityBorderChip
import com.example.ui.theme.HighDensityIconGray
import com.example.ui.theme.HighDensityOnPrimaryContainer
import com.example.ui.theme.HighDensityPrimaryContainer
import com.example.ui.theme.HighDensityPrimaryNavy
import com.example.ui.theme.HighDensitySurfaceVariantLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookSelected: (BookEntity) -> Unit,
    onNotebookSelected: (NotebookEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSettingsSheetOpen by remember { mutableStateOf(false) }
    var isNotesTabActive by remember { mutableStateOf(false) }

    // SAF Directory Picker Launcher (Optional manual folder import)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.scanFolder(uri)
        }
    }

    // SAF Document Picker Launcher (PDF / EPUB files)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            viewModel.addSingleDocument(uri)
        }
    }

    // Runtime Storage Permission Launcher (Android 10 and below)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.scanDeviceStorage(isInitial = true)
    }

    // Manage Storage Launcher (Android 11+)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                viewModel.scanDeviceStorage(isInitial = true)
            }
        }
    }

    // Automatic scan trigger on initial composition
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            } else {
                viewModel.scanDeviceStorage(isInitial = true)
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                viewModel.scanDeviceStorage(isInitial = true)
            }
        }
    }

    // Handle messages
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // High Density Header (px-4 pt-4 pb-2 flex items-center justify-between)
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Reader",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.4).sp,
                                    fontSize = 24.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (state.totalBooksCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(HighDensityPrimaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${state.totalBooksCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = HighDensityOnPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) {
                                    viewModel.onSearchQueryChanged("")
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .testTag("search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Kitap Ara",
                                tint = HighDensityIconGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Add Single File Button (SAF)
                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf("application/pdf", "application/epub+zip", "*/*")
                                )
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .testTag("add_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Kitap Dosyası Ekle",
                                tint = HighDensityIconGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Optional: Add Folder Button (SAF Directory)
                        IconButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .testTag("add_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Klasörden İçe Aktar",
                                tint = HighDensityIconGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Refresh / Rescan Device Button
                        IconButton(
                            onClick = { viewModel.scanDeviceStorage(isInitial = false) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .testTag("refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Cihazı Yeniden Tara",
                                tint = HighDensityIconGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Settings Button (Gemini AI Ayarları)
                        IconButton(
                            onClick = { isSettingsSheetOpen = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .testTag("library_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = HighDensityIconGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Search Bar (Expanded)
                AnimatedVisibility(visible = isSearchExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Kitap adı veya yazar ara...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = HighDensityIconGray)
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Temizle")
                                    }
                                }
                            }
                        )
                    }
                }

                // High Density Filter Chips (visible only in Books tab)
                AnimatedVisibility(visible = !isNotesTabActive) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            HighDensityChip(
                                label = "Tümü",
                                isSelected = state.selectedFilter == BookFilter.ALL,
                                onClick = { viewModel.onFilterSelected(BookFilter.ALL) }
                            )
                        }
                        item {
                            HighDensityChip(
                                label = "PDF",
                                isSelected = state.selectedFilter == BookFilter.PDF,
                                onClick = { viewModel.onFilterSelected(BookFilter.PDF) }
                            )
                        }
                        item {
                            HighDensityChip(
                                label = "EPUB",
                                isSelected = state.selectedFilter == BookFilter.EPUB,
                                onClick = { viewModel.onFilterSelected(BookFilter.EPUB) }
                            )
                        }
                        item {
                            HighDensityChip(
                                label = "Son Okunanlar",
                                isSelected = state.selectedFilter == BookFilter.READING,
                                onClick = { viewModel.onFilterSelected(BookFilter.READING) }
                            )
                        }
                    }
                }

                // Non-intrusive Scanning Banner
                AnimatedVisibility(visible = state.isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighDensityPrimaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = HighDensityPrimaryNavy
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.scanProgressText ?: "Cihaz belleği taranıyor...",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = HighDensityPrimaryNavy,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Loading Bar
                if (state.isLoading || state.isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            HighDensityBottomNavigation(
                activeFilter = state.selectedFilter,
                isNotesActive = isNotesTabActive,
                onLibraryClick = {
                    isNotesTabActive = false
                    viewModel.onFilterSelected(BookFilter.ALL)
                },
                onRecentClick = {
                    isNotesTabActive = false
                    viewModel.onFilterSelected(BookFilter.READING)
                },
                onNotesClick = {
                    isNotesTabActive = true
                },
                onScanDeviceClick = {
                    isNotesTabActive = false
                    viewModel.scanDeviceStorage(isInitial = false)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isNotesTabActive) {
                NotebooksListScreen(
                    onNotebookSelected = onNotebookSelected,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (state.books.isEmpty() && !state.isLoading) {
                EmptyLibraryView(
                    isSearching = state.searchQuery.isNotBlank(),
                    isScanning = state.isScanning,
                    onRescanDevice = { viewModel.scanDeviceStorage(isInitial = false) },
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onSelectFile = {
                        filePickerLauncher.launch(
                            arrayOf("application/pdf", "application/epub+zip", "*/*")
                        )
                    },
                    onLoadSamples = { viewModel.loadSampleBooks() }
                )
            } else {
                // High Density Grid (compact, 136dp min width, 12dp spacing)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 136.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("books_grid")
                ) {
                    items(
                        items = state.books,
                        key = { it.id }
                    ) { book ->
                        BookCard(
                            book = book,
                            onClick = { onBookSelected(book) }
                        )
                    }
                }
            }
        }
    }

    if (isSettingsSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        com.example.ui.reader.ai.GeminiSettingsSheet(
            sheetState = sheetState,
            onDismiss = { isSettingsSheetOpen = false }
        )
    }
}

/**
 * High Density Filter Chip
 * Matches Design HTML:
 * Active: px-4 py-1.5 bg-[#D3E3FD] text-[#001C35] rounded-full text-sm font-medium
 * Inactive: px-4 py-1.5 bg-[#F1F3F8] text-[#44474E] rounded-full text-sm font-medium border border-[#C4C7CF]
 */
@Composable
fun HighDensityChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) HighDensityPrimaryContainer else HighDensitySurfaceVariantLight
    val textColor = if (isSelected) HighDensityOnPrimaryContainer else HighDensityIconGray
    val borderModifier = if (isSelected) {
        Modifier
    } else {
        Modifier.border(1.dp, HighDensityBorderChip, CircleShape)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(borderModifier)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * High Density Bottom Navigation Bar
 * Matches Design HTML:
 * h-[72px] bg-[#F1F3F8] border-t border-[#E1E2E6] flex items-center justify-around px-6 pb-2
 */
@Composable
fun HighDensityBottomNavigation(
    activeFilter: BookFilter,
    isNotesActive: Boolean,
    onLibraryClick: () -> Unit,
    onRecentClick: () -> Unit,
    onNotesClick: () -> Unit,
    onScanDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HighDensitySurfaceVariantLight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline
            )
            .navigationBarsPadding()
            .height(68.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Library
            val isLibraryActive = !isNotesActive && activeFilter != BookFilter.READING
            HighDensityNavItem(
                icon = Icons.Default.LibraryBooks,
                label = "Kitaplık",
                isActive = isLibraryActive,
                onClick = onLibraryClick
            )

            // Tab 2: Recent
            val isRecentActive = !isNotesActive && activeFilter == BookFilter.READING
            HighDensityNavItem(
                icon = Icons.Default.History,
                label = "Son Okunan",
                isActive = isRecentActive,
                onClick = onRecentClick
            )

            // Tab 3: Notes (Not Defterleri)
            HighDensityNavItem(
                icon = Icons.Default.EditNote,
                label = "Notlar",
                isActive = isNotesActive,
                onClick = onNotesClick
            )

            // Tab 4: Cihazı Tara (Automatic Device Storage Scanner)
            HighDensityNavItem(
                icon = Icons.Default.Refresh,
                label = "Cihazı Tara",
                isActive = false,
                onClick = onScanDeviceClick
            )
        }
    }
}

@Composable
fun HighDensityNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(HighDensityPrimaryContainer)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = HighDensityPrimaryNavy,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = HighDensityIconGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) HighDensityPrimaryNavy else HighDensityIconGray
        )
    }
}

@Composable
fun EmptyLibraryView(
    isSearching: Boolean,
    isScanning: Boolean = false,
    onRescanDevice: () -> Unit,
    onSelectFolder: () -> Unit,
    onSelectFile: () -> Unit,
    onLoadSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isScanning) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HighDensityPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    strokeWidth = 3.dp,
                    color = HighDensityPrimaryNavy
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Cihazınızdaki Kitaplar Taranıyor...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cihaz belleği otomatik taranıyor. Bulunan PDF ve EPUB kitaplar anında eklenecektir.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HighDensityPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Search else Icons.Default.LocalLibrary,
                    contentDescription = null,
                    tint = HighDensityPrimaryNavy,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isSearching) "Aramanızla Eşleşen Kitap Bulunamadı" else "Cihazınızda PDF/EPUB Kitap Bulunamadı",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearching) {
                    "Farklı bir anahtar kelime ile aramayı deneyebilirsiniz."
                } else {
                    "Cihaz depolaması otomatik olarak tarandı. İndirilenler veya Belgeler klasörünüzde kitap tespit edilemedi."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isSearching) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRescanDevice,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .testTag("rescan_device_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cihazı Yeniden Tara")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSelectFile,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .testTag("empty_select_file_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dosya Seçerek Ekle")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onSelectFolder,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .testTag("empty_select_folder_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Klasörden İçe Aktar")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onLoadSamples,
                    modifier = Modifier.testTag("load_sample_books_button")
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Örnek Kitapları Yükle")
                }
            }
        }
    }
}
