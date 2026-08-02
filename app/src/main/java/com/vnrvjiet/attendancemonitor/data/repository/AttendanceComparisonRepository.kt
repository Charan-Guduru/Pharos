package com.vnrvjiet.attendancemonitor.data.repository

import android.util.Log
import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceRecordEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.SubjectEntity
import com.vnrvjiet.attendancemonitor.data.local.entity.TimetableEntryEntity
import com.vnrvjiet.attendancemonitor.data.model.AttendanceStatus
import com.vnrvjiet.attendancemonitor.data.model.ComparisonResult
import com.vnrvjiet.attendancemonitor.data.model.ComparisonStatus
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord
import com.vnrvjiet.attendancemonitor.data.model.MatchStatus

class AttendanceComparisonRepository {
    private val TAG = "AttendanceComparison"

    fun compare(
        localRecords: List<AttendanceRecordEntity>,
        timetable: List<TimetableEntryEntity>,
        subjects: List<SubjectEntity>,
        eduPrimeData: List<EduPrimeAttendanceRecord>
    ): List<ComparisonResult> {
        Log.d(TAG, "Starting comparison for ${eduPrimeData.size} subjects")
        
        // 1. Map timetable entry ID to subject code
        val timetableToSubjectCode = timetable.associate { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            entry.id to (subject?.subjectCode ?: "UNKNOWN")
        }

        // 2. Aggregate local Room records by subject code
        val localAggregates = localRecords.groupBy { record ->
            timetableToSubjectCode[record.timetableEntryId] ?: "UNKNOWN"
        }.mapValues { (_, records) ->
            val attended = records.count { it.status == AttendanceStatus.PRESENT }
            val conducted = records.size
            Pair(attended, conducted)
        }

        // 3. Compare with EduPrime data
        return eduPrimeData.map { remote ->
            val local = localAggregates[remote.subjectCode] ?: Pair(0, 0)
            
            val prevAttended = local.first
            val prevConducted = local.second
            val currAttended = remote.attendedClasses
            val currConducted = remote.conductedClasses

            val comparisonStatus = when {
                prevConducted == currConducted && prevAttended == currAttended -> ComparisonStatus.NO_CHANGE
                currConducted > prevConducted -> ComparisonStatus.ATTENDANCE_INCREASED
                currConducted < prevConducted -> ComparisonStatus.ATTENDANCE_DECREASED
                else -> ComparisonStatus.ATTENDANCE_UPDATED
            }

            val matchStatus = if (prevConducted == currConducted && prevAttended == currAttended) {
                MatchStatus.MATCH
            } else if (prevConducted > currConducted) {
                // If local has more records than EduPrime, it's a definite mismatch or out-of-sync
                MatchStatus.MISMATCH
            } else {
                MatchStatus.MISMATCH
            }

            Log.d(TAG, "Subject: ${remote.subjectCode}, Previous: ($prevAttended/$prevConducted), Current: ($currAttended/$currConducted), Result: $comparisonStatus")

            ComparisonResult(
                subjectCode = remote.subjectCode,
                prevConducted = prevConducted,
                currConducted = currConducted,
                prevAttended = prevAttended,
                currAttended = currAttended,
                comparisonStatus = comparisonStatus,
                matchStatus = matchStatus
            )
        }
    }
}
