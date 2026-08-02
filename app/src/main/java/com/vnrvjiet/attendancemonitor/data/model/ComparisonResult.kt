package com.vnrvjiet.attendancemonitor.data.model

enum class ComparisonStatus {
    NO_CHANGE,
    ATTENDANCE_UPDATED,
    ATTENDANCE_DECREASED,
    ATTENDANCE_INCREASED,
    SUBJECT_NOT_FOUND
}

enum class MatchStatus {
    MATCH,
    MISMATCH,
    UNKNOWN
}

data class ComparisonResult(
    val subjectCode: String,
    val prevConducted: Int,
    val currConducted: Int,
    val prevAttended: Int,
    val currAttended: Int,
    val comparisonStatus: ComparisonStatus,
    val matchStatus: MatchStatus
)
