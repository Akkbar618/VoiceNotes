package com.example.voicenotes.data

enum class NoteStatus {
    SYNCED,     // Полностью обработана и сохранена
    DRAFT,      // Сохранена локально, ожидает обработки
    PROCESSING, // В процессе обработки AI
    FAILED      // Ошибка обработки (можно повторить)
}
