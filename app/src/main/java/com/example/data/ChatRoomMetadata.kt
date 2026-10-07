package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue

/**
 * Firestore data model representing chat room metadata:
 * - [roomName]: The display name of the chat room.
 * - [creatorId]: The UID of the authenticated user who created the room.
 * - [timestamp]: The server creation timestamp (`Timestamp?` for null-safe deserialization).
 */
data class ChatRoomMetadata(
    val roomId: String = "",
    val roomName: String = "",
    val creatorId: String = "",
    val timestamp: Timestamp? = null
) {
    @get:Exclude
    val timestampMillis: Long
        get() = timestamp?.toDate()?.time ?: 0L

    fun toMap(): Map<String, Any> = mapOf<String, Any?>(
        "roomId" to roomId,
        "roomName" to roomName,
        "creatorId" to creatorId,
        "timestamp" to (timestamp ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}

/**
 * Active member currently present inside a chat room (`/chat_rooms/{roomId}/active_members/{userId}`).
 */
data class RoomActiveMember(
    val userId: String = "",
    val roomId: String = "",
    val memberName: String = "",
    val avatarEmoji: String = "👑",
    val roleBadge: String = "VIP عضو",
    val supabaseLinked: Boolean = true,
    val joinedAt: Timestamp? = null
) {
    @get:Exclude
    val joinedAtMillis: Long
        get() = joinedAt?.toDate()?.time ?: 0L
}

/**
 * Room join or leave transient banner notification (`/chat_rooms/{roomId}/presence_events/{eventId}`):
 * - When `eventType == "JOIN"`, displays `"دخل العضو <memberName>"` and auto-hides.
 * - When `eventType == "LEAVE"`, displays `"خرج العضو <memberName>"` and auto-hides.
 */
data class RoomPresenceBannerEvent(
    val eventId: String = "",
    val roomId: String = "",
    val userId: String = "",
    val memberName: String = "",
    val avatarEmoji: String = "👑",
    val eventType: String = "JOIN", // "JOIN" or "LEAVE"
    val createdAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMillis: Long
        get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val isJoinEvent: Boolean
        get() = eventType.equals("JOIN", ignoreCase = true)

    @get:Exclude
    val bannerText: String
        get() = if (isJoinEvent) {
            "دخل العضو $memberName"
        } else {
            "خرج العضو $memberName"
        }
}

/**
 * Personal statistics summary for a member in their profile view.
 */
data class MemberPersonalStats(
    val roomsCreatedCount: Int = 0,
    val roomsJoinedCount: Int = 0,
    val messagesSentCount: Int = 0,
    val totalActivityScore: Int = 0
)

/**
 * Represents an entry in the member's personal chat and room activity history
 * (`/members/{userId}/chat_history/{entryId}`).
 */
data class MemberChatHistoryEntry(
    val entryId: String = "",
    val userId: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val messageText: String = "",
    val activityType: String = "MESSAGE", // "MESSAGE", "JOIN_ROOM", "CREATE_ROOM", "LEAVE_ROOM"
    val timestamp: Timestamp? = null
) {
    @get:Exclude
    val timestampMillis: Long
        get() = timestamp?.toDate()?.time ?: 0L
}

/**
 * Public anonymous venting post (`/fadfada_posts/{postId}`).
 * Contains NO author name or email so regular members see it strictly as "فضفضة بدون اسم".
 */
data class AnonymousFadfadaPost(
    val postId: String = "",
    val authorId: String = "",
    val content: String = "",
    val moodTag: String = "💭 فضفضة عامة",
    val heartsCount: Int = 0,
    val timestamp: Timestamp? = null
) {
    @get:Exclude
    val timestampMillis: Long
        get() = timestamp?.toDate()?.time ?: 0L
}

/**
 * Admin-only author identity record (`/fadfada_admin_identities/{postId}`).
 * Readable ONLY by the app owner/admin so the admin can see the real name and email of the member who vented.
 */
data class FadfadaAdminIdentity(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val timestamp: Timestamp? = null
) {
    @get:Exclude
    val timestampMillis: Long
        get() = timestamp?.toDate()?.time ?: 0L
}
