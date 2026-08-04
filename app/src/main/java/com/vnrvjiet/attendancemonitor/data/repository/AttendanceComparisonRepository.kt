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

import com.vnrvjiet.attendancemonitor.util.TimeUtils

class AttendanceComparisonRepository {
    private val TAG = "AttendanceComparison"

    fun compare(
        localRecords: List<AttendanceRecordEntity>,
        timetable: List<TimetableEntryEntity>,
        subjects: List<SubjectEntity>,
        eduPrimeData: List<EduPrimeAttendanceRecord>,
        lastSyncedAt: Long = System.currentTimeMillis()
    ): Pair<List<ComparisonResult>, List<AttendanceRecordEntity>> {
        Log.d(TAG, "Starting comparison for ${eduPrimeData.size} subjects")
        
        // 1. Map timetable entry ID to subject details (code + end time)
        val timetableInfo = timetable.associate { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            entry.id to Pair(subject?.subjectCode ?: "UNKNOWN", entry.endTime)
        }

        // 2. Budgeting logic for VerificationState
        val updatedLocalRecords = mutableListOf<AttendanceRecordEntity>()
        val budgets = eduPrimeData.associateBy { it.subjectCode }.toMutableMap()
        
        // Sort local records chronologically per subject (Date + Time Slot)
        val sortedLocal = localRecords.sortedWith { r1, r2 ->
            if (r1.date != r2.date) r1.date.compareTo(r2.date)
            else {
                val t1 = timetableInfo[r1.timetableEntryId]?.second ?: ""
                val t2 = timetableInfo[r2.timetableEntryId]?.second ?: ""
                TimeUtils.parseTimeToMinutes(t1).compareTo(TimeUtils.parseTimeToMinutes(t2))
            }
        }
        
        val subjectConsumption = mutableMapOf<String, Int>() // Track index of record per subject

        sortedLocal.forEach { record ->
            val info = timetableInfo[record.timetableEntryId]
            val code = info?.first ?: "UNKNOWN"
            val endTimeStr = info?.second ?: ""
            val remote = budgets[code]
            
            if (remote == null) {
                updatedLocalRecords.add(record.copy(verificationState = VerificationState.PENDING))
                return@forEach
            }

            // Safety Check: A record cannot be verified if it concluded AFTER the last portal sync
            val recordEndMinutes = TimeUtils.parseTimeToMinutes(endTimeStr)
            val recordEndMillis = record.date + (recordEndMinutes * 60 * 1000L)
            
            if (recordEndMillis > lastSyncedAt) {
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
                    // Filter records of the same subject that were also PRESENT and came before or are this one
                    val previousPresents = sortedLocal.filter { 
                        timetableInfo[it.timetableEntryId]?.first == code && 
                        it.status == AttendanceStatus.PRESENT &&
                        (it.date < record.date || (it.date == record.date && TimeUtils.parseTimeToMinutes(timetableInfo[it.timetableEntryId]?.second ?: "") <= recordEndMinutes))
                    }.size
                    
                    if (remote.attendedClasses >= previousPresents) VerificationState.VERIFIED else VerificationState.MISMATCH
                }
                
                // Case: Recorded BUNK but portal says PRESENT (Verification logic treats it as "Verified as Unexpected")
                record.status == AttendanceStatus.BUNK -> {
                    val previousPresents = sortedLocal.filter { 
                        timetableInfo[it.timetableEntryId]?.first == code && 
                        it.status == AttendanceStatus.PRESENT &&
                        (it.date < record.date || (it.date == record.date && TimeUtils.parseTimeToMinutes(timetableInfo[it.timetableEntryId]?.second ?: "") <= recordEndMinutes))
                    }.size
                    if (remote.attendedClasses > previousPresents) VerificationState.VERIFIED else VerificationState.VERIFIED
                }
                
                else -> VerificationState.VERIFIED 
            }
            
            updatedLocalRecords.add(record.copy(verificationState = newState))
        }

        // 3. Generate UI Comparison Results (using current simple logic)
        val localAggregates = updatedLocalRecords.groupBy { record ->
            timetableInfo[record.timetableEntryId]?.first ?: "UNKNOWN"
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
