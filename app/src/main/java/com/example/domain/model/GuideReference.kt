package com.example.domain.model

/**
 * Exercise or equipment technique guide reference (N07.4).
 * Holds source, verification date, language, textual instructions, and video link.
 */
data class GuideReference(
    val exerciseId: String,
    val title: String,
    val source: String,
    val language: String = "ko",
    val verifiedDateEpochMs: Long,
    val videoUrl: String? = null,
    val summaryText: String,
    val canEmbed: Boolean = true
) {
    fun resolveDisplayFallback(): String {
        return if (!canEmbed && videoUrl != null) {
            "$summaryText\n(외부 영상 바로보기: $videoUrl)"
        } else {
            summaryText
        }
    }
}
