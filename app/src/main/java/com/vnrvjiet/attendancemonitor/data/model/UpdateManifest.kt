package com.vnrvjiet.attendancemonitor.data.model

import com.google.gson.annotations.SerializedName

data class UpdateManifest(
    @SerializedName("versionCode")
    val versionCode: Int,
    @SerializedName("versionName")
    val versionName: String,
    @SerializedName("downloadUrl")
    val downloadUrl: String,
    @SerializedName("releaseNotes")
    val releaseNotes: String,
    @SerializedName("releaseDate")
    val releaseDate: String? = null
)
