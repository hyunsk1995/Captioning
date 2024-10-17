package com.example.captioning

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64

class MakeCaption(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun sendImageToVertexAI(bitmap: Bitmap): String {
        var caption = "Loading..."

        withContext(Dispatchers.IO) {
            val base64Image = encodeImageToBase64(bitmap)
            val jsonPayload = """{
                "instances": [
                    {
                        "image": {
                            "bytesBase64Encoded": "$base64Image"
                        }
                    }
                ],
                "parameters": {
                    "sampleCount": 1,
                    "language": "en"
                  }
            }"""

            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val GC = GoogleCredential(context)
            val token = GC.getAccessToken()

            Log.d("Token", token)
            val request = Request.Builder()
                .url("https://us-central1-aiplatform.googleapis.com/v1/projects/project1-436104/locations/us-central1/publishers/google/models/imagetext:predict")
                .addHeader("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            try {
                val response: Response = client.newCall(request).execute()
                Log.d("Captioning", request.toString())
                val responseString = response.body?.string()
                if (responseString != null) {
                    Log.d("Captioning", responseString)
                }

                // Parse response JSON
                caption = parseCaptionFromResponse(responseString)
                Log.d("Captioning", caption)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return caption
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    private fun parseCaptionFromResponse(response: String?): String {
        response?.let {
            val jsonResponse = Gson().fromJson(it, VertexAIResponse::class.java)
            return jsonResponse.predictions?.get(0)?: "No caption available"
        }
        return "Error generating caption"
    }

    data class VertexAIResponse(
        val predictions: List<String>?
    )
}