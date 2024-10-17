package com.example.captioning

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore.Audio
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Base64
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class TextToSpeech (private val context: Context) {
    private val client = OkHttpClient()
    private val charset = Charsets.UTF_8
    val mediaPlayer = MediaPlayer()

    suspend fun textToSpeech(text: String): ByteArray {
        var speech = ByteArray(10)
        val jsonPayload = """{
            "input": {
                "text": "$text"
            },
            "voice": {
                "languageCode": "en-US",
                "name": "en-US-Wavenet-D",
                "ssmlGender": "MALE"
            },
            "audioConfig": {
                "audioEncoding": "MP3"
            }
        }"""

        val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val GC = GoogleCredential(context)
        val token = GC.getAccessToken()
        val request = Request.Builder()
            .url("https://texttospeech.googleapis.com/v1/text:synthesize")
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        try {
            val response: Response = client.newCall(request).execute()
//            Log.d("Captioning", request.toString())
            val responseString = response.body?.string()

            if (responseString != null) {
                val gson = Gson()
                val ttsResponse = gson.fromJson(responseString, TextToSpeechResponse::class.java)

                try {
                    // Base64로 인코딩된 오디오 데이터 디코딩
                    val decodedAudioContent = Base64.getDecoder().decode(ttsResponse.audioContent)
                    val audioUri = byteArrayToUri(context, decodedAudioContent)

                    // MediaPlayer로 오디오 재생
                    if (audioUri != null) {
                        mediaPlayer.reset()  // MediaPlayer를 초기화
                        mediaPlayer.setDataSource(context, audioUri)
                        mediaPlayer.setOnPreparedListener {
                            it.start()  // 준비 완료 후 재생
                        }
                        mediaPlayer.prepareAsync()  // 비동기로 준비
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                Log.e("TextToSpeech", "Response body is null")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return speech
    }
    data class TextToSpeechResponse(
        val audioContent: String
    )

    private fun byteArrayToUri(context: Context, audioBytes: ByteArray): Uri? {
        // 1. 앱의 외부 파일 디렉토리(음악 폴더) 안에 파일 저장
        val audioFile = File(context.cacheDir, "temp_audio.mp3")  // 내부 캐시 디렉토리에 저장

        try {
            // 2. ByteArray를 파일로 저장
            FileOutputStream(audioFile).use { fos ->
                fos.write(audioBytes)
            }

            // 3. 파일을 Uri로 변환
            return Uri.fromFile(audioFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}