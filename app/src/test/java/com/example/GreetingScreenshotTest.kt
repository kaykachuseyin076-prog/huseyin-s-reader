package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BookEntity
import com.example.ui.library.BookCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleBook = BookEntity(
      id = "sample-1",
      uriString = "content://sample/book.epub",
      title = "The Empire of Cotton",
      author = "Sven Beckert",
      format = "EPUB",
      lastReadProgress = 0.35f,
      currentPage = 4,
      totalPages = 12
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        BookCard(
          book = sampleBook,
          onClick = {},
          modifier = Modifier
            .width(180.dp)
            .padding(16.dp)
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
