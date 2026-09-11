package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BookTitleCleaner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Reader", appName)
  }

  @Test
  fun `test book title cleaner`() {
    val cleaned = BookTitleCleaner.cleanFileName("the_empire_of_cotton.pdf")
    assertEquals("The Empire of Cotton", cleaned)
  }

  @Test
  fun `test reader theme values`() {
    val light = com.example.ui.reader.ReaderTheme.LIGHT
    val sepia = com.example.ui.reader.ReaderTheme.SEPIA
    val dark = com.example.ui.reader.ReaderTheme.DARK

    assertEquals("Açık", light.displayName)
    assertEquals("Sepya", sepia.displayName)
    assertEquals("Koyu", dark.displayName)
  }

  @Test
  fun `test reading locator model`() {
    val locator = com.example.ui.reader.ReadingLocator(
      bookId = "book-123",
      format = "EPUB",
      chapterOrPageIndex = 2,
      paragraphIndex = 5,
      startOffset = 10,
      endOffset = 35,
      selectedText = "Tilki şöyle dedi: Hoşça git."
    )
    assertEquals("book-123", locator.bookId)
    assertEquals(2, locator.chapterOrPageIndex)
    assertEquals(5, locator.paragraphIndex)
    assertEquals("Tilki şöyle dedi: Hoşça git.", locator.selectedText)
  }

    @Test
  fun `test reader ui state progress calculation`() {
    val state = com.example.ui.reader.ReaderUiState(
      currentPage = 49,
      totalPages = 100
    )
    assertEquals(50, state.progressPercentage)
  }

  @Test
  fun `test highlight colors palette`() {
    val yellow = com.example.ui.reader.HighlightColor.fromId("YELLOW")
    val orange = com.example.ui.reader.HighlightColor.fromId("ORANGE")
    val green = com.example.ui.reader.HighlightColor.fromId("GREEN")
    val blue = com.example.ui.reader.HighlightColor.fromId("BLUE")
    val pink = com.example.ui.reader.HighlightColor.fromId("PINK")

    assertEquals("Sarı", yellow.displayName)
    assertEquals("Turuncu", orange.displayName)
    assertEquals("Yeşil", green.displayName)
    assertEquals("Mavi", blue.displayName)
    assertEquals("Pembe", pink.displayName)
  }

  @Test
  fun `test sentence range boundary finder`() {
    val text = "Sanayi Devrimi modern dünyayı değiştirdi. Bu cümle ikinci cümledir. Son cümle burada."
    // Offset inside first sentence
    val range1 = com.example.ui.reader.findSentenceRange(text, 10)
    assertEquals("Sanayi Devrimi modern dünyayı değiştirdi.", text.substring(range1.first, range1.second).trim())

    // Offset inside second sentence
    val range2 = com.example.ui.reader.findSentenceRange(text, 45)
    assertEquals("Bu cümle ikinci cümledir.", text.substring(range2.first, range2.second).trim())
  }

  @Test
  fun `test highlight with note hasNote logic`() {
    val hl = com.example.data.local.entity.HighlightEntity(
      id = "hl-1",
      bookId = "book-1",
      format = "EPUB",
      chapterOrPageIndex = 0,
      paragraphIndex = 1,
      startOffset = 0,
      endOffset = 20,
      selectedText = "Önemli fikir.",
      colorId = "ORANGE"
    )

    val itemWithoutNote = com.example.data.local.entity.HighlightWithNote(
      highlight = hl,
      note = null
    )
    assertEquals(false, itemWithoutNote.hasNote)

    val itemWithEmptyNote = com.example.data.local.entity.HighlightWithNote(
      highlight = hl,
      note = com.example.data.local.entity.NoteEntity(
        id = "note-1",
        highlightId = "hl-1",
        bookId = "book-1",
        noteText = "   "
      )
    )
    assertEquals(false, itemWithEmptyNote.hasNote)

    val itemWithActiveNote = com.example.data.local.entity.HighlightWithNote(
      highlight = hl,
      note = com.example.data.local.entity.NoteEntity(
        id = "note-2",
        highlightId = "hl-1",
        bookId = "book-1",
        noteText = "Bu fikir çok önemli. Daha sonra tekrar bak."
      )
    )
    assertEquals(true, itemWithActiveNote.hasNote)
  }

  @Test
  fun `test ink stroke point serialization and deserialization`() {
    val points = listOf(
      com.example.data.local.entity.InkPoint(x = 0.1f, y = 0.2f, pressure = 0.8f),
      com.example.data.local.entity.InkPoint(x = 0.35f, y = 0.45f, pressure = 1.0f),
      com.example.data.local.entity.InkPoint(x = 0.5f, y = 0.6f, pressure = 0.6f)
    )

    val serialized = com.example.data.local.entity.InkStrokeEntity.serializePoints(points)
    val entity = com.example.data.local.entity.InkStrokeEntity(
      id = "s-1",
      bookId = "b-1",
      format = "PDF",
      pageOrChapter = 0,
      toolType = "PEN",
      color = 0xFF1E1E1EL,
      strokeWidth = 4f,
      pointsData = serialized
    )
    val deserialized = entity.parsePoints()

    assertEquals(3, deserialized.size)
    assertEquals(0.1f, deserialized[0].x, 0.001f)
    assertEquals(0.2f, deserialized[0].y, 0.001f)
    assertEquals(0.8f, deserialized[0].pressure, 0.001f)

    assertEquals(0.35f, deserialized[1].x, 0.001f)
    assertEquals(0.45f, deserialized[1].y, 0.001f)

    assertEquals(0.5f, deserialized[2].x, 0.001f)
    assertEquals(0.6f, deserialized[2].y, 0.001f)
  }

  @Test
  fun `test ink tool types and pen thickness values`() {
    val pen = com.example.ui.reader.ink.InkToolType.PEN
    val highlighter = com.example.ui.reader.ink.InkToolType.HIGHLIGHTER
    val eraser = com.example.ui.reader.ink.InkToolType.ERASER

    assertEquals("Kalem", pen.displayName)
    assertEquals("Fosforlu", highlighter.displayName)
    assertEquals("Silgi", eraser.displayName)

    val thin = com.example.ui.reader.ink.PenThickness.THIN
    val medium = com.example.ui.reader.ink.PenThickness.MEDIUM
    val thick = com.example.ui.reader.ink.PenThickness.THICK

    assertEquals(2.5f, thin.penDp.value, 0.01f)
    assertEquals(5.0f, medium.penDp.value, 0.01f)
    assertEquals(9.0f, thick.penDp.value, 0.01f)
  }

  @Test
  fun `test renderable stroke conversion from entity`() {
    val points = listOf(
      com.example.data.local.entity.InkPoint(x = 0.2f, y = 0.3f, pressure = 0.9f),
      com.example.data.local.entity.InkPoint(x = 0.4f, y = 0.5f, pressure = 0.9f)
    )

    val entity = com.example.data.local.entity.InkStrokeEntity(
      id = "stroke-abc",
      bookId = "book-456",
      format = "PDF",
      pageOrChapter = 3,
      toolType = "PEN",
      color = 0xFF1E1E1EL,
      strokeWidth = 4f,
      alpha = 1.0f,
      pointsData = com.example.data.local.entity.InkStrokeEntity.serializePoints(points)
    )

    val stroke = com.example.ui.reader.ink.RenderableStroke.fromEntity(entity)
    assertEquals("stroke-abc", stroke.id)
    assertEquals(com.example.ui.reader.ink.InkToolType.PEN, stroke.toolType)
    assertEquals(4f, stroke.strokeWidthDp, 0.01f)
    assertEquals(1.0f, stroke.alpha, 0.01f)
    assertEquals(2, stroke.points.size)
    assertEquals(0.2f, stroke.points[0].x, 0.001f)
    assertEquals(0.3f, stroke.points[0].y, 0.001f)
  }

  @Test
  fun `test gemini settings manager save, get and delete key`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.gemini.GeminiSettingsManager.deleteApiKey(context)

    // Initially no custom key
    val initialKey = com.example.data.gemini.GeminiSettingsManager.getCustomApiKey(context)
    org.junit.Assert.assertNull(initialKey)

    // Save key
    com.example.data.gemini.GeminiSettingsManager.saveApiKey(context, "AIzaSyTest1234567890Key")
    val savedKey = com.example.data.gemini.GeminiSettingsManager.getCustomApiKey(context)
    assertEquals("AIzaSyTest1234567890Key", savedKey)
    org.junit.Assert.assertTrue(com.example.data.gemini.GeminiSettingsManager.hasApiKey(context))

    // Masked key
    val masked = com.example.data.gemini.GeminiSettingsManager.getMaskedApiKey(context)
    org.junit.Assert.assertNotNull(masked)
    org.junit.Assert.assertTrue(masked!!.startsWith("AIza"))
    org.junit.Assert.assertTrue(masked.endsWith("0Key"))

    // Delete key
    com.example.data.gemini.GeminiSettingsManager.deleteApiKey(context)
    val deletedKey = com.example.data.gemini.GeminiSettingsManager.getCustomApiKey(context)
    org.junit.Assert.assertNull(deletedKey)
  }

  @Test
  fun `test gemini test connection fails if key is blank`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.gemini.GeminiSettingsManager.deleteApiKey(context)
    val repo = com.example.data.gemini.GeminiRepository(context)

    val result = repo.testConnection(overrideApiKey = "")
    org.junit.Assert.assertTrue(result.isFailure)
  }

  @Test
  fun `test highlight entity spatial normalized coordinates`() {
    val highlight = com.example.data.local.entity.HighlightEntity(
      id = "hl-spatial-1",
      bookId = "book-pdf-1",
      format = "PDF",
      chapterOrPageIndex = 0,
      paragraphIndex = 2,
      startOffset = 5,
      endOffset = 25,
      selectedText = "Gerçek seçilen metin burada.",
      colorId = "GREEN",
      normLeft = 0.12f,
      normTop = 0.34f,
      normRight = 0.88f,
      normBottom = 0.37f
    )

    assertEquals("Gerçek seçilen metin burada.", highlight.selectedText)
    assertEquals(0.12f, highlight.normLeft, 0.001f)
    assertEquals(0.34f, highlight.normTop, 0.001f)
    assertEquals(0.88f, highlight.normRight, 0.001f)
    assertEquals(0.37f, highlight.normBottom, 0.001f)
  }

  @Test
  fun `test pdf text extractor getPageLayout and findTextAt`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val layout = com.example.util.PdfTextExtractor.getPageLayout(
      context = context,
      uri = null,
      pageIndex = 0,
      bookTitle = "Reader Kullanıcı Rehberi"
    )

    org.junit.Assert.assertTrue(layout.lines.isNotEmpty())
    org.junit.Assert.assertTrue(layout.fullText.isNotBlank())

    // Test finding text at normalized coordinate of first line (~0.12f, 0.14f)
    val result = layout.findTextAt(0.2f, 0.14f)
    org.junit.Assert.assertNotNull(result)
    org.junit.Assert.assertTrue(result!!.selectedText.isNotBlank())
    org.junit.Assert.assertTrue(result.normRight > result.normLeft)
  }

  @Test
  fun `test word and sentence range accuracy in epub paragraph`() {
    val paragraph = "Kullanıcı PDF veya EPUB içinde farklı metinler seçer. Seçimler her zaman doğru olmalıdır."

    // Offset in 'metinler'
    val wordOffset = paragraph.indexOf("metinler")
    val wordRange = com.example.ui.reader.findWordRange(paragraph, wordOffset + 2)
    assertEquals("metinler", paragraph.substring(wordRange.first, wordRange.second))

    // Offset in second sentence
    val secondSentenceOffset = paragraph.indexOf("Seçimler")
    val sentenceRange = com.example.ui.reader.findSentenceRange(paragraph, secondSentenceOffset + 5)
    assertEquals("Seçimler her zaman doğru olmalıdır.", paragraph.substring(sentenceRange.first, sentenceRange.second).trim())
  }

  @Test
  fun `test incoming uri extraction from action view intent`() {
    val sampleUri = android.net.Uri.parse("content://com.android.providers.downloads/download.pdf")
    val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
      setDataAndType(sampleUri, "application/pdf")
      addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val extracted = MainActivity.extractIncomingUri(viewIntent)
    org.junit.Assert.assertEquals(sampleUri, extracted)
  }

  @Test
  fun `test incoming uri extraction from action send intent`() {
    val sampleUri = android.net.Uri.parse("content://com.example.provider/books/history.epub")
    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
      type = "application/epub+zip"
      putExtra(android.content.Intent.EXTRA_STREAM, sampleUri)
    }

    val extracted = MainActivity.extractIncomingUri(sendIntent)
    org.junit.Assert.assertEquals(sampleUri, extracted)
  }

  @Test
  fun `test pdf helper extract table of contents and session close`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fakeUri = android.net.Uri.parse("content://test/dummy.pdf")
    val toc = com.example.util.PdfHelper.extractTableOfContents(context, fakeUri, 5)

    assertEquals(5, toc.size)
    assertEquals("Sayfa 1", toc[0].title)
    assertEquals(0, toc[0].targetIndex)
    assertEquals("Sayfa 5", toc[4].title)
    assertEquals(4, toc[4].targetIndex)

    // Verify closing session doesn't throw
    com.example.util.PdfHelper.closeSession()
  }
}
