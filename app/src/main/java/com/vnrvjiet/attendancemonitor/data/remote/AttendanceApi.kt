package com.vnrvjiet.attendancemonitor.data.remote

import retrofit2.http.GET

interface AttendanceApi {
    @GET("attendance")
    suspend fun fetchAttendance(): List<Any> // Placeholder
}
