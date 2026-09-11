package com.example.domain.util

/**
 * Utility to manage structured session notes and per-exercise feedback comments
 * within the single [com.example.domain.model.WorkoutSession.notes] string field,
 * preserving full offline Room DB and Firestore cloud sync compatibility without schema migrations.
 */
object SessionNotesManager {
    private const val COMMENT_TAG_PREFIX = "[EX_COMMENT:"
    private const val COMMENT_TAG_SUFFIX = "]"

    fun getSessionTitle(notes: String): String {
        val lines = notes.lines()
        val titleLine = lines.firstOrNull { !it.trim().startsWith(COMMENT_TAG_PREFIX) } ?: ""
        return titleLine.trim()
    }

    fun setSessionTitle(notes: String, newTitle: String): String {
        val existingComments = extractAllComments(notes)
        val builder = StringBuilder(newTitle.trim())
        existingComments.forEach { (exerciseId, comment) ->
            builder.append("\n").append(COMMENT_TAG_PREFIX).append(exerciseId).append(COMMENT_TAG_SUFFIX).append(" ").append(comment)
        }
        return builder.toString()
    }

    fun getExerciseComment(notes: String, exerciseId: String): String {
        val tag = "$COMMENT_TAG_PREFIX$exerciseId$COMMENT_TAG_SUFFIX"
        for (line in notes.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith(tag)) {
                return trimmed.removePrefix(tag).trim()
            }
        }
        return ""
    }

    fun setExerciseComment(notes: String, exerciseId: String, comment: String): String {
        val comments = extractAllComments(notes).toMutableMap()
        if (comment.isBlank()) {
            comments.remove(exerciseId)
        } else {
            comments[exerciseId] = comment.trim()
        }
        val title = getSessionTitle(notes)
        val builder = StringBuilder(title)
        comments.forEach { (exId, comm) ->
            builder.append("\n").append(COMMENT_TAG_PREFIX).append(exId).append(COMMENT_TAG_SUFFIX).append(" ").append(comm)
        }
        return builder.toString()
    }

    private fun extractAllComments(notes: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (line in notes.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith(COMMENT_TAG_PREFIX)) {
                val endIndex = trimmed.indexOf(COMMENT_TAG_SUFFIX)
                if (endIndex != -1) {
                    val exerciseId = trimmed.substring(COMMENT_TAG_PREFIX.length, endIndex)
                    val comment = trimmed.substring(endIndex + COMMENT_TAG_SUFFIX.length).trim()
                    map[exerciseId] = comment
                }
            }
        }
        return map
    }
}
