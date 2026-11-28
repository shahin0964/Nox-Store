package com.nox.store.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nox.store.data.model.AppItem
import com.nox.store.data.model.DeveloperBadgeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class AdminRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nox_store_admin", Context.MODE_PRIVATE)

    private val _storeNotice = MutableStateFlow<String?>(prefs.getString(KEY_STORE_NOTICE, null))
    val storeNotice: StateFlow<String?> = _storeNotice.asStateFlow()

    private val _badgesMap = MutableStateFlow<Map<String, DeveloperBadgeData>>(loadBadges())
    val badgesMap: StateFlow<Map<String, DeveloperBadgeData>> = _badgesMap.asStateFlow()

    fun isOwner(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        return username.trim().equals(OWNER_USERNAME, ignoreCase = true)
    }

    fun setStoreNotice(notice: String?) {
        val cleanNotice = notice?.trim()?.ifBlank { null }
        prefs.edit().putString(KEY_STORE_NOTICE, cleanNotice).apply()
        _storeNotice.value = cleanNotice
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
        const val OWNER_USERNAME = "shahin0964"
        private const val KEY_STORE_NOTICE = "admin_store_notice"
        private const val KEY_BADGES_JSON = "admin_badges_json"
    }
}
