package com.example.voicenotes.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toNoteStatus(value: String) = enumValueOf<NoteStatus>(value)

    @TypeConverter
    fun fromNoteStatus(value: NoteStatus) = value.name
}
