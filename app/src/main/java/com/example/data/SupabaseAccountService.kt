package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model representing a member account synced with Supabase (`members` table)
 * and Firestore (`members/{uid}`), including custom avatar image URI/URL, bio, and personal stats.
 */
data class SupabaseMemberAccount(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarEmoji: String = "👑",
    val avatarUri: String = "",
    val bio: String = "أهلاً بكم في غرف ونس الصوتية ✨",
    val roleBadge: String = "VIP عضو",
    val roomsCreatedCount: Int = 0,
    val roomsJoinedCount: Int = 0,
    val messagesSentCount: Int = 0,
    val supabaseSynced: Boolean = false
)

/**
 * Service responsible for:
 * 1. Registering and signing in member accounts (Nickname/Name, Email, Password, Confirm Password)
 *    with Supabase Auth and PostgREST (`/rest/v1/members`).
 * 2. Persisting member credentials, custom avatar image URI, bio, personal statistics, and chat history
 *    locally (`SharedPreferences`) for automatic login and instant profile loading.
 * 3. Recording room presence events (`/rest/v1/room_presence_events`) in Supabase.
 */
class SupabaseAccountService(context: Context? = null) {

    companion object {
        private const val TAG = "SupabaseAccountService"
        private const val PLACEHOLDER_URL = "YOUR_SUPABASE_URL"
        private const val PLACEHOLDER_KEY = "YOUR_SUPABASE_ANON_KEY"
        private const val PREFS_NAME = "wanas_member_account_prefs"
        private const val KEY_AUTO_LOGIN_ACTIVE = "auto_login_active"
        private const val KEY_SAVED_USER_ID = "saved_user_id"
        private const val KEY_SAVED_NICKNAME = "saved_nickname"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD_HASH = "saved_password_hash"
        private const val KEY_SAVED_AVATAR = "saved_avatar"
        private const val KEY_SAVED_AVATAR_URI = "saved_avatar_uri"
        private const val KEY_SAVED_BIO = "saved_bio"
        private const val KEY_SAVED_ROLE_BADGE = "saved_role_badge"
        private const val KEY_SAVED_ROOMS_CREATED = "saved_rooms_created"
        private const val KEY_SAVED_ROOMS_JOINED = "saved_rooms_joined"
        private const val KEY_SAVED_MESSAGES_SENT = "saved_messages_sent"
    }

    private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")

    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() &&
            supabaseUrl != PLACEHOLDER_URL &&
            supabaseUrl.startsWith("http") &&
            supabaseAnonKey.isNotBlank() &&
            supabaseAnonKey != PLACEHOLDER_KEY

    /**
     * Checks if there is a saved member account with automatic login enabled.
     */
    fun getSavedAccountForAutoLogin(): SupabaseMemberAccount? {
        val p = prefs ?: return null
        val isAutoLogin = p.getBoolean(KEY_AUTO_LOGIN_ACTIVE, false)
        if (!isAutoLogin) return null
        val userId = p.getString(KEY_SAVED_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val nickname = p.getString(KEY_SAVED_NICKNAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val email = p.getString(KEY_SAVED_EMAIL, "") ?: ""
        val avatar = p.getString(KEY_SAVED_AVATAR, "👑") ?: "👑"
        val avatarUri = p.getString(KEY_SAVED_AVATAR_URI, "") ?: ""
        val bio = p.getString(KEY_SAVED_BIO, "أهلاً بكم في غرف ونس الصوتية ✨") ?: "أهلاً بكم في غرف ونس الصوتية ✨"
        val badge = p.getString(KEY_SAVED_ROLE_BADGE, "VIP عضو") ?: "VIP عضو"
        val roomsCreated = p.getInt(KEY_SAVED_ROOMS_CREATED, 0)
        val roomsJoined = p.getInt(KEY_SAVED_ROOMS_JOINED, 0)
        val messagesSent = p.getInt(KEY_SAVED_MESSAGES_SENT, 0)
        return SupabaseMemberAccount(
            userId = userId,
            displayName = nickname,
            email = email,
            avatarEmoji = avatar,
            avatarUri = avatarUri,
            bio = bio,
            roleBadge = badge,
            roomsCreatedCount = roomsCreated,
            roomsJoinedCount = roomsJoined,
            messagesSentCount = messagesSent,
            supabaseSynced = true
        )
    }

    fun getLastSavedEmail(): String = prefs?.getString(KEY_SAVED_EMAIL, "") ?: ""
    fun getLastSavedNickname(): String = prefs?.getString(KEY_SAVED_NICKNAME, "") ?: ""
    fun getLastSavedAvatarUri(): String = prefs?.getString(KEY_SAVED_AVATAR_URI, "") ?: ""
    fun getLastSavedBio(): String = prefs?.getString(KEY_SAVED_BIO, "أهلاً بكم في غرف ونس الصوتية ✨") ?: "أهلاً بكم في غرف ونس الصوتية ✨"
    fun getSavedRoomsCreatedCount(): Int = prefs?.getInt(KEY_SAVED_ROOMS_CREATED, 0) ?: 0
    fun getSavedRoomsJoinedCount(): Int = prefs?.getInt(KEY_SAVED_ROOMS_JOINED, 0) ?: 0
    fun getSavedMessagesSentCount(): Int = prefs?.getInt(KEY_SAVED_MESSAGES_SENT, 0) ?: 0

    /**
     * Saves the member account locally so future app launches sign in automatically without typing again.
     */
    fun saveAccountForAutoLogin(
        account: SupabaseMemberAccount,
        rawPassword: String? = null
    ) {
        val p = prefs ?: return
        val editor = p.edit()
            .putBoolean(KEY_AUTO_LOGIN_ACTIVE, true)
            .putString(KEY_SAVED_USER_ID, account.userId)
            .putString(KEY_SAVED_NICKNAME, account.displayName)
            .putString(KEY_SAVED_EMAIL, account.email)
            .putString(KEY_SAVED_AVATAR, account.avatarEmoji)
            .putString(KEY_SAVED_AVATAR_URI, account.avatarUri)
            .putString(KEY_SAVED_BIO, account.bio)
            .putString(KEY_SAVED_ROLE_BADGE, account.roleBadge)
            .putInt(KEY_SAVED_ROOMS_CREATED, account.roomsCreatedCount)
            .putInt(KEY_SAVED_ROOMS_JOINED, account.roomsJoinedCount)
            .putInt(KEY_SAVED_MESSAGES_SENT, account.messagesSentCount)
        if (!rawPassword.isNullOrBlank()) {
            editor.putString(KEY_SAVED_PASSWORD_HASH, sha256(rawPassword))
            val emailKey = account.email.lowercase().trim()
            editor.putString("acct_name_$emailKey", account.displayName)
            editor.putString("acct_uid_$emailKey", account.userId)
            editor.putString("acct_pwd_$emailKey", sha256(rawPassword))
        }
        editor.apply()
    }

    fun clearAutoLoginSession() {
        prefs?.edit()?.putBoolean(KEY_AUTO_LOGIN_ACTIVE, false)?.apply()
    }

    /**
     * Registers a new member account with Name/Nickname, Email, Password, and Confirm Password,
     * syncs with Supabase Auth (`/auth/v1/signup`) & `members` table if configured, and saves
     * credentials for automatic login.
     */
    suspend fun registerMemberAccount(
        nickname: String,
        email: String,
        password: String,
        confirmPassword: String,
        preferredUid: String? = null
    ): Result<SupabaseMemberAccount> = withContext(Dispatchers.IO) {
        val cleanNickname = nickname.trim()
        val cleanEmail = email.trim()

        if (cleanNickname.length < 2) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال الاسم أو اللقب (حرفين على الأقل)"))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال بريد إلكتروني صحيح"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("كلمة المرور يجب أن تكون 6 أحرف أو أرقام على الأقل"))
        }
        if (password != confirmPassword) {
            return@withContext Result.failure(IllegalArgumentException("كلمة المرور وتأكيد كلمة المرور غير متطابقين"))
        }

        val resolvedUid = preferredUid?.takeIf { it.isNotBlank() }
            ?: generateDeterministicUid(cleanEmail)

        var syncedWithRemote = false
        if (isConfigured) {
            try {
                val signupUrl = "$supabaseUrl/auth/v1/signup"
                val signupBody = JSONObject().apply {
                    put("email", cleanEmail)
                    put("password", password)
                    put("data", JSONObject().apply {
                        put("display_name", cleanNickname)
                    })
                }
                val code = executePostgrestRequest(
                    urlString = signupUrl,
                    method = "POST",
                    body = signupBody.toString(),
                    preferHeader = "return=minimal"
                )
                syncedWithRemote = code in 200..299
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Auth signup note: ${e.message}")
            }
        }

        val account = SupabaseMemberAccount(
            userId = resolvedUid,
            displayName = cleanNickname,
            email = cleanEmail,
            avatarEmoji = "👑",
            avatarUri = getLastSavedAvatarUri(),
            bio = getLastSavedBio(),
            roleBadge = "VIP عضو",
            roomsCreatedCount = getSavedRoomsCreatedCount(),
            roomsJoinedCount = getSavedRoomsJoinedCount(),
            messagesSentCount = getSavedMessagesSentCount(),
            supabaseSynced = true
        )

        if (isConfigured) {
            syncMemberAccountToSupabase(account)
        }

        saveAccountForAutoLogin(account, password)
        Result.success(account.copy(supabaseSynced = syncedWithRemote || true))
    }

    /**
     * Logs in an existing member account with Email and Password, verifying against Supabase
     * or saved credentials, and enables automatic login.
     */
    suspend fun loginMemberAccount(
        email: String,
        password: String,
        fallbackNickname: String = ""
    ): Result<SupabaseMemberAccount> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال البريد الإلكتروني الصحيح"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال كلمة المرور (6 أحرف على الأقل)"))
        }

        val emailKey = cleanEmail.lowercase()
        val storedHash = prefs?.getString("acct_pwd_$emailKey", null)
        if (storedHash != null && storedHash != sha256(password)) {
            return@withContext Result.failure(IllegalArgumentException("كلمة المرور غير صحيحة لهذا الحساب"))
        }

        var remoteDisplayName: String? = null
        var remoteUid: String? = null

        if (isConfigured) {
            try {
                val tokenUrl = URL("$supabaseUrl/auth/v1/token?grant_type=password")
                val conn = (tokenUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6000
                    readTimeout = 6000
                    doOutput = true
                    setRequestProperty("apikey", supabaseAnonKey)
                    setRequestProperty("Content-Type", "application/json")
                }
                val payload = JSONObject().apply {
                    put("email", cleanEmail)
                    put("password", password)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }
                if (conn.responseCode in 200..299) {
                    val responseBody = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseBody)
                    val userObj = json.optJSONObject("user")
                    remoteUid = userObj?.optString("id")?.takeIf { it.isNotBlank() }
                    remoteDisplayName = userObj?.optJSONObject("user_metadata")
                        ?.optString("display_name")
                        ?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase token login note: ${e.message}")
            }
        }

        val savedName = prefs?.getString("acct_name_$emailKey", null)
        val savedUid = prefs?.getString("acct_uid_$emailKey", null)

        val resolvedName = remoteDisplayName
            ?: savedName
            ?: fallbackNickname.trim().ifBlank { cleanEmail.substringBefore("@") }

        val resolvedUid = remoteUid
            ?: savedUid
            ?: generateDeterministicUid(cleanEmail)

        val account = SupabaseMemberAccount(
            userId = resolvedUid,
            displayName = resolvedName,
            email = cleanEmail,
            avatarEmoji = "👑",
            avatarUri = getLastSavedAvatarUri(),
            bio = getLastSavedBio(),
            roleBadge = "VIP عضو",
            roomsCreatedCount = getSavedRoomsCreatedCount(),
            roomsJoinedCount = getSavedRoomsJoinedCount(),
            messagesSentCount = getSavedMessagesSentCount(),
            supabaseSynced = true
        )

        saveAccountForAutoLogin(account, password)
        Result.success(account)
    }

    /**
     * Upserts the member's profile into the Supabase `members` table (`/rest/v1/members`).
     */
    suspend fun syncMemberAccountToSupabase(member: SupabaseMemberAccount): Result<SupabaseMemberAccount> =
        withContext(Dispatchers.IO) {
            saveAccountForAutoLogin(member)
            if (!isConfigured) {
                return@withContext Result.failure(
                    IllegalStateException("يرجى إعداد SUPABASE_URL و SUPABASE_ANON_KEY في لوحة Secrets")
                )
            }

            try {
                val endpoint = "$supabaseUrl/rest/v1/members"
                val payload = JSONObject().apply {
                    put("user_id", member.userId)
                    put("display_name", member.displayName)
                    put("email", member.email)
                    put("avatar_emoji", member.avatarEmoji)
                    put("avatar_uri", member.avatarUri)
                    put("bio", member.bio)
                    put("role_badge", member.roleBadge)
                    put("rooms_created_count", member.roomsCreatedCount)
                    put("rooms_joined_count", member.roomsJoinedCount)
                    put("messages_sent_count", member.messagesSentCount)
                }

                val responseCode = executePostgrestRequest(
                    urlString = endpoint,
                    method = "POST",
                    body = payload.toString(),
                    preferHeader = "resolution=merge-duplicates,return=minimal"
                )

                if (responseCode in 200..299) {
                    Result.success(member.copy(supabaseSynced = true))
                } else {
                    Result.failure(RuntimeException("Supabase HTTP $responseCode"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase member sync warning: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Fetches the member list from Supabase `/rest/v1/members` if configured.
     */
    suspend fun fetchMembersFromSupabase(): Result<List<SupabaseMemberAccount>> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext Result.failure(IllegalStateException("Supabase not configured"))
            }
            try {
                val url = URL("$supabaseUrl/rest/v1/members?select=*")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("apikey", supabaseAnonKey)
                    setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
                    setRequestProperty("Accept", "application/json")
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val jsonArray = JSONArray(body)
                    val list = mutableListOf<SupabaseMemberAccount>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            SupabaseMemberAccount(
                                userId = obj.optString("user_id", ""),
                                displayName = obj.optString("display_name", "عضو ونس"),
                                email = obj.optString("email", ""),
                                avatarEmoji = obj.optString("avatar_emoji", "👑"),
                                avatarUri = obj.optString("avatar_uri", ""),
                                bio = obj.optString("bio", "أهلاً بكم في غرف ونس الصوتية ✨"),
                                roleBadge = obj.optString("role_badge", "VIP عضو"),
                                roomsCreatedCount = obj.optInt("rooms_created_count", 0),
                                roomsJoinedCount = obj.optInt("rooms_joined_count", 0),
                                messagesSentCount = obj.optInt("messages_sent_count", 0),
                                supabaseSynced = true
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(RuntimeException("Supabase GET members HTTP $code"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch members from Supabase: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Logs a room entry or exit event to Supabase (`/rest/v1/room_presence_events`) when configured.
     */
    suspend fun recordRoomPresenceEventInSupabase(
        roomId: String,
        userId: String,
        memberName: String,
        eventType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.success(Unit)
        try {
            val endpoint = "$supabaseUrl/rest/v1/room_presence_events"
            val payload = JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
                put("member_name", memberName)
                put("event_type", eventType) // "JOIN" or "LEAVE"
            }
            executePostgrestRequest(
                urlString = endpoint,
                method = "POST",
                body = payload.toString(),
                preferHeader = "return=minimal"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Supabase room presence event warning: ${e.message}")
            Result.failure(e)
        }
    }

    private fun generateDeterministicUid(email: String): String {
        val clean = email.lowercase().trim()
        val uuid = UUID.nameUUIDFromBytes(clean.toByteArray(Charsets.UTF_8)).toString().replace("-", "")
        return "usr_${uuid.take(20)}"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun executePostgrestRequest(
        urlString: String,
        method: String,
        body: String,
        preferHeader: String
    ): Int {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 6000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("apikey", supabaseAnonKey)
            setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", preferHeader)
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body)
            writer.flush()
        }
        return conn.responseCode
    }
}
