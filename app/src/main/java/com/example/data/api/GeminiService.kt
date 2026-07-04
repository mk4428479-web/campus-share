package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    suspend fun getBookRecommendations(interest: String, currentBooks: List<String>): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            Log.d(TAG, "No valid Gemini API key found, returning premium mock recommendations.")
            return@withContext getMockRecommendations(interest)
        }

        val prompt = """
            You are a senior academic advisor and campus study expert for CampusShare.
            A student is interested in: "$interest".
            Current available books on the campus marketplace are: ${currentBooks.joinToString(", ")}.
            
            Provide an inspiring, highly engaging, personalized academic recommendation.
            Suggest 2 specific books or study guides they should lookup on CampusShare or acquire this semester.
            Explain briefly why each fits their goal.
            Keep the tone enthusiastic, encouraging, and modern. 
            Format with clear bullet points. Keep it brief (under 120 words).
        """.trimIndent()

        try {
            // Build request JSON
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Response failed: ${response.code} - ${response.message}")
                    return@withContext getMockRecommendations(interest)
                }

                val bodyString = response.body?.string() ?: return@withContext getMockRecommendations(interest)
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")

                text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API Call failed: ${e.message}", e)
            getMockRecommendations(interest)
        }
    }

    private fun getMockRecommendations(interest: String): String {
        return when {
            interest.contains("computer", ignoreCase = true) || interest.contains("code", ignoreCase = true) || interest.contains("software", ignoreCase = true) -> {
                """
                ✨ **CampusShare AI Coding Recommendations:**
                
                • **Cracking the Coding Interview (Laakmann McDowell)**: Surviving technical interviews is a campus right-of-passage. Alex Rivera listed *CLRS Algorithms* on UC Berkeley campus, which pairs perfectly with this!
                • **Clean Code (Robert C. Martin)**: A must-read before starting your upper-division group projects. Grab notes from *Maya Lin* to clear computer architecture basics first.
                
                *Keep coding and sharing!* 🚀
                """.trimIndent()
            }
            interest.contains("chemistry", ignoreCase = true) || interest.contains("bio", ignoreCase = true) || interest.contains("medical", ignoreCase = true) -> {
                """
                🌿 **CampusShare AI Bio-Chem Recommendations:**
                
                • **Organic Chemistry as a Second Language (David Klein)**: Makes complex reaction mechanisms easy to digest. Check out *Maya Lin's* Organic Chemistry I Master Notes on Stanford campus!
                • **Campbell Biology**: The ultimate biological sciences reference. David Kim on Harvard campus has listed a *Physics Lab Manual* which has helpful overlaps in measurement techniques.
                
                *Best of luck with your pre-med journey!* 🧬
                """.trimIndent()
            }
            interest.contains("math", ignoreCase = true) || interest.contains("calculus", ignoreCase = true) || interest.contains("physics", ignoreCase = true) -> {
                """
                📐 **CampusShare AI Math-Physics Recommendations:**
                
                • **Stewart Calculus - 9th Edition**: The undisputed gold standard for university math. Alex Rivera has this listed in *UC Berkeley* campus with unused WebAssign codes—grab it fast!
                • **University Physics (Sears and Zemansky)**: Excellent diagrams and problem sets. Matches perfectly with the *TI-84 Plus CE Graphing Calculator* listed by Sarah Jenkins on UCLA campus.
                
                *Master those integrations!* 🔭
                """.trimIndent()
            }
            else -> {
                """
                📚 **CampusShare AI General Academic Picks for "$interest":**
                
                • **How to Become a Straight-A Student (Cal Newport)**: Learn the high-efficiency study strategies used by top ivy league students.
                • **Deep Work**: Rules for focused success in a distracted world. Ideal to read alongside the handwritten master lecture summaries listed in our *Notes* category.
                
                *Explore listings campus-wide to unlock your potential!* 🌟
                """.trimIndent()
            }
        }
    }
}
