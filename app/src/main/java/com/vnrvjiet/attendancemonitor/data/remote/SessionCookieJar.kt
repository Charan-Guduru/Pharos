package com.vnrvjiet.attendancemonitor.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            cookieStore[cookie.name] = cookie
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore.values.toList()
    }

    fun clear() {
        cookieStore.clear()
    }
}
