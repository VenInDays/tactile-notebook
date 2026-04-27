package com.tactile.notebook

import android.app.Application
import com.tactile.notebook.data.NoteDatabase

class TactileNotebookApp : Application() {
    val database: NoteDatabase by lazy { NoteDatabase.getInstance(this) }
}
