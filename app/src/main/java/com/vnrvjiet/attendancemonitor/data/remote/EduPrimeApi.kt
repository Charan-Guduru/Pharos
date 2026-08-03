package com.vnrvjiet.attendancemonitor.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

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

    @GET("App")
    suspend fun getAppLandingPage(): Response<String>

    @GET("Shared/Widget/STDINFO")
    suspend fun getStdInfo(): Response<String>

    @GET("Academic/Shared/GetStdAttPer")
    suspend fun getAttendanceData(
        @Query("studentId") studentId: String,
        @Query("semId") semId: String = "undefined",
        @Query("_") timestamp: Long = System.currentTimeMillis()
    ): Response<EduPrimeJsonResponse>
}
