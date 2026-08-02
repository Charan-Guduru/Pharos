package com.vnrvjiet.attendancemonitor.data.model

/**
 * Represents a single subject's attendance record parsed from the EduPrime portal.
 */
data class EduPrimeAttendanceRecord(
    val subjectCode: String,
    val subjectName: String,
    val conductedClasses: Int,
    val attendedClasses: Int,
    val attendancePercentage: Double
)
