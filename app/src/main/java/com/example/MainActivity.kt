package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.notebook.NotebookScreen
import com.example.ui.reader.ReaderScreen
import com.example.ui.reader.ReaderViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val incomingUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReaderApp(
                        incomingUri = incomingUriState.value,
                        onIncomingUriHandled = { incomingUriState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = extractIncomingUri(intent)
        if (uri != null) {
            incomingUriState.value = uri
        }
    }

    companion object {
        fun extractIncomingUri(intent: Intent?): Uri? {
            if (intent == null) return null
            val action = intent.action
            if (action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) {
                return intent.data
                    ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    ?: if (intent.clipData != null && intent.clipData!!.itemCount > 0) intent.clipData!!.getItemAt(0).uri else null
            }
            return null
        }
    }
}

@Composable
fun ReaderApp(
    incomingUri: Uri? = null,
    onIncomingUriHandled: () -> Unit = {}
) {
    val libraryViewModel: LibraryViewModel = viewModel()
    val readerViewModel: ReaderViewModel = viewModel()

    var activeBookId by remember { mutableStateOf<String?>(null) }
    var activeBookTargetPage by remember { mutableStateOf<Int?>(null) }
    var activeNotebookId by remember { mutableStateOf<String?>(null) }

    // Automatically import and open books received via "Open With" / "Birlikte Aç"
    LaunchedEffect(incomingUri) {
        if (incomingUri != null) {
            val book = libraryViewModel.importAndGetBook(incomingUri)
            if (book != null) {
                activeNotebookId = null
                activeBookId = book.id
                activeBookTargetPage = null
            }
            onIncomingUriHandled()
        }
    }

    if (activeNotebookId != null) {
        BackHandler {
            activeNotebookId = null
        }
        NotebookScreen(
            notebookId = activeNotebookId!!,
            onBack = { activeNotebookId = null },
            onNavigateToBook = { bookId, targetPage ->
                activeNotebookId = null
                activeBookId = bookId
                activeBookTargetPage = targetPage
            }
        )
    } else if (activeBookId != null) {
        BackHandler {
            activeBookId = null
            activeBookTargetPage = null
        }
        ReaderScreen(
            bookId = activeBookId!!,
            targetPage = activeBookTargetPage,
            viewModel = readerViewModel,
            onBack = {
                activeBookId = null
                activeBookTargetPage = null
            },
            onOpenNotebook = { notebookId ->
                activeNotebookId = notebookId
            }
        )
    } else {
        LibraryScreen(
            viewModel = libraryViewModel,
            onBookSelected = { book ->
                activeBookId = book.id
                activeBookTargetPage = null
            },
            onNotebookSelected = { notebook ->
                activeNotebookId = notebook.id
            }
        )
    }
}
