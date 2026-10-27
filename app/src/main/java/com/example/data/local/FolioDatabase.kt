package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        DocumentEntity::class,
        FolderEntity::class,
        TagEntity::class,
        BookmarkEntity::class,
        AnnotationEntity::class,
        ToolPresetEntity::class,
        BatchJobEntity::class,
        SignatureEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun folioDao(): FolioDao

    companion object {
        @Volatile
        private var INSTANCE: FolioDatabase? = null

        fun getInstance(context: Context): FolioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FolioDatabase::class.java,
                    "folio_offline.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class FolioRepository(private val dao: FolioDao) {
    val libraryDocuments: Flow<List<DocumentEntity>> = dao.observeLibraryDocuments()
    val favouriteDocuments: Flow<List<DocumentEntity>> = dao.observeFavouriteDocuments()
    val trashedDocuments: Flow<List<DocumentEntity>> = dao.observeTrashedDocuments()
    val vaultedDocuments: Flow<List<DocumentEntity>> = dao.observeVaultedDocuments()
    val folders: Flow<List<FolderEntity>> = dao.observeFolders()
    val tags: Flow<List<TagEntity>> = dao.observeTags()
    val toolPresets: Flow<List<ToolPresetEntity>> = dao.observeToolPresets()
    val batchJobs: Flow<List<BatchJobEntity>> = dao.observeBatchJobs()
    val signatures: Flow<List<SignatureEntity>> = dao.observeSignatures()

    fun observeBookmarks(docId: Long): Flow<List<BookmarkEntity>> = dao.observeBookmarksForDocument(docId)
    fun observeAnnotations(docId: Long): Flow<List<AnnotationEntity>> = dao.observeAnnotationsForDocument(docId)
    suspend fun getAnnotationsSync(docId: Long): List<AnnotationEntity> = dao.getAnnotationsForDocumentSync(docId)

    suspend fun getDocumentById(id: Long): DocumentEntity? = dao.getDocumentById(id)
    suspend fun getDocumentByPath(path: String): DocumentEntity? = dao.getDocumentByPath(path)

    suspend fun upsertDocument(doc: DocumentEntity): Long = dao.insertDocument(doc)
    suspend fun updateDocument(doc: DocumentEntity) = dao.updateDocument(doc)

    suspend fun moveToTrash(doc: DocumentEntity) {
        dao.updateDocument(doc.copy(isTrashed = true, trashedAt = System.currentTimeMillis()))
    }

    suspend fun restoreFromTrash(doc: DocumentEntity) {
        dao.updateDocument(doc.copy(isTrashed = false, trashedAt = null))
    }

    suspend fun deletePermanently(docId: Long) = dao.deleteDocumentPermanently(docId)

    suspend fun purgeTrashOlderThan30Days() {
        val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
        dao.purgeExpiredTrash(System.currentTimeMillis() - thirtyDaysMs)
    }

    suspend fun emptyTrash() = dao.emptyTrash()

    suspend fun createFolder(name: String): Long = dao.insertFolder(FolderEntity(name = name))
    suspend fun deleteFolder(id: Long) = dao.deleteFolder(id)

    suspend fun createTag(name: String): Long = dao.insertTag(TagEntity(name = name))
    suspend fun deleteTag(id: Long) = dao.deleteTag(id)

    suspend fun addBookmark(bookmark: BookmarkEntity): Long = dao.insertBookmark(bookmark)
    suspend fun removeBookmark(id: Long) = dao.deleteBookmark(id)

    suspend fun addAnnotation(annotation: AnnotationEntity): Long = dao.insertAnnotation(annotation)
    suspend fun removeAnnotation(id: Long) = dao.deleteAnnotation(id)
    suspend fun clearAnnotations(docId: Long) = dao.clearAnnotationsForDocument(docId)

    suspend fun savePreset(preset: ToolPresetEntity): Long = dao.insertToolPreset(preset)
    suspend fun deletePreset(id: Long) = dao.deleteToolPreset(id)

    suspend fun insertJob(job: BatchJobEntity): Long = dao.insertBatchJob(job)
    suspend fun updateJob(job: BatchJobEntity) = dao.updateBatchJob(job)
    suspend fun clearFinishedJobs() = dao.clearCompletedJobs()

    suspend fun saveSignature(sig: SignatureEntity): Long = dao.insertSignature(sig)
    suspend fun deleteSignature(id: Long) = dao.deleteSignature(id)
}
