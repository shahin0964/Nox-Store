package com.nox.store.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.nox.store.data.model.DeveloperBadgeData
import com.nox.store.data.model.StoreNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AdminRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nox_store_admin", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _storeNotice = MutableStateFlow<StoreNotice?>(loadSavedNotice())
    val storeNotice: StateFlow<StoreNotice?> = _storeNotice.asStateFlow()

    private val _badgesMap = MutableStateFlow<Map<String, DeveloperBadgeData>>(loadBadges())
    val badgesMap: StateFlow<Map<String, DeveloperBadgeData>> = _badgesMap.asStateFlow()

    fun isOwner(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        return username.trim().equals(OWNER_USERNAME, ignoreCase = true)
    }

    private fun loadSavedNotice(): StoreNotice? {
        val raw = prefs.getString(KEY_STORE_NOTICE, null)?.trim()
        if (raw.isNullOrBlank()) return null
        return try {
            if (raw.startsWith("{")) {
                val json = JSONObject(raw)
                val msg = json.optString("message", "").trim()
                if (msg.isBlank()) null
                else StoreNotice(
                    title = json.optString("title", "STORE ANNOUNCEMENT").ifBlank { "STORE ANNOUNCEMENT" },
                    message = msg,
                    type = json.optString("type", "INFO").ifBlank { "INFO" }
                )
            } else {
                StoreNotice(title = "STORE ANNOUNCEMENT", message = raw, type = "INFO")
            }
        } catch (_: Exception) {
            StoreNotice(title = "STORE ANNOUNCEMENT", message = raw, type = "INFO")
        }
    }

    fun setStoreNotice(notice: StoreNotice?) {
        val cleanMsg = notice?.message?.trim()
        if (notice == null || cleanMsg.isNullOrBlank()) {
            prefs.edit().remove(KEY_STORE_NOTICE).apply()
            _storeNotice.value = null
            return
        }
        val normalized = StoreNotice(
            title = notice.title.trim().ifBlank { "STORE ANNOUNCEMENT" },
            message = cleanMsg,
            type = notice.type.trim().ifBlank { "INFO" }
        )
        val json = JSONObject().apply {
            put("title", normalized.title)
            put("message", normalized.message)
            put("type", normalized.type)
        }
        prefs.edit().putString(KEY_STORE_NOTICE, json.toString()).apply()
        _storeNotice.value = normalized
    }

    fun syncRemoteNotice(remoteNotice: StoreNotice?) {
        if (remoteNotice != null && remoteNotice.message.isNotBlank()) {
            setStoreNotice(remoteNotice)
        } else if (remoteNotice == null && _storeNotice.value != null) {
            // Keep local notice only if remote explicitly has null? Better sync remote clearing:
            setStoreNotice(null)
        }
    }

    suspend fun publishNoticeToGitHub(notice: StoreNotice?, token: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        setStoreNotice(notice)
        if (token.isNullOrBlank()) {
            return@withContext Result.success(false) // Saved locally only
        }

        try {
            val getFileUrl = "https://api.github.com/repos/shahin0964/Nox-Store/contents/apps.json"
            val getReq = Request.Builder()
                .url(getFileUrl)
                .header("Authorization", "Bearer $token")
                .header("User-Agent", "NoxStore/1.0.0")
                .build()

            client.newCall(getReq).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API error: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val fileJson = JSONObject(body)
                val sha = fileJson.getString("sha")
                val contentBase64 = fileJson.getString("content").replace("\n", "")
                val decodedJson = String(Base64.decode(contentBase64, Base64.DEFAULT))

                val registryObj = JSONObject(decodedJson)
                if (notice != null && notice.message.isNotBlank()) {
                    val noticeJson = JSONObject().apply {
                        put("title", notice.title.trim().ifBlank { "STORE ANNOUNCEMENT" })
                        put("message", notice.message.trim())
                        put("type", notice.type.trim().ifBlank { "INFO" })
                    }
                    registryObj.put("storeNotice", noticeJson)
                } else {
                    registryObj.remove("storeNotice")
                }
                registryObj.put("lastUpdated", System.currentTimeMillis().toString())

                val updatedContent = registryObj.toString(2)
                val encodedUpdated = Base64.encodeToString(updatedContent.toByteArray(), Base64.NO_WRAP)

                val commitMsg = if (notice != null && notice.message.isNotBlank()) {
                    "chore(admin): update store notice banner"
                } else {
                    "chore(admin): clear store notice banner"
                }

                val putBody = JSONObject().apply {
                    put("message", commitMsg)
                    put("content", encodedUpdated)
                    put("sha", sha)
                }

                val putReq = Request.Builder()
                    .url(getFileUrl)
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", "NoxStore/1.0.0")
                    .put(putBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(putReq).execute().use { putRes ->
                    if (!putRes.isSuccessful) {
                        return@withContext Result.failure(Exception("Failed pushing notice: HTTP ${putRes.code}"))
                    }
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed publishing notice to GitHub", e)
            Result.failure(e)
        }
    }

    fun setDeveloperBadge(username: String, isVerified: Boolean, level: String) {
        val current = _badgesMap.value.toMutableMap()
        val data = DeveloperBadgeData(
            username = username,
            isVerified = isVerified,
            level = level
        )
        current[username.lowercase()] = data
        saveBadges(current)
        _badgesMap.value = current
    }

    fun getDeveloperBadge(username: String): DeveloperBadgeData {
        if (isOwner(username)) {
            return DeveloperBadgeData(
                username = username,
                isVerified = true,
                level = "Store Founder"
            )
        }
        return _badgesMap.value[username.lowercase()] ?: DeveloperBadgeData(
            username = username,
            isVerified = false,
            level = "Starter"
        )
    }

    private fun loadBadges(): Map<String, DeveloperBadgeData> {
        val raw = prefs.getString(KEY_BADGES_JSON, null) ?: return emptyMap()
        val map = mutableMapOf<String, DeveloperBadgeData>()
        try {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.getJSONObject(key)
                map[key.lowercase()] = DeveloperBadgeData(
                    username = obj.getString("username"),
                    isVerified = obj.optBoolean("isVerified", false),
                    level = obj.optString("level", "Starter")
                )
            }
        } catch (_: Exception) {}
        return map
    }

    private fun saveBadges(map: Map<String, DeveloperBadgeData>) {
        try {
            val json = JSONObject()
            map.forEach { (key, data) ->
                val obj = JSONObject().apply {
                    put("username", data.username)
                    put("isVerified", data.isVerified)
                    put("level", data.level)
                }
                json.put(key.lowercase(), obj)
            }
            prefs.edit().putString(KEY_BADGES_JSON, json.toString()).apply()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "AdminRepository"
        const val OWNER_USERNAME = "shahin0964"
        private const val KEY_STORE_NOTICE = "admin_store_notice"
        private const val KEY_BADGES_JSON = "admin_badges_json"
    }
}
