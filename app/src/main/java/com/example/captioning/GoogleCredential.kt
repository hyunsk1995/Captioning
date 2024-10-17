package com.example.captioning

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials

class GoogleCredential(private val context: Context) {
    fun getAccessToken(): String {
        val inputStream = context.resources.openRawResource(R.raw.project1_436104_ec72547ebdf0)
        val credentials = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        credentials.refreshIfExpired() // 만료된 경우 토큰 새로 고침
        return credentials.accessToken.tokenValue
    }
}