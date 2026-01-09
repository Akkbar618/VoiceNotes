package com.example.voicenotes.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Форматирует timestamp в человеческую дату.
 * 
 * Примеры:
 * - "Сегодня, 14:30"
 * - "Вчера, 10:15"
 * - "9 янв, 16:00"
 * - "15 дек 2025, 09:30"
 */
object DateFormatter {
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    private val fullFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru"))
    
    fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val date = dateTime.toLocalDate()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        val time = dateTime.format(timeFormatter)
        
        return when (date) {
            today -> "Сегодня, $time"
            yesterday -> "Вчера, $time"
            else -> {
                // Если тот же год — не показывать год
                if (date.year == today.year) {
                    "${dateTime.format(dayMonthFormatter)}, $time"
                } else {
                    "${dateTime.format(fullFormatter)}, $time"
                }
            }
        }
    }
}
