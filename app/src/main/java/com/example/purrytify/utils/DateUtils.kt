package com.example.purrytify.utils

import java.util.Calendar
import java.util.Date

object DateUtils {

    fun isRefreshNeeded(lastFetchDate: Date?): Boolean {
        if (lastFetchDate == null) return true
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        calendar.time = today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.time
        
        return lastFetchDate.before(startOfToday)
    }

    fun getMillisUntilMidnight(): Long {
        val currentTime = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        return nextMidnight.timeInMillis - currentTime.timeInMillis
    }
}