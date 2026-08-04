package com.vnrvjiet.attendancemonitor.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val timeFormatter12 = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val date = timeFormatter12.parse(timeStr)
            val calendar = Calendar.getInstance().apply { 
                if (date != null) time = date 
            }
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentTimeInMinutes(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    fun formatTo12Hour(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
