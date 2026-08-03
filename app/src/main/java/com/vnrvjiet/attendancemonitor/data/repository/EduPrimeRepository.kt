package com.vnrvjiet.attendancemonitor.data.repository

import android.util.Log
import com.vnrvjiet.attendancemonitor.BuildConfig
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord
import com.vnrvjiet.attendancemonitor.data.model.LoginResult
import com.vnrvjiet.attendancemonitor.data.remote.EduPrimeApi
import com.vnrvjiet.attendancemonitor.data.remote.SessionCookieJar
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class EduPrimeRepository {
    private val TAG = "EduPrimeRepo"
    private val cookieJar = SessionCookieJar()
    private val BASE_URL = "https://automation.vnrvjiet.ac.in/"
    
    private val okHttpClient = OkHttpClient.Builder().apply {
        cookieJar(cookieJar)
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        followRedirects(true)
        followSslRedirects(true)
        
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                var safeMessage = message
                val sensitiveKeys = listOf("xpassword", "password", "Cookie", "Token", "__RequestVerificationToken")
                sensitiveKeys.forEach { key ->
                    if (safeMessage.contains(key, ignoreCase = true)) {
                        safeMessage = "[SCRUBBED]"
                    }
                }
                Log.d("EduPrimeWire", safeMessage)
            }
            logging.level = HttpLoggingInterceptor.Level.BODY
            addInterceptor(logging)
        }
    }.build()

    private val api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EduPrimeApi::class.java)

    suspend fun testLogin(user: String, pass: String, dob: String): LoginResult {
        return try {
            cookieJar.clear()
            performLogin(user, pass, dob)
        } catch (e: UnknownHostException) {
            LoginResult.NetworkUnavailable
        } catch (e: SocketTimeoutException) {
            LoginResult.Timeout
        } catch (e: Exception) {
            Log.e(TAG, "Auth failed: ${e.message}")
            LoginResult.Error(e.message ?: "Unknown error")
        } finally {
            cookieJar.clear()
        }
    }

    private suspend fun performLogin(user: String, pass: String, dob: String): LoginResult {
        if (user.isEmpty() || pass.isEmpty()) {
            return LoginResult.InvalidCredentials
        }

        val loginPageResponse = api.getLoginPage()
        if (!loginPageResponse.isSuccessful) return LoginResult.HttpError(loginPageResponse.code())
        
        val html = loginPageResponse.body() ?: return LoginResult.UnexpectedResponse
        val document = Jsoup.parse(html)
        val token = document.select("input[name=__RequestVerificationToken]").`val`() ?: ""
        val captchaId = document.select("input[name=xCaptchaId]").`val`() ?: ""
        
        if (token.isEmpty()) return LoginResult.TokenExtractionFailed

        val loginResponse = api.login(
            tokenHeader = token,
            referer = BASE_URL,
            origin = BASE_URL.trimEnd('/'),
            token = MultipartBody.Part.createFormData("__RequestVerificationToken", token),
            username = MultipartBody.Part.createFormData("username", user),
            password = MultipartBody.Part.createFormData("xpassword", pass),
            captchaId = MultipartBody.Part.createFormData("xCaptchaId", captchaId),
            captchaAnswer = MultipartBody.Part.createFormData("xCaptchaAnswer", ""),
            dob = MultipartBody.Part.createFormData("xDob", dob)
        )
        
        val responseBody = loginResponse.body() ?: ""
        val isStatusSuccess = responseBody.contains("\"Status\":1")
        val isStatusFailure = responseBody.contains("\"Status\":0") || responseBody.contains("Invalid Username or Password", ignoreCase = true)
        
        return if (isStatusSuccess) {
            LoginResult.Success
        } else if (isStatusFailure) {
            LoginResult.InvalidCredentials
        } else {
            LoginResult.AuthenticationFailed
        }
    }

    suspend fun fetchAttendance(user: String, pass: String, dob: String): Result<List<EduPrimeAttendanceRecord>> {
        return try {
            cookieJar.clear()
            
            val loginResult = performLogin(user, pass, dob)
            if (loginResult !is LoginResult.Success) {
                return Result.failure(Exception("Login failed"))
            }

            val stdInfoResponse = api.getStdInfo()
            if (!stdInfoResponse.isSuccessful) {
                return Result.failure(Exception("Portal communication failed"))
            }
            
            val stdInfoHtml = stdInfoResponse.body() ?: return Result.failure(Exception("Empty response"))
            val studentId = extractStudentId(stdInfoHtml) ?: return Result.failure(Exception("Data inaccessible"))
            
            val attendanceResponse = api.getAttendanceData(studentId)
            if (!attendanceResponse.isSuccessful) {
                return Result.failure(Exception("HTTP ${attendanceResponse.code()}"))
            }

            val jsonResponse = attendanceResponse.body() ?: return Result.failure(Exception("Invalid format"))
            if (jsonResponse.status != 1) {
                return Result.failure(Exception("Request rejected by server"))
            }

            val doc = Jsoup.parse(jsonResponse.data)
            val table = doc.select("table").firstOrNull { table ->
                table.select("tr").firstOrNull()?.text()?.contains("Cumulative", ignoreCase = true) == true
            }

            if (table == null) return Result.success(emptyList())

            val records = mutableListOf<EduPrimeAttendanceRecord>()
            val rows = table.select("tr").drop(1)

            rows.forEach { row ->
                val th = row.selectFirst("th")
                val cells = row.select("td")
                val rowText = row.text()

                if (th?.text()?.contains("Total", ignoreCase = true) == true || rowText.contains("Total", ignoreCase = true)) {
                    return@forEach
                }

                val subjectCode = th?.text()?.trim() ?: ""
                val cumulativeCell = cells.lastOrNull()?.text()?.trim() ?: ""

                if (subjectCode.isNotEmpty() && cumulativeCell.contains("/")) {
                    try {
                        val parts = cumulativeCell.split("/").map { it.trim() }
                        if (parts.size >= 2) {
                            val attended = parts[0].toIntOrNull() ?: 0
                            val conducted = parts[1].toIntOrNull() ?: 0
                            val percentage = if (conducted > 0) (attended.toDouble() / conducted * 100) else 0.0

                            records.add(EduPrimeAttendanceRecord(
                                subjectCode = subjectCode,
                                subjectName = subjectCode,
                                conductedClasses = conducted,
                                attendedClasses = attended,
                                attendancePercentage = percentage
                            ))
                        }
                    } catch (e: Exception) {
                        // Silent skip
                    }
                }
            }

            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.failure(e)
        } finally {
            try { api.logout() } catch (e: Exception) { /* Ignore */ }
            cookieJar.clear()
        }
    }

    private fun extractStudentId(html: String): String? {
        val studentIdRegex = "GetStdAttPer\\?studentId=([^&]+)".toRegex(RegexOption.IGNORE_CASE)
        val match = studentIdRegex.find(html)
        val studentId = match?.groupValues?.get(1)
        
        if (!studentId.isNullOrEmpty()) return studentId
        
        val jsRegex = "studentId\\s*[:=]\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE)
        jsRegex.find(html)?.groupValues?.get(1)?.let { return it }
        
        return null
    }
}
