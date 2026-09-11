package com.example.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

object DeviceBookScanner {
    private const val TAG = "DeviceBookScanner"

    data class DiscoveredDocument(
        val uri: Uri,
        val fileName: String,
        val fileSize: Long,
        val isPdf: Boolean,
        val lastModified: Long,
        val filePath: String? = null
    )

    /**
     * Recursively and asynchronously scans all accessible device storage locations for PDF and EPUB files.
     * Uses modern MediaStore APIs, direct accessible filesystem traversal, and SAF persisted trees.
     */
    suspend fun discoverAllDocuments(
        context: Context,
        persistedFolders: List<Uri> = emptyList(),
        onProgressUpdate: (suspend (scannedCount: Int, foundCount: Int) -> Unit)? = null
    ): List<DiscoveredDocument> {
        val discovered = mutableListOf<DiscoveredDocument>()
        val seenSignatures = mutableSetOf<String>()
        var scannedCounter = 0

        fun addIfNew(doc: DiscoveredDocument) {
            // Signature based on canonical path or filename + size to prevent duplicates across MediaStore and Filesystem
            val signature = doc.filePath?.lowercase() ?: "${doc.fileName.lowercase()}_${doc.fileSize}"
            if (!seenSignatures.contains(signature) && doc.fileSize > 64L) {
                seenSignatures.add(signature)
                discovered.add(doc)
            }
        }

        // 1. Scan via MediaStore.Files (Primary modern Android storage index)
        try {
            val mediaStoreDocs = scanMediaStore(context)
            for (doc in mediaStoreDocs) {
                addIfNew(doc)
            }
            scannedCounter += mediaStoreDocs.size
            onProgressUpdate?.invoke(scannedCounter, discovered.size)
        } catch (e: Exception) {
            Log.w(TAG, "Error querying MediaStore: ${e.message}")
        }

        // 2. Scan directly accessible filesystem storage directories (Books, Downloads, Documents, SD/emulated)
        try {
            val rootDirs = getAccessibleStorageDirectories(context)
            for (dir in rootDirs) {
                if (dir.exists() && dir.canRead()) {
                    scanDirectoryRecursive(
                        directory = dir,
                        maxDepth = 10,
                        currentDepth = 0,
                        onFileVisited = { scannedCounter++ },
                        onDocumentFound = { doc ->
                            addIfNew(doc)
                            onProgressUpdate?.invoke(scannedCounter, discovered.size)
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in filesystem scan: ${e.message}")
        }

        // 3. Scan persisted SAF Tree URIs (if any previously granted by user)
        for (treeUri in persistedFolders) {
            try {
                val docFile = DocumentFile.fromTreeUri(context, treeUri)
                if (docFile != null && docFile.exists() && docFile.canRead()) {
                    scanDocumentFileRecursive(
                        directory = docFile,
                        maxDepth = 10,
                        currentDepth = 0,
                        onFileVisited = { scannedCounter++ },
                        onDocumentFound = { doc ->
                            addIfNew(doc)
                            onProgressUpdate?.invoke(scannedCounter, discovered.size)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error scanning SAF tree $treeUri: ${e.message}")
            }
        }

        return discovered
    }

    /**
     * Queries MediaStore.Files for all PDF and EPUB files across external storage volumes.
     */
    private fun scanMediaStore(context: Context): List<DiscoveredDocument> {
        val results = mutableListOf<DiscoveredDocument>()

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA
        )

        val selection = "(${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf' OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.epub')"

        val selectionArgs = arrayOf("application/pdf", "application/epub+zip")

        try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else continue
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Belge" else "Belge"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val dateModified = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null

                    val isPdf = name.endsWith(".pdf", ignoreCase = true) || mime == "application/pdf"
                    val isEpub = name.endsWith(".epub", ignoreCase = true) || mime == "application/epub+zip"

                    if (isPdf || isEpub) {
                        val contentUri = ContentUris.withAppendedId(collectionUri, id)
                        results.add(
                            DiscoveredDocument(
                                uri = contentUri,
                                fileName = name,
                                fileSize = size,
                                isPdf = isPdf,
                                lastModified = dateModified,
                                filePath = dataPath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query failed: ${e.message}")
        }

        return results
    }

    /**
     * Collects all candidate accessible directories for filesystem traversal.
     */
    private fun getAccessibleStorageDirectories(context: Context): List<File> {
        val dirs = LinkedHashSet<File>()

        // 1. Standard public storage root (if accessible)
        try {
            val extStorage = Environment.getExternalStorageDirectory()
            if (extStorage != null) {
                dirs.add(extStorage)
                dirs.add(File(extStorage, "Books"))
                dirs.add(File(extStorage, "Documents"))
                dirs.add(File(extStorage, "Download"))
                dirs.add(File(extStorage, "Downloads"))
                dirs.add(File(extStorage, "eBooks"))
                dirs.add(File(extStorage, "Kindle"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Standard storage directory access: ${e.message}")
        }

        // 2. Environment standard public directories
        try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { dirs.add(it) }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)?.let { dirs.add(it) }
        } catch (e: Exception) {
            Log.d(TAG, "Public directories access: ${e.message}")
        }

        // 3. App external files and media directories
        try {
            context.getExternalFilesDirs(null).filterNotNull().forEach { dirs.add(it) }
            context.externalMediaDirs.filterNotNull().forEach { dirs.add(it) }
        } catch (e: Exception) {
            Log.d(TAG, "App external dirs access: ${e.message}")
        }

        return dirs.toList()
    }

    /**
     * Recursively walks accessible filesystem directories to discover PDF and EPUB files.
     */
    private suspend fun scanDirectoryRecursive(
        directory: File,
        maxDepth: Int,
        currentDepth: Int,
        onFileVisited: () -> Unit,
        onDocumentFound: suspend (DiscoveredDocument) -> Unit
    ) {
        if (currentDepth > maxDepth || !directory.canRead()) return

        val files = try {
            directory.listFiles()
        } catch (e: Exception) {
            null
        } ?: return

        for (file in files) {
            onFileVisited()
            val name = file.name

            // Skip hidden files/directories (starting with ".")
            if (name.startsWith(".")) continue

            if (file.isDirectory) {
                // Skip restricted system scoped storage paths that cause slowdowns or permission blocks
                if (file.absolutePath.contains("/Android/data") || file.absolutePath.contains("/Android/obb")) {
                    continue
                }
                scanDirectoryRecursive(file, maxDepth, currentDepth + 1, onFileVisited, onDocumentFound)
            } else if (file.isFile) {
                val lowerName = name.lowercase()
                val isPdf = lowerName.endsWith(".pdf")
                val isEpub = lowerName.endsWith(".epub")

                if ((isPdf || isEpub) && file.length() > 64L) {
                    onDocumentFound(
                        DiscoveredDocument(
                            uri = Uri.fromFile(file),
                            fileName = name,
                            fileSize = file.length(),
                            isPdf = isPdf,
                            lastModified = file.lastModified(),
                            filePath = file.absolutePath
                        )
                    )
                }
            }
        }
    }

    /**
     * Recursively walks a SAF DocumentFile directory.
     */
    private suspend fun scanDocumentFileRecursive(
        directory: DocumentFile,
        maxDepth: Int,
        currentDepth: Int,
        onFileVisited: () -> Unit,
        onDocumentFound: suspend (DiscoveredDocument) -> Unit
    ) {
        if (currentDepth > maxDepth || !directory.canRead()) return

        val files = try {
            directory.listFiles()
        } catch (e: Exception) {
            null
        } ?: return

        for (file in files) {
            onFileVisited()
            val name = file.name ?: continue
            if (name.startsWith(".")) continue

            if (file.isDirectory) {
                scanDocumentFileRecursive(file, maxDepth, currentDepth + 1, onFileVisited, onDocumentFound)
            } else if (file.isFile) {
                val lower = name.lowercase()
                val isPdf = lower.endsWith(".pdf") || file.type == "application/pdf"
                val isEpub = lower.endsWith(".epub") || file.type == "application/epub+zip"

                if (isPdf || isEpub) {
                    onDocumentFound(
                        DiscoveredDocument(
                            uri = file.uri,
                            fileName = name,
                            fileSize = file.length(),
                            isPdf = isPdf,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
    }

    /**
     * Verifies if a given URI or file path is still accessible on the device.
     */
    fun isDocumentAccessible(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return false)
                file.exists() && file.canRead()
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }
}
