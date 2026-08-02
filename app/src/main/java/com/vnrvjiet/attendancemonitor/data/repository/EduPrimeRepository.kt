package com.vnrvjiet.attendancemonitor.data.repository

import android.util.Log
import com.vnrvjiet.attendancemonitor.BuildConfig
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeAttendanceRecord
import com.vnrvjiet.attendancemonitor.data.model.LoginResult
import com.vnrvjiet.attendancemonitor.data.remote.EduPrimeApi
import com.vnrvjiet.attendancemonitor.data.remote.EduPrimeJsonResponse
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
    private val TAG = "EduPrimeAuth"
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
                // Scrub sensitive data from logs
                var safeMessage = message
                val sensitiveKeys = listOf("xpassword", "password", "Cookie", "Token", "__RequestVerificationToken")
                sensitiveKeys.forEach { key ->
                    if (safeMessage.contains(key, ignoreCase = true)) {
                        safeMessage = "[SCRUBBED SENSITIVE DATA]"
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
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing permissions?", e)
            LoginResult.Error("Security: ${e.message}")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "UnknownHostException: No internet or DNS failure", e)
            LoginResult.NetworkUnavailable
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "SocketTimeoutException: Server timed out", e)
            LoginResult.Timeout
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IOException during network call", e)
            LoginResult.Error("IO: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during auth", e)
            LoginResult.Error(e.message ?: "Unknown error")
        } finally {
            cookieJar.clear()
        }
    }

    private suspend fun performLogin(user: String, pass: String, dob: String): LoginResult {
        if (user.isEmpty() || pass.isEmpty()) {
            return LoginResult.InvalidCredentials
        }

        // 1. GET Login Page for Cookies & Token
        val loginPageResponse = api.getLoginPage()
        if (!loginPageResponse.isSuccessful) return LoginResult.HttpError(loginPageResponse.code())
        
        val html = loginPageResponse.body() ?: return LoginResult.UnexpectedResponse
        
        // 2. Extract Token and Captcha Info
        val document = Jsoup.parse(html)
        val token = document.select("input[name=__RequestVerificationToken]").`val`() ?: ""
        val captchaId = document.select("input[name=xCaptchaId]").`val`() ?: ""
        
        if (token.isEmpty()) return LoginResult.TokenExtractionFailed

        // 3. POST Login (Multipart)
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
        
        // Success Detection: Server returns {"Status":1, ...} for success
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
            
            // 1. Login
            val loginResult = performLogin(user, pass, dob)
            if (loginResult !is LoginResult.Success) {
                return Result.failure(Exception("Login failed: $loginResult"))
            }

            // 2. Obtain Student ID from STDINFO widget
            val stdInfoResponse = api.getStdInfo()
            if (!stdInfoResponse.isSuccessful) {
                return Result.failure(Exception("Failed to load STDINFO: ${stdInfoResponse.code()}"))
            }
            Log.d(TAG, "✓ STDINFO request succeeded")
            
            val stdInfoHtml = stdInfoResponse.body() ?: return Result.failure(Exception("Empty STDINFO body"))
            val studentId = extractStudentId(stdInfoHtml) ?: return Result.failure(Exception("Student ID not found in portal"))
            
            Log.d(TAG, "✓ studentId extracted")
            Log.d(TAG, "studentId length: ${studentId.length}")

            // 3. Fetch Attendance JSON Data
            val attendanceResponse = api.getAttendanceData(studentId)
            if (!attendanceResponse.isSuccessful) {
                return Result.failure(Exception("HTTP Error ${attendanceResponse.code()}"))
            }

            val jsonResponse = attendanceResponse.body() ?: return Result.failure(Exception("Empty JSON response"))
            Log.d(TAG, "JSON Status: ${jsonResponse.status}")
            
            if (jsonResponse.status != 1) {
                return Result.failure(Exception("Server returned status ${jsonResponse.status}"))
            }

            // 4. Parse HTML fragment in Data field
            val htmlFragment = jsonResponse.data
            val doc = Jsoup.parse(htmlFragment)
            
            // Locate the table by finding the one whose first row contains "Cumulative"
            val table = doc.select("table").firstOrNull { table ->
                table.select("tr").firstOrNull()?.text()?.contains("Cumulative", ignoreCase = true) == true
            }

            if (table == null) {
                Log.w(TAG, "Attendance table with 'Cumulative' header not found")
                return Result.success(emptyList())
            }

            val records = mutableListOf<EduPrimeAttendanceRecord>()
            val rows = table.select("tr").drop(1) // Skip the first header row

            rows.forEach { row ->
                val th = row.selectFirst("th")
                val cells = row.select("td")
                val rowText = row.text()

                // Skip "Total" row
                if (th?.text()?.contains("Total", ignoreCase = true) == true || 
                    rowText.contains("Total", ignoreCase = true)) {
                    return@forEach
                }

                // Subject Code is in TH
                val subjectCode = th?.text()?.trim() ?: ""
                
                // Cumulative attendance is in the last TD (usually the 2nd TD in this specific structure)
                val cumulativeCell = cells.lastOrNull()?.text()?.trim() ?: ""

                if (subjectCode.isNotEmpty() && cumulativeCell.contains("/")) {
                    try {
                        val parts = cumulativeCell.split("/").map { it.trim() }
                        if (parts.size >= 2) {
                            val attended = parts[0].toIntOrNull() ?: 0
                            val conducted = parts[1].toIntOrNull() ?: 0
                            val percentage = if (conducted > 0) (attended.toDouble() / conducted * 100) else 0.0

                            Log.d(TAG, "Parsed Subject: $subjectCode, Attended: $attended, Conducted: $conducted")
                            
                            records.add(EduPrimeAttendanceRecord(
                                subjectCode = subjectCode,
                                subjectName = subjectCode, // Using code as name since name is not in this specific row structure
                                conductedClasses = conducted,
                                attendedClasses = attended,
                                attendancePercentage = percentage
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing row: $rowText", e)
                    }
                }
            }

            Log.d(TAG, "Total subjects parsed: ${records.size}")
            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance", e)
            Result.failure(e)
        } finally {
            try { api.logout() } catch (e: Exception) { /* Ignore */ }
            cookieJar.clear()
        }
    }

    private fun extractStudentId(html: String): String? {
        // Robust extraction from STDINFO: GetStdAttPer?studentId=...&semId=
        val studentIdRegex = "GetStdAttPer\\?studentId=([^&]+)".toRegex(RegexOption.IGNORE_CASE)
        val match = studentIdRegex.find(html)
        val studentId = match?.groupValues?.get(1)
        
        if (!studentId.isNullOrEmpty()) {
            return studentId
        }
        
        // Fallback: try direct variable search
        val jsRegex = "studentId\\s*[:=]\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE)
        jsRegex.find(html)?.groupValues?.get(1)?.let { return it }
        
        return null
    }
}
