package at.wenzina.client

import at.wenzina.AppConfig
import at.wenzina.model.Project
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoginException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class AtivApiClient(private val config: AppConfig) {

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun login(): String {
        val body = gson.toJson(mapOf("email" to config.apiUsername, "password" to config.apiPassword))
        val request = Request.Builder()
            .url("${config.apiBaseUrl}/api/users/login")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMedia))
            .build()

        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw LoginException("API nicht erreichbar: ${config.apiBaseUrl} – ${e.message}", e)
        }

        val responseBody = response.use {
            if (!it.isSuccessful) {
                throw LoginException("Login fehlgeschlagen (HTTP ${it.code} ${it.message})")
            }
            it.body?.string() ?: throw LoginException("Leere Antwort vom Login-Endpunkt")
        }

        return try {
            gson.fromJson(responseBody, JsonObject::class.java).get("token").asString
        } catch (e: Exception) {
            throw LoginException("Token konnte nicht aus Antwort gelesen werden: ${e.message}", e)
        }
    }

    fun getProjects(authToken: String): List<Project> {
        val request = Request.Builder()
            .url("${config.apiAtlasUrl}/atlas/projects/getProjects?all=true")
            .addHeader("accept", "*/*")
            .addHeader("Authorization", "Bearer $authToken")
            .get()
            .build()

        val responseBody = http.newCall(request).execute().use { it.body!!.string() }
        val root = gson.fromJson(responseBody, JsonObject::class.java)
        val projectsJson = root.getAsJsonArray("projects")
        val type = object : TypeToken<List<Project>>() {}.type
        return gson.fromJson(projectsJson, type)
    }

    fun setProjectImported(authToken: String, projectId: Int) {
        val request = Request.Builder()
            .url("${config.apiAtlasUrl}/atlas/projects/$projectId/imported?all=true")
            .addHeader("accept", "*/*")
            .addHeader("Authorization", "Bearer $authToken")
            .post(ByteArray(0).toRequestBody())
            .build()

        http.newCall(request).execute().use { }
    }
}