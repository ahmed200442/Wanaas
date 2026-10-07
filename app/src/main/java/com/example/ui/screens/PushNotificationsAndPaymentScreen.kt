package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ChatRoomMetadata
import com.example.data.PushNotificationEvent
import com.example.data.VerifiedPaymentReceipt
import com.example.data.VipPaymentPlan
import com.example.notifications.PushNotificationHelper
import com.example.ui.theme.WanasAmberGold
import com.example.ui.theme.WanasCardBg
import com.example.ui.theme.WanasCoralWarm
import com.example.ui.theme.WanasDayDarkEmerald
import com.example.ui.theme.WanasDayDarkHeader
import com.example.ui.theme.WanasDayLightSurface
import com.example.ui.theme.WanasDaySurfaceVariant
import com.example.ui.theme.WanasDayTextPrimary
import com.example.ui.theme.WanasDayTextSecondary
import com.example.ui.theme.WanasEmeraldOnline
import com.example.ui.theme.WanasTealCalm
import com.example.ui.theme.WanasTealDeep
import com.example.ui.theme.WanasVioletAccent
import com.example.viewmodel.ChatRoomsActionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VIP_PAYMENT_PLANS = listOf(
    VipPaymentPlan(
        planId = "vip_gold_monthly",
        title = "باقة VIP الذهبية",
        badgeLabel = "👑 VIP ذهبي مدفوع",
        amountEgp = 150,
        description = "شارة التاج الذهبي + أولوية الظهور في الغرف + إشعارات دعوات مميزة"
    ),
    VipPaymentPlan(
        planId = "vip_royal_diamond",
        title = "باقة الملوك الماسية",
        badgeLabel = "💎 ملك الغرف المدفوع",
        amountEgp = 350,
        description = "شارة الماسة الملكية + تثبيت الغرف الصوتية + صلاحيات خاصة كاملة"
    )
)

/**
 * Screen combining:
 * 1. Real-time Push Notifications Center (`NEW_MESSAGE` & `ROOM_INVITE`) with Android 13+ runtime permission request,
 *    Friend Room Invitation sender, and Instant Message Push sender.
 * 2. Real Verified Payment Checkout Gate (`BANK_CARD` with Luhn 16-digit validation, expiry, CVV, or
 *    `VODAFONE_CASH` / `INSTAPAY` with 11-digit mobile & real transaction reference number) so NO member can unlock
 *    paid VIP features simply by clicking a button without completing a real validated payment.
 */
@Composable
fun PushNotificationsAndPaymentScreen(
    actionState: ChatRoomsActionState,
    availableRooms: List<ChatRoomMetadata>,
    isDarkMode: Boolean,
    initialTabIndex: Int = 0, // 0 = Push Notifications & Friend Invite, 1 = Real Payment Checkout
    onSendFriendInvitePush: (friendNameOrEmail: String, room: ChatRoomMetadata?, customMessage: String) -> Unit,
    onSendRoomMessagePush: (messageText: String) -> Unit,
    onProcessRealPayment: (
        plan: VipPaymentPlan,
        paymentMethod: String,
        cardHolderName: String,
        cardNumber: String,
        expiryMmYy: String,
        cvv: String,
        walletPhone: String,
        transferReferenceNumber: String
    ) -> Boolean,
    onBackToRooms: () -> Unit
) {
    BackHandler {
        onBackToRooms()
    }

    val context = LocalContext.current
    val notifHelper = remember(context) { PushNotificationHelper(context) }
    var hasNotifPermission by remember { mutableStateOf(notifHelper.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    var selectedTab by rememberSaveable { mutableStateOf(initialTabIndex) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val primaryCardBg = if (isDarkMode) WanasCardBg else WanasDayDarkHeader
    val secondaryCardBg = if (isDarkMode) WanasCardBg else WanasDayLightSurface
    val textPrimary = if (isDarkMode) Color.White else WanasDayTextPrimary
    val textSub = if (isDarkMode) Color.White.copy(alpha = 0.72f) else WanasDayTextSecondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .testTag("notifications_and_payment_screen_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header + Tab Switcher
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notifications_payment_header_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = primaryCardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.NotificationsActive else Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = WanasAmberGold
                        )
                        Column {
                            Text(
                                text = if (selectedTab == 0) {
                                    "الإشعارات الفورية ودعوة الأصدقاء"
                                } else {
                                    "بوابة الدفع الحقيقي وتفعيل VIP"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = WanasAmberGold
                            )
                            Text(
                                text = if (selectedTab == 0) {
                                    "إشعارات فورية عند تلقي رسالة جديدة أو دعوة صديق للغرفة"
                                } else {
                                    "لا يتم التفعيل بمجرد الضغط — يتطلب التحقق من بيانات الدفع الفعلي"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onBackToRooms,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WanasAmberGold,
                            contentColor = Color(0xFF1A103C)
                        ),
                        modifier = Modifier.testTag("notifications_payment_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع للغرف",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الغرف", fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Switcher between Push Notifications (0) and Real Payment Checkout (1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("tab_push_notifications_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) WanasAmberGold else Color.White.copy(alpha = 0.12f),
                            contentColor = if (selectedTab == 0) Color(0xFF1A103C) else Color.White
                        )
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإشعارات والدعوات", fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("tab_real_payment_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) WanasEmeraldOnline else Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الدفع الحقيقي VIP", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Feedback Banner
        val feedback = actionState.errorMessage ?: actionState.statusMessage
        if (feedback != null) {
            Surface(
                color = if (actionState.errorMessage != null) {
                    WanasCoralWarm.copy(alpha = 0.2f)
                } else {
                    WanasDayDarkEmerald
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_or_push_feedback_banner")
            ) {
                Text(
                    text = feedback,
                    modifier = Modifier.padding(12.dp),
                    color = if (actionState.errorMessage != null) WanasCoralWarm else Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selectedTab == 0) {
            PushNotificationsAndInviteContent(
                actionState = actionState,
                availableRooms = availableRooms,
                hasNotifPermission = hasNotifPermission,
                isDarkMode = isDarkMode,
                secondaryCardBg = secondaryCardBg,
                textPrimary = textPrimary,
                textSub = textSub,
                dateFormatter = dateFormatter,
                onRequestNotifPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onSendFriendInvitePush = onSendFriendInvitePush,
                onSendRoomMessagePush = onSendRoomMessagePush
            )
        } else {
            RealPaymentCheckoutContent(
                actionState = actionState,
                isDarkMode = isDarkMode,
                secondaryCardBg = secondaryCardBg,
                textPrimary = textPrimary,
                textSub = textSub,
                dateFormatter = dateFormatter,
                onProcessRealPayment = onProcessRealPayment
            )
        }
    }
}

@Composable
private fun PushNotificationsAndInviteContent(
    actionState: ChatRoomsActionState,
    availableRooms: List<ChatRoomMetadata>,
    hasNotifPermission: Boolean,
    isDarkMode: Boolean,
    secondaryCardBg: Color,
    textPrimary: Color,
    textSub: Color,
    dateFormatter: SimpleDateFormat,
    onRequestNotifPermission: () -> Unit,
    onSendFriendInvitePush: (friendNameOrEmail: String, room: ChatRoomMetadata?, customMessage: String) -> Unit,
    onSendRoomMessagePush: (messageText: String) -> Unit
) {
    var friendNameInput by rememberSaveable { mutableStateOf("") }
    var inviteMessageInput by rememberSaveable { mutableStateOf("") }
    var newMessagePushInput by rememberSaveable { mutableStateOf("") }

    val selectedRoom = actionState.activeRoom ?: availableRooms.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("push_notifications_content_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Permission status bar
        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                Surface(
                    color = WanasDayDarkHeader,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WanasAmberGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔 فعّل صلاحية الإشعارات الفورية للنظام لتلقي التنبيهات في شريط الهاتف",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestNotifPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WanasAmberGold,
                                contentColor = Color(0xFF1A103C)
                            ),
                            modifier = Modifier.testTag("enable_system_notifications_button")
                        ) {
                            Text("تفعيل", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // 1. Invite a Friend to Join a Chat Room Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invite_friend_push_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = secondaryCardBg),
                border = BorderStroke(1.5.dp, WanasTealCalm.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = WanasTealCalm
                        )
                        Column {
                            Text(
                                text = "📩 دعوة صديق للانضمام إلى غرفة دردشة (Push Notification)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                            Text(
                                text = "الغرفة المحددة: ${selectedRoom?.roomName ?: "غرفة الدردشة العامة"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDarkMode) WanasAmberGold else WanasTealDeep
                            )
                        }
                    }

                    OutlinedTextField(
                        value = friendNameInput,
                        onValueChange = { friendNameInput = it },
                        label = { Text("اسم الصديق أو بريده الإلكتروني") },
                        placeholder = { Text("مثال: محمد علي أو friend@email.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_friend_name_input")
                    )

                    OutlinedTextField(
                        value = inviteMessageInput,
                        onValueChange = { inviteMessageInput = it },
                        label = { Text("رسالة الدعوة (اختياري)") },
                        placeholder = { Text("تعال انضم لينا في الغرفة الآن!") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_friend_message_input")
                    )

                    Button(
                        onClick = {
                            onSendFriendInvitePush(friendNameInput, selectedRoom, inviteMessageInput)
                            if (friendNameInput.isNotBlank()) {
                                friendNameInput = ""
                                inviteMessageInput = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_friend_invite_push_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) WanasTealCalm else WanasDayDarkHeader,
                            contentColor = if (isDarkMode) Color(0xFF00201D) else WanasAmberGold
                        )
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال دعوة فورية للصديق", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 2. Send New Room Message with Push Notification Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_message_push_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = secondaryCardBg),
                border = BorderStroke(1.2.dp, WanasVioletAccent.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💬 إرسال رسالة جديدة مع إشعار فوري للأعضاء",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newMessagePushInput,
                            onValueChange = { newMessagePushInput = it },
                            label = { Text("اكتب رسالة جديدة للغرفة...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_message_push_input")
                        )

                        Button(
                            onClick = {
                                if (newMessagePushInput.isNotBlank()) {
                                    onSendRoomMessagePush(newMessagePushInput)
                                    newMessagePushInput = ""
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("send_new_message_push_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WanasEmeraldOnline,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // 3. Live Feed of Received Push Notifications
        item {
            Text(
                text = "🔔 سجل الإشعارات الفورية المباشر (${actionState.pushNotifications.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
        }

        if (actionState.pushNotifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_push_notifications_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = secondaryCardBg)
                ) {
                    Text(
                        text = "لا توجد إشعارات حتى الآن. أرسل رسالة جديدة أو ادعُ صديقاً ليظهر الإشعار الفوري هنا وفي شريط إشعارات الهاتف.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = textSub
                    )
                }
            }
        } else {
            items(actionState.pushNotifications, key = { it.notificationId }) { notif ->
                PushNotificationItemCard(
                    event = notif,
                    isDarkMode = isDarkMode,
                    formattedTime = if (notif.timestampMillis > 0L) {
                        dateFormatter.format(Date(notif.timestampMillis))
                    } else {
                        "الآن"
                    }
                )
            }
        }
    }
}

@Composable
private fun PushNotificationItemCard(
    event: PushNotificationEvent,
    isDarkMode: Boolean,
    formattedTime: String
) {
    val isInvite = event.isRoomInvite
    val cardBg = if (isDarkMode) WanasCardBg else WanasDayLightSurface
    val textColor = if (isDarkMode) Color.White else WanasDayTextPrimary
    val accentColor = if (isInvite) WanasAmberGold else WanasEmeraldOnline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("push_notification_item_${event.notificationId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isInvite) Icons.Default.PersonAdd else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (isDarkMode) accentColor else WanasDayDarkHeader
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.formattedTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = "${event.senderName}: ${event.messageBody}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.88f)
                )
                Text(
                    text = "المستلم: ${event.recipientQuery} • $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkMode) WanasTealCalm else WanasTealDeep
                )
            }
        }
    }
}

@Composable
private fun RealPaymentCheckoutContent(
    actionState: ChatRoomsActionState,
    isDarkMode: Boolean,
    secondaryCardBg: Color,
    textPrimary: Color,
    textSub: Color,
    dateFormatter: SimpleDateFormat,
    onProcessRealPayment: (
        plan: VipPaymentPlan,
        paymentMethod: String,
        cardHolderName: String,
        cardNumber: String,
        expiryMmYy: String,
        cvv: String,
        walletPhone: String,
        transferReferenceNumber: String
    ) -> Boolean
) {
    var selectedPlan by remember { mutableStateOf(VIP_PAYMENT_PLANS.first()) }
    var selectedPaymentMethod by rememberSaveable { mutableStateOf("BANK_CARD") }

    // Bank Card fields
    var cardHolderName by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiryMmYy by rememberSaveable { mutableStateOf("") }
    var cvv by rememberSaveable { mutableStateOf("") }

    // Vodafone Cash / InstaPay fields
    var walletPhone by rememberSaveable { mutableStateOf("") }
    var transferReferenceNumber by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("real_payment_checkout_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Current VIP Payment Verification Status Banner
        item {
            Surface(
                color = if (actionState.isPaidVip) WanasDayDarkEmerald else WanasDayDarkHeader,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (actionState.isPaidVip) WanasEmeraldOnline else WanasAmberGold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vip_payment_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (actionState.isPaidVip) Icons.Default.Verified else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (actionState.isPaidVip) WanasEmeraldOnline else WanasAmberGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (actionState.isPaidVip) {
                                "✅ اشتراك مدفوع ومؤكد: ${actionState.paidPlanTitle.ifBlank { actionState.memberRoleBadge }}"
                            } else {
                                "🔒 حالة الحساب: عضو عادي (غير مشترك في الباقات المدفوعة)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.testTag("vip_payment_status_title")
                        )
                        Text(
                            text = if (actionState.isPaidVip) {
                                "تم التحقق من إيصال الدفع الفعلي وتفعيل جميع مزايا VIP لحسابك"
                            } else {
                                "تم إيقاف التفعيل المجاني عند الضغط — يجب إتمام عملية دفع حقيقية وموثقة لتفعيل VIP"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 1. Choose Plan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = secondaryCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1️⃣ اختر الباقة المطلوبة:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VIP_PAYMENT_PLANS.forEach { plan ->
                            val isSelected = selectedPlan.planId == plan.planId
                            Surface(
                                color = if (isSelected) {
                                    WanasDayDarkHeader
                                } else if (isDarkMode) {
                                    Color.White.copy(alpha = 0.06f)
                                } else {
                                    WanasDaySurfaceVariant
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) WanasAmberGold else WanasTealCalm.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPlan = plan }
                                    .testTag("select_plan_${plan.planId}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = plan.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) WanasAmberGold else textPrimary
                                    )
                                    Text(
                                        text = "${plan.amountEgp} ج.م",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) WanasEmeraldOnline else WanasTealDeep
                                    )
                                    Text(
                                        text = plan.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else textSub
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Payment Method & Strict Validation Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_form_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = secondaryCardBg),
                border = BorderStroke(1.5.dp, WanasAmberGold.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2️⃣ اختر وسيلة الدفع وأدخل بيانات الدفع الحقيقية:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val methods = listOf(
                            "BANK_CARD" to "💳 بطاقة بنكية",
                            "VODAFONE_CASH" to "📱 فودافون كاش",
                            "INSTAPAY" to "⚡ إنستا باي"
                        )
                        methods.forEach { (code, label) ->
                            val isChosen = selectedPaymentMethod == code
                            Surface(
                                color = if (isChosen) WanasAmberGold else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, WanasAmberGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPaymentMethod = code }
                                    .testTag("payment_method_$code")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isChosen) Color(0xFF1A103C) else textPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (selectedPaymentMethod == "BANK_CARD") {
                        OutlinedTextField(
                            value = cardHolderName,
                            onValueChange = { cardHolderName = it },
                            label = { Text("اسم صاحب البطاقة البنكية بالكامل") },
                            placeholder = { Text("مثال: AHMED ELMASRY") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_card_holder_input")
                        )

                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("رقم البطاقة البنكية (16 رقماً صحيحاً)") },
                            placeholder = { Text("4532 XXXX XXXX XXXX") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_card_number_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = expiryMmYy,
                                onValueChange = { expiryMmYy = it },
                                label = { Text("تاريخ الانتهاء (MM/YY)") },
                                placeholder = { Text("08/28") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_card_expiry_input")
                            )

                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it },
                                label = { Text("رمز CVV") },
                                placeholder = { Text("123") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("payment_card_cvv_input")
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = walletPhone,
                            onValueChange = { walletPhone = it },
                            label = { Text("رقم المحفظة المحول منها (11 رقماً)") },
                            placeholder = { Text("01012345678") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_wallet_phone_input")
                        )

                        OutlinedTextField(
                            value = transferReferenceNumber,
                            onValueChange = { transferReferenceNumber = it },
                            label = { Text("رقم العملية / المرجع البنكي للتحويل") },
                            placeholder = { Text("مثال: TXN98451203") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_transfer_ref_input")
                        )
                    }

                    Button(
                        onClick = {
                            val ok = onProcessRealPayment(
                                selectedPlan,
                                selectedPaymentMethod,
                                cardHolderName,
                                cardNumber,
                                expiryMmYy,
                                cvv,
                                walletPhone,
                                transferReferenceNumber
                            )
                            if (ok) {
                                cardNumber = ""
                                cvv = ""
                                transferReferenceNumber = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_real_payment_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WanasEmeraldOnline,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تأكيد الدفع الفعلي (${selectedPlan.amountEgp} ج.م) وتفعيل الباقة",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 3. Verified Payment Receipts List
        if (actionState.paymentReceipts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verified_receipts_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = secondaryCardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🧾 إيصالات الدفع المؤكدة (${actionState.paymentReceipts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        HorizontalDivider(color = textSub.copy(alpha = 0.2f))
                        actionState.paymentReceipts.forEach { receipt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${receipt.planTitle} (${receipt.amountEgp} ج.م)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "المرجع: ${receipt.transactionReference}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDarkMode) WanasTealCalm else WanasTealDeep
                                    )
                                }
                                Text(
                                    text = if (receipt.timestampMillis > 0L) {
                                        dateFormatter.format(Date(receipt.timestampMillis))
                                    } else {
                                        "مؤكد الآن"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WanasEmeraldOnline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
