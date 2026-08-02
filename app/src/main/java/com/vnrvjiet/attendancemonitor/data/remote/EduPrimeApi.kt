package com.vnrvjiet.attendancemonitor.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface EduPrimeApi {
    @GET("/")
    suspend fun getLoginPage(): Response<String>

    @Multipart
    @POST("Login")
    suspend fun login(
        @Header("RequestVerificationToken") tokenHeader: String,
        @Header("Referer") referer: String,
        @Header("Origin") origin: String,
        @Header("Accept") accept: String = "application/json, text/javascript, */*; q=0.01",
        @Header("X-Requested-With") requestedWith: String = "XMLHttpRequest",
        @Part token: MultipartBody.Part,
        @Part username: MultipartBody.Part,
        @Part password: MultipartBody.Part,
        @Part captchaId: MultipartBody.Part,
        @Part captchaAnswer: MultipartBody.Part,
        @Part dob: MultipartBody.Part
    ): Response<String>

    @GET("Login/Logoff")
    suspend fun logout(): Response<String>

    @GET("Student/StudentAttendance/StudentAttendance")
    suspend fun getAttendancePage(): Response<String>
}
