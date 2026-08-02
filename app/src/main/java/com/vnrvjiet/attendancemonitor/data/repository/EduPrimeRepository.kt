package com.vnrvjiet.attendancemonitor.data.repository

import android.util.Log
import com.vnrvjiet.attendancemonitor.BuildConfig
import com.vnrvjiet.attendancemonitor.data.model.EduPrimeRecord
import com.vnrvjiet.attendancemonitor.data.model.LoginResult
import com.vnrvjiet.attendancemonitor.data.remote.EduPrimeApi
import com.vnrvjiet.attendancemonitor.data.remote.SessionCookieJar
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup
import retrofit2.Retrofit
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

    suspend fun fetchAttendance(user: String, pass: String, dob: String): Result<List<EduPrimeRecord>> {
        return try {
            cookieJar.clear()
            val loginResult = performLogin(user, pass, dob)
            if (loginResult !is LoginResult.Success) {
                return Result.failure(Exception("Login failed: $loginResult"))
            }

            val response = api.getAttendancePage()
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP Error ${response.code()}"))
            }

            val html = response.body() ?: return Result.failure(Exception("Empty attendance body"))
            val doc = Jsoup.parse(html)
            
            // Look for the attendance table. Common pattern in EduPrime.
            val table = doc.select("table#dgAttendance, table.attendance-table").first()
            if (table == null) {
                Log.w(TAG, "Attendance table not found in HTML")
                return Result.success(emptyList())
            }

            val rows = table.select("tr").drop(1) // Skip header
            val records = rows.mapNotNull { row ->
                val cells = row.select("td")
                if (cells.size >= 5) {
                    EduPrimeRecord(
                        date = cells[1].text(),
                        period = cells[2].text(),
                        subject = cells[3].text(),
                        status = cells[4].text()
                    )
                } else null
            }

            Result.success(records)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching attendance", e)
            Result.failure(e)
        } finally {
            cookieJar.clear()
        }
    }
}
