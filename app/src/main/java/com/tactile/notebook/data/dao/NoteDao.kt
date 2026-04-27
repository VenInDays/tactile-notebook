package com.tactile.notebook.data.dao

import androidx.room.*
import com.tactile.notebook.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND offsetX BETWEEN :minX AND :maxX AND offsetY BETWEEN :minY AND :maxY")
    fun getNotesInRegion(minX: Float, maxX: Float, minY: Float, maxY: Float): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isArchived = 1 WHERE id = :id")
    suspend fun archiveNote(id: Long)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}
