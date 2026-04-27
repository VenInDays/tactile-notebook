package com.tactile.notebook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.tactile.notebook.data.dao.Converters

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val color: String = "parchment",
    val rotation: Float = 0f,          // Random ±3 degrees for paper effect
    val offsetX: Float = 0f,           // Spatial X position on canvas
    val offsetY: Float = 0f,           // Spatial Y position on canvas
    val isArchived: Boolean = false,
    val toolUsed: String = "pen",      // pen, camera, voice
    val pinned: Boolean = false
)
