package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolioDao {
    @Query("SELECT * FROM documents WHERE isTrashed = 0 AND isVaulted = 0 ORDER BY lastOpenedAt DESC")
    fun observeLibraryDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrashed = 0 AND isVaulted = 0 AND isFavourite = 1 ORDER BY lastOpenedAt DESC")
    fun observeFavouriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isVaulted = 1 AND isTrashed = 0 ORDER BY lastOpenedAt DESC")
    fun observeVaultedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE filePath = :path LIMIT 1")
    suspend fun getDocumentByPath(path: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentPermanently(id: Long)

    @Query("DELETE FROM documents WHERE isTrashed = 1 AND trashedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrash(cutoffTimestamp: Long)

    @Query("DELETE FROM documents WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // Folders
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    // Tags
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE documentId = :docId ORDER BY pageIndex ASC")
    fun observeBookmarksForDocument(docId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)

    // Annotations
    @Query("SELECT * FROM annotations WHERE documentId = :docId ORDER BY createdAt ASC")
    fun observeAnnotationsForDocument(docId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE documentId = :docId ORDER BY createdAt ASC")
    suspend fun getAnnotationsForDocumentSync(docId: Long): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Query("DELETE FROM annotations WHERE id = :annotationId")
    suspend fun deleteAnnotation(annotationId: Long)

    @Query("DELETE FROM annotations WHERE documentId = :docId")
    suspend fun clearAnnotationsForDocument(docId: Long)

    // Tool Presets
    @Query("SELECT * FROM tool_presets ORDER BY createdAt DESC")
    fun observeToolPresets(): Flow<List<ToolPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToolPreset(preset: ToolPresetEntity): Long

    @Query("DELETE FROM tool_presets WHERE id = :presetId")
    suspend fun deleteToolPreset(presetId: Long)

    // Batch Jobs
    @Query("SELECT * FROM batch_jobs ORDER BY createdAt DESC")
    fun observeBatchJobs(): Flow<List<BatchJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchJob(job: BatchJobEntity): Long

    @Update
    suspend fun updateBatchJob(job: BatchJobEntity)

    @Query("DELETE FROM batch_jobs WHERE status != 'RUNNING'")
    suspend fun clearCompletedJobs()

    // Signatures
    @Query("SELECT * FROM signatures ORDER BY createdAt DESC")
    fun observeSignatures(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Query("DELETE FROM signatures WHERE id = :sigId")
    suspend fun deleteSignature(sigId: Long)
}
