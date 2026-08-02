package com.vnrvjiet.attendancemonitor.data.remote

import com.google.gson.annotations.SerializedName

data class EduPrimeJsonResponse(
    @SerializedName("Status") val status: Int,
    @SerializedName("Data") val data: String
)
