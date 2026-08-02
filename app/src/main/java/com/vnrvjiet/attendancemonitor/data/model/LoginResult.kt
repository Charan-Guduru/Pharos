package com.vnrvjiet.attendancemonitor.data.model

sealed class LoginResult {
    object Success : LoginResult()
    object InvalidCredentials : LoginResult()
    object NetworkUnavailable : LoginResult()
    object Timeout : LoginResult()
    object TokenExtractionFailed : LoginResult()
    data class HttpError(val code: Int) : LoginResult()
    object AuthenticationFailed : LoginResult()
    object UnexpectedResponse : LoginResult()
    data class Error(val message: String) : LoginResult()
}
