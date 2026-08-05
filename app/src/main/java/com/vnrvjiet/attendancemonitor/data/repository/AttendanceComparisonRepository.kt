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

import com.vnrvjiet.attendancemonitor.data.local.entity.AttendanceSnapshotEntity
import com.vnrvjiet.attendancemonitor.util.TimeUtils

class AttendanceComparisonRepository {
    private val TAG = "AttendanceComparison"

    fun compare(
        localRecords: List<AttendanceRecordEntity>,
        timetable: List<TimetableEntryEntity>,
        subjects: List<SubjectEntity>,
        eduPrimeData: List<EduPrimeAttendanceRecord>,
        snapshots: List<AttendanceSnapshotEntity> = emptyList(),
        lastSyncedAt: Long = System.currentTimeMillis()
    ): Pair<List<ComparisonResult>, List<AttendanceRecordEntity>> {
        Log.d(TAG, "Starting comparison for ${eduPrimeData.size} subjects")
        
        // 1. Map timetable entry ID to subject details (code + end time)
        val timetableInfo = timetable.associate { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            entry.id to Pair(subject?.subjectCode ?: "UNKNOWN", entry.endTime)
        }

        val snapshotMap = snapshots.associateBy { it.subjectCode }

        // 2. State Machine Logic for VerificationState
        val updatedLocalRecords = mutableListOf<AttendanceRecordEntity>()
        val budgets = eduPrimeData.associateBy { it.subjectCode }
        
        // Group and sort records per subject
        val subjectRecords = localRecords.groupBy { timetableInfo[it.timetableEntryId]?.first ?: "UNKNOWN" }

        subjectRecords.forEach { (code, records) ->
            if (code == "UNKNOWN") {
                updatedLocalRecords.addAll(records)
                return@forEach
            }

            val remote = budgets[code]
            val snapshot = snapshotMap[code]

            // Sort subject records chronologically
            val sorted = records.sortedWith { r1, r2 ->
                if (r1.date != r2.date) r1.date.compareTo(r2.date)
                else {
                    val t1 = timetableInfo[r1.timetableEntryId]?.second ?: ""
                    val t2 = timetableInfo[r2.timetableEntryId]?.second ?: ""
                    TimeUtils.parseTimeToMinutes(t1).compareTo(TimeUtils.parseTimeToMinutes(t2))
                }
            }

            if (remote == null || snapshot == null) {
                // If no remote data or no snapshot, everything stays PENDING
                updatedLocalRecords.addAll(sorted.map { it.copy(verificationState = VerificationState.PENDING) })
                return@forEach
            }

            // Calculate anchor: How many local records existed at the time of the snapshot?
            val countBeforeSnapshot = sorted.count { 
                val endTimeStr = timetableInfo[it.timetableEntryId]?.second ?: ""
                val endMillis = it.date + (TimeUtils.parseTimeToMinutes(endTimeStr) * 60 * 1000L)
                endMillis <= snapshot.lastUpdated 
            }
            
            // Formula: globalPortalIndex = snapshot.conductedClasses - countBeforeSnapshot + localPosition
            val baseIndex = snapshot.conductedClasses - countBeforeSnapshot

            sorted.forEachIndexed { i, record ->
                val localPos = i + 1
                val globalPortalIndex = baseIndex + localPos
                val endTimeStr = timetableInfo[record.timetableEntryId]?.second ?: ""
                val recordEndMillis = record.date + (TimeUtils.parseTimeToMinutes(endTimeStr) * 60 * 1000L)

                val newState = when {
                    // Rule 1: Cannot verify if class ended after last sync
                    recordEndMillis >= lastSyncedAt -> VerificationState.PENDING
                    
                    // Rule 2: Cannot verify if portal hasn't reached this index yet
                    remote.conductedClasses < globalPortalIndex -> VerificationState.PENDING
                    
                    // Rule 3: Actual Comparison
                    record.status == AttendanceStatus.PRESENT -> {
                        // Count how many PRESENT records exist up to this one for this subject
                        val localPresentCount = sorted.take(localPos).count { it.status == AttendanceStatus.PRESENT }
                        
                        // We need the portal to show at least this many presents
                        if (remote.attendedClasses >= localPresentCount) {
                            VerificationState.VERIFIED
                        } else {
                            // High confidence mismatch: Portal conducted this class but attended count didn't increase
                            VerificationState.MISMATCH
                        }
                    }
                    
                    // Rule 4: Other statuses (ABSENT, etc)
                    else -> {
                        // Check if attended count matches expected count for other statuses
                        val localPresentCount = sorted.take(localPos).count { it.status == AttendanceStatus.PRESENT }
                        if (remote.attendedClasses >= localPresentCount) {
                            VerificationState.VERIFIED
                        } else {
                            VerificationState.MISMATCH
                        }
                    }
                }
                updatedLocalRecords.add(record.copy(verificationState = newState))
            }
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
