package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.BookEntity
import com.example.ui.theme.EpubBadgeColor
import java.io.File
import java.util.Locale

@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("book_card_${book.id.hashCode()}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            // Book Cover Frame (3:4 ratio, rounded-2xl, border, shadow-sm)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                val hasCoverFile = book.coverPath != null && File(book.coverPath).exists()

                if (hasCoverFile) {
                    AsyncImage(
                        model = File(book.coverPath!!),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DefaultBookCover(
                        title = book.title,
                        author = book.author,
                        format = book.format
                    )
                }

                // Subtle bottom shadow gradient matching design
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.18f)
                                )
                            )
                        )
                )

                // High Density Format Badge (Top-Right)
                FormatBadge(
                    format = book.format,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                // Reading Progress bar at base of cover
                if (book.lastReadProgress > 0f) {
                    LinearProgressIndicator(
                        progress = { book.lastReadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.25f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Book Title (line-clamp-2 text-sm font-medium leading-tight)
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Subtitle / Size & Progress (text-[11px] text-[#74777F])
            val subtext = buildString {
                if (book.lastReadProgress > 0f) {
                    val progressPercent = (book.lastReadProgress * 100).toInt().coerceIn(1, 100)
                    append("%$progressPercent • ")
                }
                if (book.fileSize > 0) {
                    append(formatFileSize(book.fileSize))
                } else if (!book.author.isNullOrBlank()) {
                    append(book.author)
                } else {
                    append(book.format.uppercase())
                }
            }

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun FormatBadge(
    format: String,
    modifier: Modifier = Modifier
) {
    val isPdf = format.equals("PDF", ignoreCase = true)
    val bgColor = if (isPdf) {
        Color.Black.copy(alpha = 0.65f)
    } else {
        EpubBadgeColor.copy(alpha = 0.90f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = format.uppercase(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DefaultBookCover(
    title: String,
    author: String?,
    format: String,
    modifier: Modifier = Modifier
) {
    val isPdf = format.equals("PDF", ignoreCase = true)
    val bgColor = if (isPdf) {
        Color(0xFFFFFFFF)
    } else {
        Color(0xFFFFEBB7) // Soft amber tone from High Density HTML
    }
    val iconColor = if (isPdf) Color(0xFF8E919A) else Color(0xFFD97706)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isPdf) Icons.Default.Description else Icons.Default.Book,
                contentDescription = null,
                tint = iconColor.copy(alpha = 0.45f),
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp
                ),
                color = Color(0xFF191C1E),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!author.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF74777F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%d KB", kb.toLong())
    }
}
