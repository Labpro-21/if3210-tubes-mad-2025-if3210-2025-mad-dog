package com.example.purrytify.utils

import android.util.Log
import androidx.room.TypeConverter
import java.util.Date

class DateConverter {
    private val tag = "DateConverter"
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

}