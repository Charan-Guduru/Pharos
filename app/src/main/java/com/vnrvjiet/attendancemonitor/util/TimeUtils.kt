package com.vnrvjiet.attendancemonitor.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val date = timeFormatter.parse(timeStr)
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
}
