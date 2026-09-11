package com.example.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BookEntity
import com.example.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BookRepository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(BookFilter.ALL)
    private val _isLoading = MutableStateFlow(false)
    private val _isScanning = MutableStateFlow(false)
    private val _scanProgressText = MutableStateFlow<String?>(null)
    private val _scannedCount = MutableStateFlow(0)
    private val _foundCount = MutableStateFlow(0)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        // Automatic initial scan of device storage for PDF/EPUB books on startup
        scanDeviceStorage(isInitial = true)
    }

    private val _filteredBooks = combine(
        repository.allBooks,
        _searchQuery,
        _selectedFilter
    ) { allBooks, query, filter ->
        val filtered = allBooks.filter { book ->
            val matchesQuery = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    (book.author?.contains(query, ignoreCase = true) == true)

            val matchesFilter = when (filter) {
                BookFilter.ALL -> true
                BookFilter.PDF -> book.format.equals("PDF", ignoreCase = true)
                BookFilter.EPUB -> book.format.equals("EPUB", ignoreCase = true)
                BookFilter.READING -> book.lastReadTimestamp > 0L
            }

            matchesQuery && matchesFilter
        }
        Pair(allBooks.size, filtered)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _filteredBooks,
        _isLoading,
        _isScanning,
        _scanProgressText,
        _statusMessage
    ) { booksPair, loading, scanning, progressText, status ->
        val (totalCount, filtered) = booksPair
        LibraryUiState(
            books = filtered,
            isLoading = loading,
            isScanning = scanning,
            scanProgressText = progressText,
            scannedCount = _scannedCount.value,
            foundCount = _foundCount.value,
            searchQuery = _searchQuery.value,
            selectedFilter = _selectedFilter.value,
            totalBooksCount = totalCount,
            statusMessage = status,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState(isLoading = true, isScanning = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: BookFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Automatic device storage scan: searches MediaStore, standard accessible directories,
     * and previously persisted SAF folders for PDF and EPUB files.
     */
    fun scanDeviceStorage(isInitial: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Cihaz belleği taranıyor..."
            try {
                val newBooksAdded = repository.scanDeviceStorage(
                    onProgress = { scanned, found ->
                        _scannedCount.value = scanned
                        _foundCount.value = found
                        _scanProgressText.value = if (found > 0) {
                            "Cihaz taranıyor... ($found kitap bulundu)"
                        } else {
                            "Cihaz taranıyor... ($scanned dosya incelendi)"
                        }
                    }
                )
                if (!isInitial || newBooksAdded > 0) {
                    _statusMessage.value = if (newBooksAdded > 0) {
                        "$newBooksAdded yeni kitap kütüphaneye eklendi."
                    } else {
                        "Tarama tamamlandı. Kütüphaneniz güncel."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Cihaz taranırken hata: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
                _scanProgressText.value = null
            }
        }
    }

    fun scanFolder(treeUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Klasör taranıyor..."
            try {
                val foundCount = repository.scanAndAddFolder(treeUri)
                _statusMessage.value = if (foundCount > 0) {
                    "$foundCount yeni kitap kütüphaneye eklendi."
                } else {
                    "Klasör tarandı. Yeni PDF veya EPUB kitap bulunamadı."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Klasör taranırken hata oluştu: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun importAndGetBook(uri: Uri): BookEntity? {
        return try {
            repository.addSingleDocument(uri)
        } catch (e: Exception) {
            null
        }
    }

    fun addSingleDocument(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val book = repository.addSingleDocument(uri)
                if (book != null) {
                    _statusMessage.value = "\"${book.title}\" kütüphaneye eklendi."
                } else {
                    _errorMessage.value = "Seçilen dosya PDF veya EPUB olarak tanınamadı."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Dosya eklenemedi: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLibrary() {
        scanDeviceStorage(isInitial = false)
    }

    fun loadSampleBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.loadSampleBooksExplicitly()
                _statusMessage.value = "Örnek kitaplar kütüphaneye yüklendi."
            } catch (e: Exception) {
                _errorMessage.value = "Örnek kitaplar yüklenirken hata: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            try {
                repository.deleteBook(bookId)
                _statusMessage.value = "Kitap kütüphaneden kaldırıldı."
            } catch (e: Exception) {
                _errorMessage.value = "Silme işlemi başarısız oldu."
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
