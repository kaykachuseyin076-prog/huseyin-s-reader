package com.example.util

import java.util.Locale

object BookTitleCleaner {

    /**
     * Cleans a raw filename into a human-readable book title.
     * Example: "the_empire_of_cotton.pdf" -> "The Empire of Cotton"
     */
    fun cleanFileName(fileName: String): String {
        // Strip extension
        val withoutExt = fileName.substringBeforeLast('.')
        
        // Remove common tags like [z-lib.org], (v1.0), etc.
        val withoutTags = withoutExt
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("\\((z-lib|annas-archive|libgen|oceanofpdf)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("[-_]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (withoutTags.isBlank()) return fileName

        // Capitalize words
        val words = withoutTags.split(" ")
        val minorWords = setOf("a", "an", "the", "and", "but", "or", "for", "nor", "on", "at", "to", "from", "by", "in", "of", "ve", "veya", "ile", "de", "da", "bir")

        return words.mapIndexed { index, word ->
            val lower = word.lowercase(Locale.ROOT)
            if (index == 0 || index == words.lastIndex || lower !in minorWords) {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            } else {
                lower
            }
        }.joinToString(" ")
    }
}
