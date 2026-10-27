package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "documents", indices = [Index("folderId"), Index("isTrashed"), Index("isVaulted")])
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val filePath: String,
    val uriString: String = "",
    val formatName: String = "PDF",
    val sizeBytes: Long = 0L,
    val pageCount: Int = 1,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastPage: Int = 0,
    val lastZoom: Float = 1.0f,
    val viewModeName: String = "VERTICAL_CONTINUOUS",
    val fitModeName: String = "FIT_WIDTH",
    val readingThemeName: String = "DAY",
    val isFavourite: Boolean = false,
    val folderId: Long? = null,
    val tagsCsv: String = "",
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val isVaulted: Boolean = false,
    val indexedContent: String = ""
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "bookmarks", indices = [Index("documentId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "annotations", indices = [Index("documentId")])
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val typeName: String, // HIGHLIGHT, UNDERLINE, STRIKETHROUGH, SQUIGGLY, FREEHAND_PEN, RECTANGLE, CIRCLE, ARROW, TEXT_BOX, STICKY_NOTE, STAMP, SIGNATURE
    val colorArgb: Long = 0xFF4F5BD5,
    val strokeWidth: Float = 4f,
    val opacity: Float = 0.85f,
    val textContent: String = "",
    val pointsJson: String = "", // Serialized normalized x,y pairs
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_presets")
data class ToolPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val toolIdName: String,
    val configSummary: String,
    val configJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "batch_jobs")
data class BatchJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val toolIdName: String,
    val status: String, // RUNNING, COMPLETED, FAILED, CANCELLED
    val progressPercent: Int = 0,
    val inputFilesCount: Int = 1,
    val outputPath: String = "",
    val beforeBytes: Long = 0L,
    val afterBytes: Long = 0L,
    val summaryMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val isInitials: Boolean = false,
    val typedText: String = "",
    val strokePointsJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
