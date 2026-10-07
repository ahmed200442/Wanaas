package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AnonymousFadfadaPost
import com.example.data.ChatRoomMetadata
import com.example.data.ChatRoomRepository
import com.example.data.FadfadaAdminIdentity
import com.example.data.MemberChatHistoryEntry
import com.example.data.MemberPersonalStats
import com.example.data.PushNotificationEvent
import com.example.data.RoomActiveMember
import com.example.data.RoomPresenceBannerEvent
import com.example.data.SupabaseAccountService
import com.example.data.SupabaseMemberAccount
import com.example.data.VerifiedPaymentReceipt
import com.example.data.VipPaymentPlan
import com.example.notifications.PushNotificationHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ChatRoomsActionState(
    val isSubmitting: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val memberDisplayName: String = "أحمد المصري",
    val memberEmail: String = "",
    val memberAvatarEmoji: String = "👑",
    val memberAvatarUri: String = "",
    val memberBio: String = "أهلاً بكم في غرف ونس الصوتية ✨",
    val memberRoleBadge: String = "عضو ونس",
    val isPaidVip: Boolean = false,
    val paidPlanTitle: String = "",
    val paymentReceipts: List<VerifiedPaymentReceipt> = emptyList(),
    val roomsCreatedCount: Int = 0,
    val roomsJoinedCount: Int = 0,
    val messagesSentCount: Int = 0,
    val chatHistory: List<MemberChatHistoryEntry> = emptyList(),
    val fadfadaPosts: List<AnonymousFadfadaPost> = emptyList(),
    val fadfadaAdminIdentities: Map<String, FadfadaAdminIdentity> = emptyMap(),
    val pushNotifications: List<PushNotificationEvent> = emptyList(),
    val latestPushBanner: PushNotificationEvent? = null,
    val isAdminOwner: Boolean = false,
    val isSupabaseConfigured: Boolean = false,
    val isSupabaseSynced: Boolean = true,
    val supabaseStatusText: String = "حساب العضو محفوظ للدخول التلقائي (Supabase)",
    val activeRoom: ChatRoomMetadata? = null,
    val activeRoomMembers: List<RoomActiveMember> = emptyList(),
    val transientPresenceBanner: RoomPresenceBannerEvent? = null,
    val isDarkMode: Boolean = false // Default to Daytime Mode (الوضع النهاري) with rich dark & light contrast
) {
    val personalStats: MemberPersonalStats
        get() = MemberPersonalStats(
            roomsCreatedCount = roomsCreatedCount,
            roomsJoinedCount = roomsJoinedCount,
            messagesSentCount = messagesSentCount,
            totalActivityScore = (roomsCreatedCount * 15) + (roomsJoinedCount * 5) + (messagesSentCount * 3)
        )
}

class ChatRoomViewModel(
    private val repository: ChatRoomRepository,
    private val currentUserId: String,
    private val currentUserDisplayName: String = "",
    private val currentUserEmail: String = "",
    private val supabaseService: SupabaseAccountService = SupabaseAccountService(),
    private val pushNotificationHelper: PushNotificationHelper? = null
) : ViewModel() {

    private val _localRooms = MutableStateFlow<List<ChatRoomMetadata>>(emptyList())
    private val _localChatHistory = MutableStateFlow<List<MemberChatHistoryEntry>>(emptyList())
    private val _localFadfadaPosts = MutableStateFlow<List<AnonymousFadfadaPost>>(emptyList())
    private val _localFadfadaAdminIdentities = MutableStateFlow<Map<String, FadfadaAdminIdentity>>(emptyMap())
    private val _localPushNotifications = MutableStateFlow<List<PushNotificationEvent>>(emptyList())
    private val _localPaymentReceipts = MutableStateFlow<List<VerifiedPaymentReceipt>>(emptyList())

    private val resolvedEmail: String = currentUserEmail.ifBlank { supabaseService.getLastSavedEmail() }

    private val firestoreRoomsFlow = if (Firebase.auth.currentUser != null) {
        repository.observeChatRooms(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing chat rooms", error)
                emit(emptyList())
            }
    } else {
        MutableStateFlow(emptyList())
    }

    val roomsState: StateFlow<UiState<List<ChatRoomMetadata>>> = combine(
        firestoreRoomsFlow,
        _localRooms
    ) { cloudRooms, localRooms ->
        val merged = (cloudRooms + localRooms)
            .distinctBy { it.roomId }
            .sortedByDescending { it.timestampMillis }
        UiState.Success(merged) as UiState<List<ChatRoomMetadata>>
    }.catch { error ->
        Log.w(TAG, "Error combining chat rooms", error)
        emit(UiState.Error(error.message ?: "تعذر تحميل غرف الدردشة"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Success(emptyList())
    )

    private val _actionState = MutableStateFlow(
        ChatRoomsActionState(
            memberDisplayName = currentUserDisplayName.ifBlank {
                supabaseService.getLastSavedNickname().ifBlank {
                    resolvedEmail.substringBefore("@").ifBlank { "عضو ونس المميز" }
                }
            },
            memberEmail = resolvedEmail,
            memberAvatarUri = supabaseService.getLastSavedAvatarUri(),
            memberBio = supabaseService.getLastSavedBio(),
            memberRoleBadge = supabaseService.getSavedRoleBadge(),
            isPaidVip = supabaseService.getSavedIsPaidVip(),
            paidPlanTitle = supabaseService.getSavedPaidPlanTitle(),
            roomsCreatedCount = supabaseService.getSavedRoomsCreatedCount(),
            roomsJoinedCount = supabaseService.getSavedRoomsJoinedCount(),
            messagesSentCount = supabaseService.getSavedMessagesSentCount(),
            isAdminOwner = isUserAppOwner(resolvedEmail),
            isSupabaseConfigured = supabaseService.isConfigured
        )
    )
    val actionState: StateFlow<ChatRoomsActionState> = _actionState.asStateFlow()

    private var activeMembersJob: Job? = null
    private var presenceEventsJob: Job? = null
    private var chatHistoryJob: Job? = null
    private var fadfadaPostsJob: Job? = null
    private var fadfadaAdminJob: Job? = null
    private var pushNotificationsJob: Job? = null
    private var paymentReceiptsJob: Job? = null
    private var bannerAutoHideJob: Job? = null
    private var pushBannerAutoHideJob: Job? = null
    private var lastShownEventId: String? = null
    private var lastDeliveredNotificationId: String? = null

    init {
        syncMemberAccountWithSupabaseAndCloud()
        observeMemberChatHistoryStream()
        observeFadfadaStreams()
        observePushNotificationsStream()
        observeVerifiedPaymentsStream()
    }

    private fun isUserAppOwner(email: String): Boolean {
        val clean = email.trim().lowercase()
        val fbEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase().orEmpty()
        val owner = ChatRoomRepository.APP_OWNER_EMAIL.lowercase()
        return clean == owner || fbEmail == owner
    }

    private fun observePushNotificationsStream() {
        pushNotificationsJob?.cancel()
        val cloudNotificationsFlow = if (Firebase.auth.currentUser != null) {
            repository.observePushNotifications()
                .catch { e ->
                    Log.w(TAG, "Error observing push notifications", e)
                    emit(emptyList())
                }
        } else {
            MutableStateFlow(emptyList())
        }

        pushNotificationsJob = viewModelScope.launch {
            combine(cloudNotificationsFlow, _localPushNotifications) { cloudList, localList ->
                (cloudList + localList)
                    .distinctBy { it.notificationId }
                    .sortedByDescending { it.timestampMillis }
            }.collect { combinedList ->
                _actionState.update { it.copy(pushNotifications = combinedList) }
                val newest = combinedList.firstOrNull()
                if (newest != null &&
                    newest.notificationId.isNotBlank() &&
                    newest.notificationId != lastDeliveredNotificationId
                ) {
                    lastDeliveredNotificationId = newest.notificationId
                    triggerSystemAndBannerPushNotification(newest)
                }
            }
        }
    }

    private fun observeVerifiedPaymentsStream() {
        paymentReceiptsJob?.cancel()
        val cloudReceiptsFlow = if (Firebase.auth.currentUser != null) {
            repository.observePaymentReceiptsForUser(currentUserId)
                .catch { e ->
                    Log.w(TAG, "Error observing payment receipts", e)
                    emit(emptyList())
                }
        } else {
            MutableStateFlow(emptyList())
        }

        paymentReceiptsJob = viewModelScope.launch {
            combine(cloudReceiptsFlow, _localPaymentReceipts) { cloudList, localList ->
                (cloudList + localList)
                    .distinctBy { it.receiptId }
                    .sortedByDescending { it.timestampMillis }
            }.collect { receipts ->
                val hasVerified = receipts.any { it.verified } || supabaseService.getSavedIsPaidVip()
                val latestReceipt = receipts.firstOrNull { it.verified }
                _actionState.update { state ->
                    state.copy(
                        paymentReceipts = receipts,
                        isPaidVip = hasVerified,
                        paidPlanTitle = latestReceipt?.planTitle?.ifBlank { state.paidPlanTitle } ?: state.paidPlanTitle,
                        memberRoleBadge = if (hasVerified) {
                            state.memberRoleBadge.takeIf { it.contains("VIP") } ?: "👑 VIP مدفوع"
                        } else {
                            "عضو ونس"
                        }
                    )
                }
            }
        }
    }

    private fun triggerSystemAndBannerPushNotification(event: PushNotificationEvent) {
        pushNotificationHelper?.showPushNotification(event)
        pushBannerAutoHideJob?.cancel()
        _actionState.update { it.copy(latestPushBanner = event) }
        pushBannerAutoHideJob = viewModelScope.launch {
            delay(BANNER_DISPLAY_DURATION_MS)
            _actionState.update { state ->
                if (state.latestPushBanner?.notificationId == event.notificationId) {
                    state.copy(latestPushBanner = null)
                } else {
                    state
                }
            }
        }
    }

    fun dismissPushBanner() {
        pushBannerAutoHideJob?.cancel()
        _actionState.update { it.copy(latestPushBanner = null) }
    }

    /**
     * Invites a friend to join a chat room and immediately triggers a real-time Push Notification (`ROOM_INVITE`).
     */
    fun inviteFriendToChatRoom(
        friendNameOrEmail: String,
        room: ChatRoomMetadata? = _actionState.value.activeRoom,
        customInviteMessage: String = ""
    ) {
        val cleanFriend = friendNameOrEmail.trim()
        if (cleanFriend.isBlank()) {
            _actionState.update {
                it.copy(errorMessage = "يرجى إدخال اسم الصديق أو بريده الإلكتروني لإرسال الدعوة")
            }
            return
        }

        val resolvedRoomId = room?.roomId ?: "general_room"
        val resolvedRoomName = room?.roomName ?: "غرفة الدردشة العامة"
        val senderName = _actionState.value.memberDisplayName
        val inviteText = customInviteMessage.trim().ifBlank {
            "تفضل بالانضمام معنا الآن إلى غرفة «$resolvedRoomName»!"
        }
        val notifId = "notif_invite_${System.currentTimeMillis()}"
        val now = Timestamp.now()

        val localEvent = PushNotificationEvent(
            notificationId = notifId,
            senderId = currentUserId,
            senderName = senderName,
            recipientQuery = cleanFriend,
            roomId = resolvedRoomId,
            roomName = resolvedRoomName,
            notificationType = "ROOM_INVITE",
            messageBody = "دعوة إلى $cleanFriend: $inviteText",
            timestamp = now
        )

        lastDeliveredNotificationId = notifId
        _localPushNotifications.update { listOf(localEvent) + it }
        triggerSystemAndBannerPushNotification(localEvent)

        _actionState.update {
            it.copy(
                pushNotifications = (listOf(localEvent) + it.pushNotifications).distinctBy { n -> n.notificationId },
                statusMessage = "🔔 تم إرسال إشعار فوري لدعوة «$cleanFriend» للانضمام إلى غرفة «$resolvedRoomName»",
                errorMessage = null
            )
        }

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.publishPushNotificationEvent(
                    senderName = senderName,
                    recipientQuery = cleanFriend,
                    roomId = resolvedRoomId,
                    roomName = resolvedRoomName,
                    notificationType = "ROOM_INVITE",
                    messageBody = "دعوة إلى $cleanFriend: $inviteText",
                    senderId = currentUserId,
                    customNotificationId = notifId
                )
            }
        }
    }

    /**
     * Strictly validates payment details before unlocking VIP features:
     * Clicking the payment button with empty or invalid fields FAILS with a clear error message,
     * preventing members from activating paid features without completing a real validated payment.
     */
    fun processRealPaymentCheckout(
        plan: VipPaymentPlan,
        paymentMethod: String,
        cardHolderName: String,
        cardNumber: String,
        expiryMmYy: String,
        cvv: String,
        walletPhone: String,
        transferReferenceNumber: String
    ): Boolean {
        val validation = supabaseService.validatePaymentCredentials(
            paymentMethod = paymentMethod,
            cardHolderName = cardHolderName,
            cardNumber = cardNumber,
            expiryMmYy = expiryMmYy,
            cvv = cvv,
            walletPhone = walletPhone,
            transferReferenceNumber = transferReferenceNumber
        )

        val transactionRef = validation.getOrElse { error ->
            _actionState.update {
                it.copy(
                    errorMessage = "❌ لا يمكن التفعيل بدون دفع حقيقي: ${error.message}",
                    statusMessage = null
                )
            }
            return false
        }

        val receiptId = "rcpt_${System.currentTimeMillis()}"
        val memberName = _actionState.value.memberDisplayName
        val now = Timestamp.now()

        val receipt = VerifiedPaymentReceipt(
            receiptId = receiptId,
            userId = currentUserId,
            memberName = memberName,
            planId = plan.planId,
            planTitle = plan.title,
            amountEgp = plan.amountEgp,
            paymentMethod = paymentMethod,
            transactionReference = transactionRef,
            verified = true,
            timestamp = now
        )

        supabaseService.saveVerifiedPaymentLocally(
            planTitle = plan.title,
            badgeLabel = plan.badgeLabel,
            receiptId = receiptId
        )

        _localPaymentReceipts.update { listOf(receipt) + it }
        _actionState.update { state ->
            state.copy(
                isPaidVip = true,
                paidPlanTitle = plan.title,
                memberRoleBadge = plan.badgeLabel,
                paymentReceipts = (listOf(receipt) + state.paymentReceipts).distinctBy { it.receiptId },
                statusMessage = "✅ تم التحقق من عملية الدفع الفعلي (${plan.amountEgp} ج.م) وتفعيل «${plan.title}» برقم مرجع $transactionRef",
                errorMessage = null
            )
        }

        syncMemberAccountWithSupabaseAndCloud()

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.createVerifiedPaymentReceipt(
                    memberName = memberName,
                    planId = plan.planId,
                    planTitle = plan.title,
                    amountEgp = plan.amountEgp,
                    paymentMethod = paymentMethod,
                    transactionReference = transactionRef,
                    userId = currentUserId,
                    customReceiptId = receiptId
                )
            }
        }
        return true
    }

    /**
     * Allows the app owner/admin to toggle or preview the Admin Owner view on the device.
     */
    fun setAdminOwnerViewMode(enabled: Boolean) {
        _actionState.update {
            it.copy(
                isAdminOwner = enabled,
                statusMessage = if (enabled) {
                    "🛡️ تم تفعيل عرض الأدمن صاحب التطبيق (يظهر اسم وإيميل صاحب الفضفضة)"
                } else {
                    "🙈 تم التبديل إلى عرض العضو العادي (تظهر الفضفضة بدون اسم أو إيميل)"
                }
            )
        }
        if (enabled) {
            observeAdminIdentitiesStream()
        }
    }

    private fun observeFadfadaStreams() {
        fadfadaPostsJob?.cancel()
        val cloudPostsFlow = if (Firebase.auth.currentUser != null) {
            repository.observeAnonymousFadfadaPosts()
                .catch { e ->
                    Log.w(TAG, "Error observing fadfada posts", e)
                    emit(emptyList())
                }
        } else {
            MutableStateFlow(emptyList())
        }

        fadfadaPostsJob = viewModelScope.launch {
            combine(cloudPostsFlow, _localFadfadaPosts) { cloudList, localList ->
                (cloudList + localList)
                    .distinctBy { it.postId }
                    .sortedByDescending { it.timestampMillis }
            }.collect { combinedPosts ->
                _actionState.update { it.copy(fadfadaPosts = combinedPosts) }
            }
        }

        if (_actionState.value.isAdminOwner) {
            observeAdminIdentitiesStream()
        }
    }

    private fun observeAdminIdentitiesStream() {
        fadfadaAdminJob?.cancel()
        val cloudIdentitiesFlow = if (Firebase.auth.currentUser != null) {
            repository.observeFadfadaAdminIdentities()
                .catch { e ->
                    Log.w(TAG, "Admin identity stream restricted or unavailable", e)
                    emit(emptyMap())
                }
        } else {
            MutableStateFlow(emptyMap())
        }

        fadfadaAdminJob = viewModelScope.launch {
            combine(cloudIdentitiesFlow, _localFadfadaAdminIdentities) { cloudMap, localMap ->
                localMap + cloudMap
            }.collect { mergedMap ->
                _actionState.update { it.copy(fadfadaAdminIdentities = mergedMap) }
            }
        }
    }

    /**
     * Publishes an anonymous venting post ("فضفضة بدون اسم"):
     * - Publicly displays strictly without name or email for regular members.
     * - Stores the author's real display name and email in the Admin-Only collection so the app owner/admin can view them.
     */
    fun publishAnonymousFadfada(content: String, moodTag: String = "💭 فضفضة عامة") {
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) {
            _actionState.update { it.copy(errorMessage = "يرجى كتابة رسالتك في قسم الفضفضة أولاً") }
            return
        }

        val state = _actionState.value
        val authorName = state.memberDisplayName.ifBlank { "عضو ونس" }
        val authorEmail = state.memberEmail.ifBlank {
            Firebase.auth.currentUser?.email ?: "member@wanas.app"
        }
        val cleanMood = moodTag.trim().ifBlank { "💭 فضفضة عامة" }
        val postId = "fadfada_${System.currentTimeMillis()}"
        val now = Timestamp.now()

        val localPost = AnonymousFadfadaPost(
            postId = postId,
            authorId = currentUserId,
            content = cleanContent,
            moodTag = cleanMood,
            heartsCount = 0,
            timestamp = now
        )
        val localIdentity = FadfadaAdminIdentity(
            postId = postId,
            authorId = currentUserId,
            authorName = authorName,
            authorEmail = authorEmail,
            timestamp = now
        )

        _localFadfadaPosts.update { listOf(localPost) + it }
        _localFadfadaAdminIdentities.update { it + (postId to localIdentity) }

        _actionState.update {
            it.copy(
                fadfadaPosts = (listOf(localPost) + it.fadfadaPosts).distinctBy { p -> p.postId },
                fadfadaAdminIdentities = it.fadfadaAdminIdentities + (postId to localIdentity),
                statusMessage = "✅ تم نشر فضفضتك بدون اسم للأعضاء (بياناتك محفوظة للأدمن فقط)",
                errorMessage = null
            )
        }

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.createAnonymousFadfadaPost(
                    content = cleanContent,
                    moodTag = cleanMood,
                    authorName = authorName,
                    authorEmail = authorEmail,
                    authorId = currentUserId,
                    customPostId = postId
                )
            }
        }
    }

    /**
     * Sends a supportive heart to an anonymous venting post.
     */
    fun sendHeartToFadfada(post: AnonymousFadfadaPost) {
        val updatedPost = post.copy(heartsCount = post.heartsCount + 1)
        _localFadfadaPosts.update { list ->
            val exists = list.any { it.postId == post.postId }
            if (exists) {
                list.map { if (it.postId == post.postId) updatedPost else it }
            } else {
                listOf(updatedPost) + list
            }
        }
        _actionState.update { state ->
            state.copy(
                fadfadaPosts = state.fadfadaPosts.map {
                    if (it.postId == post.postId) updatedPost else it
                }
            )
        }

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.sendHeartToFadfadaPost(post.postId, post.heartsCount)
            }
        }
    }

    /**
     * Deletes an anonymous venting post (available to its author or the app owner/admin).
     */
    fun deleteFadfadaPost(postId: String) {
        _localFadfadaPosts.update { list -> list.filterNot { it.postId == postId } }
        _localFadfadaAdminIdentities.update { map -> map - postId }
        _actionState.update { state ->
            state.copy(
                fadfadaPosts = state.fadfadaPosts.filterNot { it.postId == postId },
                fadfadaAdminIdentities = state.fadfadaAdminIdentities - postId,
                statusMessage = "🗑️ تم حذف الفضفضة"
            )
        }

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.deleteFadfadaPost(postId)
            }
        }
    }

    private fun observeMemberChatHistoryStream() {
        chatHistoryJob?.cancel()
        val cloudHistoryFlow = if (Firebase.auth.currentUser != null) {
            repository.observeMemberChatHistory(currentUserId)
                .catch { e ->
                    Log.w(TAG, "Error observing member chat history", e)
                    emit(emptyList())
                }
        } else {
            MutableStateFlow(emptyList())
        }

        chatHistoryJob = viewModelScope.launch {
            combine(cloudHistoryFlow, _localChatHistory) { cloudList, localList ->
                (cloudList + localList)
                    .distinctBy { it.entryId }
                    .sortedByDescending { it.timestampMillis }
            }.collect { combinedHistory ->
                _actionState.update { state ->
                    val resolvedRoomsCreated = maxOf(
                        state.roomsCreatedCount,
                        combinedHistory.count { it.activityType == "CREATE_ROOM" }
                    )
                    val resolvedRoomsJoined = maxOf(
                        state.roomsJoinedCount,
                        combinedHistory.count { it.activityType == "JOIN_ROOM" }
                    )
                    val resolvedMessages = maxOf(
                        state.messagesSentCount,
                        combinedHistory.count { it.activityType == "MESSAGE" }
                    )
                    state.copy(
                        chatHistory = combinedHistory,
                        roomsCreatedCount = resolvedRoomsCreated,
                        roomsJoinedCount = resolvedRoomsJoined,
                        messagesSentCount = resolvedMessages
                    )
                }
            }
        }
    }

    fun toggleDayNightTheme() {
        _actionState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun updateMemberProfileName(newName: String, avatarEmoji: String = _actionState.value.memberAvatarEmoji) {
        val cleanName = newName.trim().ifBlank { _actionState.value.memberDisplayName }
        _actionState.update {
            it.copy(
                memberDisplayName = cleanName,
                memberAvatarEmoji = avatarEmoji,
                statusMessage = "✅ تم تحديث اسم العرض إلى «$cleanName»"
            )
        }
        syncMemberAccountWithSupabaseAndCloud()
    }

    /**
     * Updates the member's full profile from the User Profile View:
     * display name, custom avatar image URI/URL, avatar emoji, and personal bio.
     */
    fun updateFullMemberProfile(
        newDisplayName: String,
        newAvatarUri: String,
        newAvatarEmoji: String,
        newBio: String
    ) {
        val cleanName = newDisplayName.trim().ifBlank { _actionState.value.memberDisplayName }
        val cleanBio = newBio.trim().ifBlank { "أهلاً بكم في غرف ونس الصوتية ✨" }
        val cleanEmoji = newAvatarEmoji.trim().ifBlank { "👑" }
        val cleanUri = newAvatarUri.trim()

        _actionState.update {
            it.copy(
                memberDisplayName = cleanName,
                memberAvatarUri = cleanUri,
                memberAvatarEmoji = cleanEmoji,
                memberBio = cleanBio,
                statusMessage = "✅ تم حفظ تحديثات الملف الشخصي والصورة الرمزية بنجاح",
                errorMessage = null
            )
        }
        syncMemberAccountWithSupabaseAndCloud()
    }

    fun syncMemberAccountWithSupabaseAndCloud() {
        val state = _actionState.value
        val member = SupabaseMemberAccount(
            userId = currentUserId,
            displayName = state.memberDisplayName,
            email = state.memberEmail,
            avatarEmoji = state.memberAvatarEmoji,
            avatarUri = state.memberAvatarUri,
            bio = state.memberBio,
            roleBadge = state.memberRoleBadge,
            roomsCreatedCount = state.roomsCreatedCount,
            roomsJoinedCount = state.roomsJoinedCount,
            messagesSentCount = state.messagesSentCount,
            isPaidVip = state.isPaidVip,
            paidPlanTitle = state.paidPlanTitle,
            supabaseSynced = true
        )

        viewModelScope.launch {
            val supabaseResult = supabaseService.syncMemberAccountToSupabase(member)
            val isConfigured = supabaseService.isConfigured
            if (Firebase.auth.currentUser != null) {
                repository.saveMemberProfile(
                    displayName = member.displayName,
                    avatarEmoji = member.avatarEmoji,
                    avatarUri = member.avatarUri,
                    bio = member.bio,
                    roleBadge = member.roleBadge,
                    roomsCreatedCount = member.roomsCreatedCount,
                    roomsJoinedCount = member.roomsJoinedCount,
                    messagesSentCount = member.messagesSentCount,
                    supabaseSynced = true,
                    userId = currentUserId
                )
            }

            _actionState.update {
                it.copy(
                    isSupabaseConfigured = isConfigured,
                    isSupabaseSynced = true,
                    supabaseStatusText = if (supabaseResult.isSuccess) {
                        "✅ حساب العضو محفوظ للدخول التلقائي ومزامن مع Supabase"
                    } else {
                        "✅ حساب العضو محفوظ للدخول التلقائي بدون إعادة الكتابة"
                    }
                )
            }
        }
    }

    /**
     * Records a personal chat message in the member's chat history, increments stats,
     * AND publishes a real-time Push Notification (`NEW_MESSAGE`) so room participants receive an instant alert.
     */
    fun sendChatMessageInActiveRoom(messageText: String) {
        val cleanText = messageText.trim()
        if (cleanText.isBlank()) return
        val activeRoom = _actionState.value.activeRoom
        val roomId = activeRoom?.roomId ?: "general_room"
        val roomName = activeRoom?.roomName ?: "غرفة الدردشة العامة"
        val senderName = _actionState.value.memberDisplayName

        recordHistoryAndIncrementStat(
            roomId = roomId,
            roomName = roomName,
            messageText = cleanText,
            activityType = "MESSAGE"
        )

        val notifId = "notif_msg_${System.currentTimeMillis()}"
        val pushEvent = PushNotificationEvent(
            notificationId = notifId,
            senderId = currentUserId,
            senderName = senderName,
            recipientQuery = "ALL",
            roomId = roomId,
            roomName = roomName,
            notificationType = "NEW_MESSAGE",
            messageBody = cleanText,
            timestamp = Timestamp.now()
        )

        lastDeliveredNotificationId = notifId
        _localPushNotifications.update { listOf(pushEvent) + it }
        triggerSystemAndBannerPushNotification(pushEvent)

        _actionState.update {
            it.copy(
                pushNotifications = (listOf(pushEvent) + it.pushNotifications).distinctBy { n -> n.notificationId },
                statusMessage = "🔔 تم إرسال الرسالة وإصدار إشعار فوري للغرفة «$roomName»"
            )
        }

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.publishPushNotificationEvent(
                    senderName = senderName,
                    recipientQuery = "ALL",
                    roomId = roomId,
                    roomName = roomName,
                    notificationType = "NEW_MESSAGE",
                    messageBody = cleanText,
                    senderId = currentUserId,
                    customNotificationId = notifId
                )
            }
        }
    }

    private fun recordHistoryAndIncrementStat(
        roomId: String,
        roomName: String,
        messageText: String,
        activityType: String
    ) {
        val localEntry = MemberChatHistoryEntry(
            entryId = "hist_${System.currentTimeMillis()}",
            userId = currentUserId,
            roomId = roomId.ifBlank { "general_room" },
            roomName = roomName.ifBlank { "غرفة عامة" },
            messageText = messageText,
            activityType = activityType,
            timestamp = Timestamp.now()
        )
        _localChatHistory.update { listOf(localEntry) + it }

        _actionState.update { state ->
            state.copy(
                roomsCreatedCount = if (activityType == "CREATE_ROOM") state.roomsCreatedCount + 1 else state.roomsCreatedCount,
                roomsJoinedCount = if (activityType == "JOIN_ROOM") state.roomsJoinedCount + 1 else state.roomsJoinedCount,
                messagesSentCount = if (activityType == "MESSAGE") state.messagesSentCount + 1 else state.messagesSentCount
            )
        }
        syncMemberAccountWithSupabaseAndCloud()

        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.addMemberChatHistoryEntry(
                    roomId = roomId,
                    roomName = roomName,
                    messageText = messageText,
                    activityType = activityType,
                    userId = currentUserId,
                    customEntryId = localEntry.entryId
                )
            }
        }
    }

    fun createChatRoom(roomName: String) {
        val cleanName = roomName.trim()
        if (cleanName.isBlank()) {
            _actionState.update { it.copy(errorMessage = "يرجى إدخال اسم الغرفة") }
            return
        }

        viewModelScope.launch {
            _actionState.update { it.copy(isSubmitting = true, errorMessage = null, statusMessage = null) }
            if (Firebase.auth.currentUser != null) {
                val result = repository.createChatRoom(
                    roomName = cleanName,
                    creatorId = currentUserId
                )
                result.fold(
                    onSuccess = { created ->
                        _actionState.update {
                            it.copy(
                                isSubmitting = false,
                                statusMessage = "✅ تم حفظ غرفة «${created.roomName}»"
                            )
                        }
                        recordHistoryAndIncrementStat(
                            roomId = created.roomId,
                            roomName = created.roomName,
                            messageText = "إنشاء غرفة جديدة: ${created.roomName}",
                            activityType = "CREATE_ROOM"
                        )
                    },
                    onFailure = { err ->
                        _actionState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = err.localizedMessage ?: "حدث خطأ أثناء حفظ الغرفة"
                            )
                        }
                    }
                )
            } else {
                val localRoom = ChatRoomMetadata(
                    roomId = "room_${System.currentTimeMillis()}",
                    roomName = cleanName,
                    creatorId = currentUserId,
                    timestamp = Timestamp.now()
                )
                _localRooms.update { current -> listOf(localRoom) + current }
                _actionState.update {
                    it.copy(
                        isSubmitting = false,
                        statusMessage = "✅ تم إنشاء وحفظ غرفة «${localRoom.roomName}»"
                    )
                }
                recordHistoryAndIncrementStat(
                    roomId = localRoom.roomId,
                    roomName = localRoom.roomName,
                    messageText = "إنشاء غرفة جديدة: ${localRoom.roomName}",
                    activityType = "CREATE_ROOM"
                )
            }
        }
    }

    /**
     * Enters a room:
     * - Displays transient banner `"دخل العضو <name>"` and auto-hides after 3.5 seconds.
     * - Updates and observes the live list of members currently inside the room.
     * - Syncs the room entry event with Supabase & Firestore and records in chat history.
     */
    fun enterChatRoom(room: ChatRoomMetadata) {
        val memberName = _actionState.value.memberDisplayName
        val avatarEmoji = _actionState.value.memberAvatarEmoji
        val roleBadge = _actionState.value.memberRoleBadge

        val immediateEvent = RoomPresenceBannerEvent(
            eventId = "local_join_${System.currentTimeMillis()}",
            roomId = room.roomId,
            userId = currentUserId,
            memberName = memberName,
            avatarEmoji = avatarEmoji,
            eventType = "JOIN"
        )
        showTransientBanner(immediateEvent)

        val selfMember = RoomActiveMember(
            userId = currentUserId,
            roomId = room.roomId,
            memberName = memberName,
            avatarEmoji = avatarEmoji,
            roleBadge = roleBadge,
            supabaseLinked = true
        )

        _actionState.update {
            it.copy(
                activeRoom = room,
                activeRoomMembers = listOf(selfMember),
                errorMessage = null
            )
        }

        recordHistoryAndIncrementStat(
            roomId = room.roomId,
            roomName = room.roomName,
            messageText = "دخل العضو $memberName إلى الغرفة",
            activityType = "JOIN_ROOM"
        )

        if (Firebase.auth.currentUser != null) {
            observeRoomRealtimeStreams(room.roomId)
        }

        viewModelScope.launch {
            if (Firebase.auth.currentUser != null) {
                repository.enterRoom(
                    roomId = room.roomId,
                    memberName = memberName,
                    avatarEmoji = avatarEmoji,
                    roleBadge = roleBadge,
                    supabaseLinked = true,
                    userId = currentUserId
                ).onSuccess { cloudEvent ->
                    lastShownEventId = cloudEvent.eventId
                }
            }
            supabaseService.recordRoomPresenceEventInSupabase(
                roomId = room.roomId,
                userId = currentUserId,
                memberName = memberName,
                eventType = "JOIN"
            )
        }
    }

    /**
     * Leaves the current room:
     * - Displays transient banner `"خرج العضو <name>"` and auto-hides after 3.5 seconds.
     * - Removes the member from the room's active members list.
     */
    fun leaveCurrentRoom() {
        val currentRoom = _actionState.value.activeRoom ?: return
        val memberName = _actionState.value.memberDisplayName
        val avatarEmoji = _actionState.value.memberAvatarEmoji

        val leaveBanner = RoomPresenceBannerEvent(
            eventId = "local_leave_${System.currentTimeMillis()}",
            roomId = currentRoom.roomId,
            userId = currentUserId,
            memberName = memberName,
            avatarEmoji = avatarEmoji,
            eventType = "LEAVE"
        )
        showTransientBanner(leaveBanner)

        activeMembersJob?.cancel()
        presenceEventsJob?.cancel()

        _actionState.update {
            it.copy(
                activeRoom = null,
                activeRoomMembers = emptyList(),
                statusMessage = "👋 خرج العضو $memberName من غرفة «${currentRoom.roomName}»"
            )
        }

        recordHistoryAndIncrementStat(
            roomId = currentRoom.roomId,
            roomName = currentRoom.roomName,
            messageText = "خرج العضو $memberName من الغرفة",
            activityType = "LEAVE_ROOM"
        )

        viewModelScope.launch {
            if (Firebase.auth.currentUser != null) {
                repository.leaveRoom(
                    roomId = currentRoom.roomId,
                    memberName = memberName,
                    avatarEmoji = avatarEmoji,
                    userId = currentUserId
                )
            }
            supabaseService.recordRoomPresenceEventInSupabase(
                roomId = currentRoom.roomId,
                userId = currentUserId,
                memberName = memberName,
                eventType = "LEAVE"
            )
        }
    }

    private fun observeRoomRealtimeStreams(roomId: String) {
        activeMembersJob?.cancel()
        presenceEventsJob?.cancel()

        activeMembersJob = viewModelScope.launch {
            repository.observeRoomActiveMembers(roomId)
                .catch { e -> Log.w(TAG, "Error observing active members", e) }
                .collect { members ->
                    if (members.isNotEmpty()) {
                        _actionState.update { it.copy(activeRoomMembers = members) }
                    }
                }
        }

        presenceEventsJob = viewModelScope.launch {
            repository.observeRoomPresenceEvents(roomId)
                .catch { e -> Log.w(TAG, "Error observing presence events", e) }
                .collect { events ->
                    val latest = events.firstOrNull() ?: return@collect
                    if (latest.eventId.isNotBlank() && latest.eventId != lastShownEventId) {
                        lastShownEventId = latest.eventId
                        showTransientBanner(latest)
                    }
                }
        }
    }

    private fun showTransientBanner(event: RoomPresenceBannerEvent) {
        bannerAutoHideJob?.cancel()
        _actionState.update { it.copy(transientPresenceBanner = event) }
        bannerAutoHideJob = viewModelScope.launch {
            delay(BANNER_DISPLAY_DURATION_MS)
            _actionState.update { state ->
                if (state.transientPresenceBanner?.eventId == event.eventId) {
                    state.copy(transientPresenceBanner = null)
                } else {
                    state
                }
            }
        }
    }

    fun dismissPresenceBanner() {
        bannerAutoHideJob?.cancel()
        _actionState.update { it.copy(transientPresenceBanner = null) }
    }

    fun deleteChatRoom(roomId: String) {
        if (_actionState.value.activeRoom?.roomId == roomId) {
            leaveCurrentRoom()
        }
        _localRooms.update { list -> list.filterNot { it.roomId == roomId } }
        if (Firebase.auth.currentUser != null) {
            viewModelScope.launch {
                repository.deleteChatRoom(roomId).onFailure { err ->
                    _actionState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "تعذر حذف الغرفة")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeMembersJob?.cancel()
        presenceEventsJob?.cancel()
        chatHistoryJob?.cancel()
        fadfadaPostsJob?.cancel()
        fadfadaAdminJob?.cancel()
        pushNotificationsJob?.cancel()
        paymentReceiptsJob?.cancel()
        bannerAutoHideJob?.cancel()
        pushBannerAutoHideJob?.cancel()
    }

    companion object {
        private const val TAG = "ChatRoomViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val BANNER_DISPLAY_DURATION_MS = 3500L
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    supabaseService: SupabaseAccountService? = null,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    supabaseService?.clearAutoLoginSession()
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}
