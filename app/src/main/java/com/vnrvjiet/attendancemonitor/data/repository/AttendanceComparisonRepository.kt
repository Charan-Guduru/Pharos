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
import com.vnrvjiet.attendancemonitor.data.model.VerificationState

class AttendanceComparisonRepository {
    private val TAG = "AttendanceComparison"

    fun compare(
        localRecords: List<AttendanceRecordEntity>,
        timetable: List<TimetableEntryEntity>,
        subjects: List<SubjectEntity>,
        eduPrimeData: List<EduPrimeAttendanceRecord>
    ): Pair<List<ComparisonResult>, List<AttendanceRecordEntity>> {
        Log.d(TAG, "Starting comparison for ${eduPrimeData.size} subjects")
        
        // 1. Map timetable entry ID to subject code
        val timetableToSubjectCode = timetable.associate { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            entry.id to (subject?.subjectCode ?: "UNKNOWN")
        }

        // 2. Budgeting logic for VerificationState
        val updatedLocalRecords = mutableListOf<AttendanceRecordEntity>()
        val budgets = eduPrimeData.associateBy { it.subjectCode }.toMutableMap()
        
        // Sort local records chronologically per subject (Date + Timetable Slot)
        val sortedLocal = localRecords.sortedWith(compareBy({ it.date }, { it.timetableEntryId }))
        val subjectConsumption = mutableMapOf<String, Int>() // Track index of record per subject

        sortedLocal.forEach { record ->
            val code = timetableToSubjectCode[record.timetableEntryId] ?: "UNKNOWN"
            val remote = budgets[code]
            
            if (remote == null) {
                updatedLocalRecords.add(record.copy(verificationState = VerificationState.PENDING))
                return@forEach
            }

            val index = subjectConsumption.getOrDefault(code, 0)
            subjectConsumption[code] = index + 1

            val newState = when {
                // If portal has recorded fewer classes than our local index (1-based)
                remote.conductedClasses < (index + 1) -> VerificationState.PENDING
                
                // If we were present and remote attended count covers this record
                record.status == AttendanceStatus.PRESENT -> {
                    // This logic is simplified for the budget method
                    // In a real cumulative system, we check if attended > 0
                    // Since we can't be sure which specific slot was attended, 
                    // we assume chronological verification.
                    val attendedRem = remote.attendedClasses - (sortedLocal.filter { it.date <= record.date && timetableToSubjectCode[it.timetableEntryId] == code && it.status == AttendanceStatus.PRESENT }.size - 1)
                    
                    if (attendedRem > 0) VerificationState.VERIFIED else VerificationState.MISMATCH
                }
                
                // Case 4: Recorded BUNK but portal says PRESENT
                record.status == AttendanceStatus.BUNK -> {
                    val attendedAtThatPoint = sortedLocal.filter { it.date <= record.date && timetableToSubjectCode[it.timetableEntryId] == code && it.status == AttendanceStatus.PRESENT }.size
                    if (remote.attendedClasses > attendedAtThatPoint) VerificationState.VERIFIED else VerificationState.VERIFIED // Logic: Bunked but granted = Verified (Unexpected)
                }
                
                else -> VerificationState.VERIFIED // For Absent/other that match portal's lack of attendance
            }
            
            updatedLocalRecords.add(record.copy(verificationState = newState))
        }

        // 3. Generate UI Comparison Results (using current simple logic)
        val localAggregates = updatedLocalRecords.groupBy { record ->
            timetableToSubjectCode[record.timetableEntryId] ?: "UNKNOWN"
        }.mapValues { (_, records) ->
            val attended = records.count { it.status == AttendanceStatus.PRESENT }
            val conducted = records.size
            Pair(attended, conducted)
        }

        val results = eduPrimeData.map { remote ->
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
            } else {
                MatchStatus.MISMATCH
            }

            ComparisonResult(
                subjectCode = remote.subjectCode,
                subjectName = remote.subjectName,
                prevConducted = prevConducted,
                currConducted = currConducted,
                prevAttended = prevAttended,
                currAttended = currAttended,
                comparisonStatus = comparisonStatus,
                matchStatus = matchStatus
            )
        }
        
        return Pair(results, updatedLocalRecords)
    }
}
