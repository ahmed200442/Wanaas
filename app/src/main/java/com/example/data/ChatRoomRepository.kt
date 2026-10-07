package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

/**
 * Repository for storing, querying, and observing [ChatRoomMetadata],
 * Supabase-linked member profiles (`/members/{userId}`), personal chat history
 * (`/members/{userId}/chat_history/{entryId}`), live room active members
 * (`/chat_rooms/{roomId}/active_members/{userId}`), transient join/leave banner events
 * (`/chat_rooms/{roomId}/presence_events/{eventId}`), and Anonymous Fadfada posts
 * (`/fadfada_posts/{postId}`) with Admin-Only Author Identity (`/fadfada_admin_identities/{postId}`).
 */
class ChatRoomRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val COLLECTION_CHAT_ROOMS = "chat_rooms"
        const val COLLECTION_MEMBERS = "members"
        const val SUBCOLLECTION_CHAT_HISTORY = "chat_history"
        const val SUBCOLLECTION_ACTIVE_MEMBERS = "active_members"
        const val SUBCOLLECTION_PRESENCE_EVENTS = "presence_events"
        const val COLLECTION_FADFADA_POSTS = "fadfada_posts"
        const val COLLECTION_FADFADA_ADMIN_IDENTITIES = "fadfada_admin_identities"
        const val COLLECTION_PUSH_NOTIFICATIONS = "push_notifications"
        const val COLLECTION_PAYMENT_RECEIPTS = "payment_receipts"
        const val APP_OWNER_EMAIL = "hamadanagy1979@gmail.com"
    }

    private val auth: FirebaseAuth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Creates and stores a new [ChatRoomMetadata] document in Firestore (`chat_rooms/{roomId}`).
     */
    suspend fun createChatRoom(
        roomName: String,
        creatorId: String = requireUserId(),
        customRoomId: String? = null
    ): Result<ChatRoomMetadata> {
        val cleanName = roomName.trim()
        val cleanCreator = creatorId.trim()
        if (cleanName.isEmpty() || cleanCreator.isEmpty()) {
            return Result.failure(IllegalArgumentException("Room name and creator ID must not be empty"))
        }

        val docRef = if (!customRoomId.isNullOrBlank()) {
            db.collection(COLLECTION_CHAT_ROOMS).document(customRoomId)
        } else {
            db.collection(COLLECTION_CHAT_ROOMS).document()
        }

        val payload = mapOf(
            "roomId" to docRef.id,
            "roomName" to cleanName,
            "creatorId" to cleanCreator,
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val snapshot = docRef.get().await()
            val created = snapshot.toObject(
                ChatRoomMetadata::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: ChatRoomMetadata(
                roomId = docRef.id,
                roomName = cleanName,
                creatorId = cleanCreator
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$COLLECTION_CHAT_ROOMS/${docRef.id}")
            Result.failure(e)
        }
    }

    /**
     * Updates the `roomName` of an existing chat room document (`chat_rooms/{roomId}`).
     */
    suspend fun updateChatRoomName(roomId: String, newRoomName: String): Result<Unit> {
        val cleanName = newRoomName.trim()
        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Room name must not be empty"))
        }
        return try {
            db.collection(COLLECTION_CHAT_ROOMS)
                .document(roomId)
                .update("roomName", cleanName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_CHAT_ROOMS/$roomId")
            Result.failure(e)
        }
    }

    /**
     * Fetches a single [ChatRoomMetadata] document by its [roomId].
     */
    suspend fun getChatRoomById(roomId: String): Result<ChatRoomMetadata?> {
        return try {
            val snapshot = db.collection(COLLECTION_CHAT_ROOMS)
                .document(roomId)
                .get()
                .await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                val room = snapshot.toObject(
                    ChatRoomMetadata::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(room)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_CHAT_ROOMS/$roomId")
            Result.failure(e)
        }
    }

    /**
     * Fetches all stored chat rooms for the authenticated creator ordered by timestamp descending.
     */
    suspend fun getChatRoomsForUser(creatorId: String = requireUserId()): Result<List<ChatRoomMetadata>> {
        return try {
            val querySnapshot = db.collection(COLLECTION_CHAT_ROOMS)
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()
            val rooms = querySnapshot.toObjects(
                ChatRoomMetadata::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.timestampMillis }
            Result.success(rooms)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_CHAT_ROOMS)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of [ChatRoomMetadata] scoped to [creatorId].
     */
    fun observeChatRooms(creatorId: String? = auth.currentUser?.uid): Flow<List<ChatRoomMetadata>> =
        callbackFlow {
            val query = if (!creatorId.isNullOrBlank()) {
                db.collection(COLLECTION_CHAT_ROOMS).whereEqualTo("creatorId", creatorId)
            } else {
                db.collection(COLLECTION_CHAT_ROOMS)
            }
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_CHAT_ROOMS)
                    close(error)
                    return@addSnapshotListener
                }
                val rooms = snapshot?.toObjects(
                    ChatRoomMetadata::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.sortedByDescending { it.timestampMillis }.orEmpty()
                trySend(rooms)
            }
            awaitClose { registration.remove() }
        }

    /**
     * Deletes a chat room metadata document from Firestore (`chat_rooms/{roomId}`).
     */
    suspend fun deleteChatRoom(roomId: String): Result<Unit> {
        return try {
            db.collection(COLLECTION_CHAT_ROOMS)
                .document(roomId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$COLLECTION_CHAT_ROOMS/$roomId")
            Result.failure(e)
        }
    }

    /**
     * Saves or updates the authenticated member's public profile (`/members/{userId}`),
     * including custom avatar image URI/URL, bio, and personal statistics.
     */
    suspend fun saveMemberProfile(
        displayName: String,
        avatarEmoji: String = "👑",
        avatarUri: String? = null,
        bio: String? = null,
        roleBadge: String = "VIP عضو",
        roomsCreatedCount: Int? = null,
        roomsJoinedCount: Int? = null,
        messagesSentCount: Int? = null,
        supabaseSynced: Boolean = true,
        userId: String = requireUserId()
    ): Result<SupabaseMemberAccount> {
        val cleanName = displayName.trim().ifBlank { "عضو ونس" }
        val docRef = db.collection(COLLECTION_MEMBERS).document(userId)
        val payload = mapOf<String, Any?>(
            "userId" to userId,
            "displayName" to cleanName,
            "avatarEmoji" to avatarEmoji.ifBlank { "👑" },
            "avatarUri" to avatarUri?.takeIf { it.isNotBlank() },
            "bio" to bio?.takeIf { it.isNotBlank() },
            "roleBadge" to roleBadge.ifBlank { "VIP عضو" },
            "roomsCreatedCount" to roomsCreatedCount,
            "roomsJoinedCount" to roomsJoinedCount,
            "messagesSentCount" to messagesSentCount,
            "supabaseSynced" to supabaseSynced,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            Result.success(
                SupabaseMemberAccount(
                    userId = userId,
                    displayName = cleanName,
                    email = auth.currentUser?.email ?: "",
                    avatarEmoji = avatarEmoji,
                    avatarUri = avatarUri ?: "",
                    bio = bio ?: "أهلاً بكم في غرف ونس الصوتية ✨",
                    roleBadge = roleBadge,
                    roomsCreatedCount = roomsCreatedCount ?: 0,
                    roomsJoinedCount = roomsJoinedCount ?: 0,
                    messagesSentCount = messagesSentCount ?: 0,
                    supabaseSynced = supabaseSynced
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_MEMBERS/$userId")
            Result.failure(e)
        }
    }

    /**
     * Records a personal chat message or room activity entry in `/members/{userId}/chat_history/{entryId}`.
     */
    suspend fun addMemberChatHistoryEntry(
        roomId: String,
        roomName: String,
        messageText: String,
        activityType: String = "MESSAGE",
        userId: String = requireUserId(),
        customEntryId: String? = null
    ): Result<MemberChatHistoryEntry> {
        val cleanRoomName = roomName.trim().ifBlank { "غرفة عامة" }
        val cleanText = messageText.trim()
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("History message text must not be empty"))
        }

        val collectionRef = db.collection(COLLECTION_MEMBERS)
            .document(userId)
            .collection(SUBCOLLECTION_CHAT_HISTORY)

        val docRef = if (!customEntryId.isNullOrBlank()) {
            collectionRef.document(customEntryId)
        } else {
            collectionRef.document()
        }

        val payload = mapOf(
            "entryId" to docRef.id,
            "userId" to userId,
            "roomId" to roomId.ifBlank { "general_room" },
            "roomName" to cleanRoomName,
            "messageText" to cleanText,
            "activityType" to activityType,
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val snapshot = docRef.get().await()
            val created = snapshot.toObject(
                MemberChatHistoryEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: MemberChatHistoryEntry(
                entryId = docRef.id,
                userId = userId,
                roomId = roomId,
                roomName = cleanRoomName,
                messageText = cleanText,
                activityType = activityType
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$COLLECTION_MEMBERS/$userId/$SUBCOLLECTION_CHAT_HISTORY/${docRef.id}")
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of the member's personal chat and room activity history (`/members/{userId}/chat_history`).
     */
    fun observeMemberChatHistory(userId: String = requireUserId()): Flow<List<MemberChatHistoryEntry>> = callbackFlow {
        val path = "$COLLECTION_MEMBERS/$userId/$SUBCOLLECTION_CHAT_HISTORY"
        val query = db.collection(COLLECTION_MEMBERS)
            .document(userId)
            .collection(SUBCOLLECTION_CHAT_HISTORY)
            .whereEqualTo("userId", userId)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            val entries = snapshot?.toObjects(
                MemberChatHistoryEntry::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.sortedByDescending { it.timestampMillis }.orEmpty()
            trySend(entries)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Enters a chat room:
     * 1. Adds the member to `/chat_rooms/{roomId}/active_members/{userId}`
     * 2. Publishes a `"JOIN"` event to `/chat_rooms/{roomId}/presence_events/{eventId}`
     *    so all room participants see `"دخل العضو فلان"` and it auto-hides.
     */
    suspend fun enterRoom(
        roomId: String,
        memberName: String,
        avatarEmoji: String = "👑",
        roleBadge: String = "VIP عضو",
        supabaseLinked: Boolean = true,
        userId: String = requireUserId()
    ): Result<RoomPresenceBannerEvent> {
        val cleanName = memberName.trim().ifBlank { "عضو ونس" }
        val activeMemberRef = db.collection(COLLECTION_CHAT_ROOMS)
            .document(roomId)
            .collection(SUBCOLLECTION_ACTIVE_MEMBERS)
            .document(userId)

        val eventRef = db.collection(COLLECTION_CHAT_ROOMS)
            .document(roomId)
            .collection(SUBCOLLECTION_PRESENCE_EVENTS)
            .document()

        val memberPayload = mapOf(
            "userId" to userId,
            "roomId" to roomId,
            "memberName" to cleanName,
            "avatarEmoji" to avatarEmoji.ifBlank { "👑" },
            "roleBadge" to roleBadge.ifBlank { "VIP عضو" },
            "supabaseLinked" to supabaseLinked,
            "joinedAt" to FieldValue.serverTimestamp()
        )

        val eventPayload = mapOf(
            "eventId" to eventRef.id,
            "roomId" to roomId,
            "userId" to userId,
            "memberName" to cleanName,
            "avatarEmoji" to avatarEmoji.ifBlank { "👑" },
            "eventType" to "JOIN",
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            activeMemberRef.set(memberPayload, SetOptions.merge()).await()
            eventRef.set(eventPayload).await()
            Result.success(
                RoomPresenceBannerEvent(
                    eventId = eventRef.id,
                    roomId = roomId,
                    userId = userId,
                    memberName = cleanName,
                    avatarEmoji = avatarEmoji,
                    eventType = "JOIN"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_CHAT_ROOMS/$roomId/$SUBCOLLECTION_ACTIVE_MEMBERS/$userId")
            Result.failure(e)
        }
    }

    /**
     * Leaves a chat room:
     * 1. Removes the member from `/chat_rooms/{roomId}/active_members/{userId}`
     * 2. Publishes a `"LEAVE"` event to `/chat_rooms/{roomId}/presence_events/{eventId}`
     *    so participants see `"خرج العضو فلان"` and it auto-hides.
     */
    suspend fun leaveRoom(
        roomId: String,
        memberName: String,
        avatarEmoji: String = "👑",
        userId: String = requireUserId()
    ): Result<RoomPresenceBannerEvent> {
        val cleanName = memberName.trim().ifBlank { "عضو ونس" }
        val activeMemberRef = db.collection(COLLECTION_CHAT_ROOMS)
            .document(roomId)
            .collection(SUBCOLLECTION_ACTIVE_MEMBERS)
            .document(userId)

        val eventRef = db.collection(COLLECTION_CHAT_ROOMS)
            .document(roomId)
            .collection(SUBCOLLECTION_PRESENCE_EVENTS)
            .document()

        val eventPayload = mapOf(
            "eventId" to eventRef.id,
            "roomId" to roomId,
            "userId" to userId,
            "memberName" to cleanName,
            "avatarEmoji" to avatarEmoji.ifBlank { "👑" },
            "eventType" to "LEAVE",
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            activeMemberRef.delete().await()
            eventRef.set(eventPayload).await()
            Result.success(
                RoomPresenceBannerEvent(
                    eventId = eventRef.id,
                    roomId = roomId,
                    userId = userId,
                    memberName = cleanName,
                    avatarEmoji = avatarEmoji,
                    eventType = "LEAVE"
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$COLLECTION_CHAT_ROOMS/$roomId/$SUBCOLLECTION_ACTIVE_MEMBERS/$userId")
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of active members currently inside [roomId].
     */
    fun observeRoomActiveMembers(roomId: String): Flow<List<RoomActiveMember>> = callbackFlow {
        val path = "$COLLECTION_CHAT_ROOMS/$roomId/$SUBCOLLECTION_ACTIVE_MEMBERS"
        val query = db.collection(COLLECTION_CHAT_ROOMS)
            .document(roomId)
            .collection(SUBCOLLECTION_ACTIVE_MEMBERS)
            .whereEqualTo("roomId", roomId)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            val members = snapshot?.toObjects(
                RoomActiveMember::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.sortedByDescending { it.joinedAtMillis }.orEmpty()
            trySend(members)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of join/leave events inside [roomId].
     */
    fun observeRoomPresenceEvents(roomId: String): Flow<List<RoomPresenceBannerEvent>> = callbackFlow {
        val path = "$COLLECTION_CHAT_ROOMS/$roomId/$SUBCOLLECTION_PRESENCE_EVENTS"
        val query = db.collection(COLLECTION_CHAT_ROOMS)
            .document(roomId)
            .collection(SUBCOLLECTION_PRESENCE_EVENTS)
            .whereEqualTo("roomId", roomId)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            val events = snapshot?.toObjects(
                RoomPresenceBannerEvent::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.sortedByDescending { it.createdAtMillis }.orEmpty()
            trySend(events)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Creates an anonymous venting post ("فضفضة بدون اسم"):
     * 1. Writes the public post without name or email to `/fadfada_posts/{postId}` so all members see it anonymously.
     * 2. Writes the author's real name and email to `/fadfada_admin_identities/{postId}` which is strictly protected
     *    by Firestore rules so ONLY the app owner/admin (`isAdmin()`) can read or list it.
     */
    suspend fun createAnonymousFadfadaPost(
        content: String,
        moodTag: String = "💭 فضفضة عامة",
        authorName: String,
        authorEmail: String,
        authorId: String = requireUserId(),
        customPostId: String? = null
    ): Result<AnonymousFadfadaPost> {
        val cleanContent = content.trim()
        val cleanMood = moodTag.trim().ifBlank { "💭 فضفضة عامة" }
        val cleanAuthorName = authorName.trim().ifBlank { "عضو ونس" }
        val cleanAuthorEmail = authorEmail.trim().ifBlank { auth.currentUser?.email ?: "member@wanas.app" }

        if (cleanContent.isEmpty()) {
            return Result.failure(IllegalArgumentException("محتوى الفضفضة لا يمكن أن يكون فارغاً"))
        }

        val postDocRef = if (!customPostId.isNullOrBlank()) {
            db.collection(COLLECTION_FADFADA_POSTS).document(customPostId)
        } else {
            db.collection(COLLECTION_FADFADA_POSTS).document()
        }

        val identityDocRef = db.collection(COLLECTION_FADFADA_ADMIN_IDENTITIES).document(postDocRef.id)

        val publicPostPayload = mapOf(
            "postId" to postDocRef.id,
            "authorId" to authorId,
            "content" to cleanContent,
            "moodTag" to cleanMood,
            "heartsCount" to 0,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val adminIdentityPayload = mapOf(
            "postId" to postDocRef.id,
            "authorId" to authorId,
            "authorName" to cleanAuthorName,
            "authorEmail" to cleanAuthorEmail,
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            postDocRef.set(publicPostPayload).await()
            identityDocRef.set(adminIdentityPayload).await()
            val snapshot = postDocRef.get().await()
            val created = snapshot.toObject(
                AnonymousFadfadaPost::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: AnonymousFadfadaPost(
                postId = postDocRef.id,
                authorId = authorId,
                content = cleanContent,
                moodTag = cleanMood,
                heartsCount = 0
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$COLLECTION_FADFADA_POSTS/${postDocRef.id}")
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of anonymous venting posts (`/fadfada_posts`) visible to all members without author PII.
     */
    fun observeAnonymousFadfadaPosts(): Flow<List<AnonymousFadfadaPost>> = callbackFlow {
        val registration = db.collection(COLLECTION_FADFADA_POSTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_FADFADA_POSTS)
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(
                    AnonymousFadfadaPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.sortedByDescending { it.timestampMillis }.orEmpty()
                trySend(posts)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of author identities (`/fadfada_admin_identities`) containing `authorName` and `authorEmail`.
     * Readable ONLY by the app owner/admin (`isAdmin()`).
     */
    fun observeFadfadaAdminIdentities(): Flow<Map<String, FadfadaAdminIdentity>> = callbackFlow {
        val registration = db.collection(COLLECTION_FADFADA_ADMIN_IDENTITIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_FADFADA_ADMIN_IDENTITIES)
                    close(error)
                    return@addSnapshotListener
                }
                val identities = snapshot?.toObjects(
                    FadfadaAdminIdentity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).orEmpty().associateBy { it.postId }
                trySend(identities)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Fetches a single admin identity document for [postId]. Succeeds ONLY if the caller is an admin/owner.
     */
    suspend fun getFadfadaAdminIdentity(postId: String): Result<FadfadaAdminIdentity?> {
        return try {
            val snapshot = db.collection(COLLECTION_FADFADA_ADMIN_IDENTITIES)
                .document(postId)
                .get()
                .await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                val identity = snapshot.toObject(
                    FadfadaAdminIdentity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(identity)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$COLLECTION_FADFADA_ADMIN_IDENTITIES/$postId")
            Result.failure(e)
        }
    }

    /**
     * Increments the supportive heart counter on an anonymous venting post (`/fadfada_posts/{postId}`).
     */
    suspend fun sendHeartToFadfadaPost(postId: String, currentHearts: Int): Result<Unit> {
        return try {
            db.collection(COLLECTION_FADFADA_POSTS)
                .document(postId)
                .update("heartsCount", currentHearts + 1)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_FADFADA_POSTS/$postId")
            Result.failure(e)
        }
    }

    /**
     * Deletes an anonymous venting post (`/fadfada_posts/{postId}`).
     */
    suspend fun deleteFadfadaPost(postId: String): Result<Unit> {
        return try {
            db.collection(COLLECTION_FADFADA_POSTS)
                .document(postId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$COLLECTION_FADFADA_POSTS/$postId")
            Result.failure(e)
        }
    }

    /**
     * Publishes a real-time push notification event (`/push_notifications/{notificationId}`)
     * for either a new room message (`NEW_MESSAGE`) or a friend room invitation (`ROOM_INVITE`).
     */
    suspend fun publishPushNotificationEvent(
        senderName: String,
        recipientQuery: String = "ALL",
        roomId: String,
        roomName: String,
        notificationType: String, // "NEW_MESSAGE" or "ROOM_INVITE"
        messageBody: String,
        senderId: String = requireUserId(),
        customNotificationId: String? = null
    ): Result<PushNotificationEvent> {
        val cleanSenderName = senderName.trim().ifBlank { "عضو ونس" }
        val cleanRecipient = recipientQuery.trim().ifBlank { "ALL" }
        val cleanRoomId = roomId.trim().ifBlank { "general_room" }
        val cleanRoomName = roomName.trim().ifBlank { "غرفة عامة" }
        val cleanBody = messageBody.trim()
        if (cleanBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("محتوى الإشعار لا يمكن أن يكون فارغاً"))
        }

        val docRef = if (!customNotificationId.isNullOrBlank()) {
            db.collection(COLLECTION_PUSH_NOTIFICATIONS).document(customNotificationId)
        } else {
            db.collection(COLLECTION_PUSH_NOTIFICATIONS).document()
        }

        val payload = mapOf(
            "notificationId" to docRef.id,
            "senderId" to senderId,
            "senderName" to cleanSenderName,
            "recipientQuery" to cleanRecipient,
            "roomId" to cleanRoomId,
            "roomName" to cleanRoomName,
            "notificationType" to notificationType,
            "messageBody" to cleanBody,
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val snapshot = docRef.get().await()
            val created = snapshot.toObject(
                PushNotificationEvent::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: PushNotificationEvent(
                notificationId = docRef.id,
                senderId = senderId,
                senderName = cleanSenderName,
                recipientQuery = cleanRecipient,
                roomId = cleanRoomId,
                roomName = cleanRoomName,
                notificationType = notificationType,
                messageBody = cleanBody
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$COLLECTION_PUSH_NOTIFICATIONS/${docRef.id}")
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of push notifications (`/push_notifications`) for new messages and room invitations.
     */
    fun observePushNotifications(): Flow<List<PushNotificationEvent>> = callbackFlow {
        val registration = db.collection(COLLECTION_PUSH_NOTIFICATIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_PUSH_NOTIFICATIONS)
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.toObjects(
                    PushNotificationEvent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.sortedByDescending { it.timestampMillis }.orEmpty()
                trySend(events)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Records a verified payment receipt (`/payment_receipts/{receiptId}`) in Firestore
     * after payment credentials have been strictly validated.
     */
    suspend fun createVerifiedPaymentReceipt(
        memberName: String,
        planId: String,
        planTitle: String,
        amountEgp: Int,
        paymentMethod: String,
        transactionReference: String,
        userId: String = requireUserId(),
        customReceiptId: String? = null
    ): Result<VerifiedPaymentReceipt> {
        val cleanMemberName = memberName.trim().ifBlank { "عضو ونس" }
        val cleanPlanId = planId.trim().ifBlank { "vip_gold" }
        val cleanPlanTitle = planTitle.trim().ifBlank { "باقة VIP الذهبية" }
        val cleanRef = transactionReference.trim()

        if (amountEgp <= 0 || cleanRef.length < 4) {
            return Result.failure(IllegalArgumentException("بيانات إيصال الدفع غير مكتملة"))
        }

        val docRef = if (!customReceiptId.isNullOrBlank()) {
            db.collection(COLLECTION_PAYMENT_RECEIPTS).document(customReceiptId)
        } else {
            db.collection(COLLECTION_PAYMENT_RECEIPTS).document()
        }

        val payload = mapOf(
            "receiptId" to docRef.id,
            "userId" to userId,
            "memberName" to cleanMemberName,
            "planId" to cleanPlanId,
            "planTitle" to cleanPlanTitle,
            "amountEgp" to amountEgp,
            "paymentMethod" to paymentMethod,
            "transactionReference" to cleanRef,
            "verified" to true,
            "timestamp" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            val snapshot = docRef.get().await()
            val created = snapshot.toObject(
                VerifiedPaymentReceipt::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: VerifiedPaymentReceipt(
                receiptId = docRef.id,
                userId = userId,
                memberName = cleanMemberName,
                planId = cleanPlanId,
                planTitle = cleanPlanTitle,
                amountEgp = amountEgp,
                paymentMethod = paymentMethod,
                transactionReference = cleanRef,
                verified = true
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$COLLECTION_PAYMENT_RECEIPTS/${docRef.id}")
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of verified payment receipts for [userId].
     */
    fun observePaymentReceiptsForUser(userId: String = requireUserId()): Flow<List<VerifiedPaymentReceipt>> = callbackFlow {
        val query = db.collection(COLLECTION_PAYMENT_RECEIPTS).whereEqualTo("userId", userId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, COLLECTION_PAYMENT_RECEIPTS)
                close(error)
                return@addSnapshotListener
            }
            val receipts = snapshot?.toObjects(
                VerifiedPaymentReceipt::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.sortedByDescending { it.timestampMillis }.orEmpty()
            trySend(receipts)
        }
        awaitClose { registration.remove() }
    }
}
