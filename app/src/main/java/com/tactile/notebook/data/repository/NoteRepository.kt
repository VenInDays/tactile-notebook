package com.tactile.notebook.data.repository

import com.tactile.notebook.data.dao.NoteDao
import com.tactile.notebook.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class NoteRepository(private val noteDao: NoteDao) {

    fun getActiveNotes(): Flow<List<NoteEntity>> = noteDao.getActiveNotes()

    fun getArchivedNotes(): Flow<List<NoteEntity>> = noteDao.getArchivedNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNotesInRegion(minX: Float, maxX: Float, minY: Float, maxY: Float): Flow<List<NoteEntity>> =
        noteDao.getNotesInRegion(minX, maxX, minY, maxY)

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long {
        // Apply random rotation for tactile paper effect
        val rotation = Random.nextFloat() * 6f - 3f // ±3 degrees
        return noteDao.insertNote(note.copy(rotation = rotation))
    }

    suspend fun updateNote(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun archiveNote(id: Long) = noteDao.archiveNote(id)

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun createNote(
        title: String = "",
        content: String = "",
        toolUsed: String = "pen",
        color: String = "parchment"
    ): Long {
        val offsetX = Random.nextFloat() * 800f - 400f
        val offsetY = Random.nextFloat() * 1200f - 600f
        return insertNote(
            NoteEntity(
                title = title,
                content = content,
                color = color,
                offsetX = offsetX,
                offsetY = offsetY,
                toolUsed = toolUsed
            )
        )
    }
}
