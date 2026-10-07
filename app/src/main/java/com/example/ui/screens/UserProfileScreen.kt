package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.MemberChatHistoryEntry
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

private val AVATAR_EMOJI_OPTIONS = listOf("👑", "🦁", "🦅", "🌟", "🎙️", "💎", "🔥", "🌙")

@Composable
fun UserProfileScreen(
    userId: String,
    actionState: ChatRoomsActionState,
    isDarkMode: Boolean,
    onSaveProfile: (displayName: String, avatarUri: String, avatarEmoji: String, bio: String) -> Unit,
    onSendQuickChatMessage: (String) -> Unit,
    onBackToRooms: () -> Unit
) {
    BackHandler {
        onBackToRooms()
    }

    var displayNameInput by rememberSaveable(actionState.memberDisplayName) {
        mutableStateOf(actionState.memberDisplayName)
    }
    var avatarUriInput by rememberSaveable(actionState.memberAvatarUri) {
        mutableStateOf(actionState.memberAvatarUri)
    }
    var selectedEmoji by rememberSaveable(actionState.memberAvatarEmoji) {
        mutableStateOf(actionState.memberAvatarEmoji)
    }
    var bioInput by rememberSaveable(actionState.memberBio) {
        mutableStateOf(actionState.memberBio)
    }
    var quickMessageInput by rememberSaveable { mutableStateOf("") }

    // Zero-permission Android Photo Picker for selecting a custom avatar image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val uriString = uri.toString()
            avatarUriInput = uriString
            onSaveProfile(displayNameInput, uriString, selectedEmoji, bioInput)
        }
    }

    val stats = actionState.personalStats

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_profile_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Profile Hero Card with Custom Avatar Image Picker & Display Name Editor
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("profile_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkMode) WanasCardBg else WanasDayDarkHeader
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onBackToRooms,
                                modifier = Modifier.testTag("profile_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "العودة للغرف",
                                    tint = WanasAmberGold
                                )
                            }
                            Text(
                                text = "الملف الشخصي للعضو والإحصائيات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = WanasAmberGold
                            )
                        }

                        Surface(
                            color = WanasEmeraldOnline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, WanasEmeraldOnline)
                        ) {
                            Text(
                                text = actionState.memberRoleBadge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = WanasEmeraldOnline
                            )
                        }
                    }

                    // Custom Avatar Image / Emoji Circle
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(WanasAmberGold.copy(alpha = 0.2f))
                            .border(3.dp, WanasAmberGold, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_avatar_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUriInput.isNotBlank()) {
                            AsyncImage(
                                model = avatarUriInput,
                                contentDescription = "الصورة الرمزية للعضو",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = selectedEmoji,
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }

                    Text(
                        text = displayNameInput.ifBlank { "عضو ونس" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("profile_display_name_title")
                    )

                    Text(
                        text = bioInput,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    // Photo Picker Button + Emoji Avatar Selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = WanasAmberGold,
                                contentColor = Color(0xFF1A103C)
                            ),
                            modifier = Modifier.testTag("pick_custom_avatar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "اختيار صورة رمزية مخصصة",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار صورة من المعرض", fontWeight = FontWeight.ExtraBold)
                        }

                        if (avatarUriInput.isNotBlank()) {
                            FilledTonalButton(
                                onClick = {
                                    avatarUriInput = ""
                                    onSaveProfile(displayNameInput, "", selectedEmoji, bioInput)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("clear_custom_avatar_button")
                            ) {
                                Text("استخدام رمز", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Quick Avatar Emoji Picker Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("profile_emoji_selector_row")
                    ) {
                        items(AVATAR_EMOJI_OPTIONS) { emoji ->
                            val isSelected = selectedEmoji == emoji
                            Surface(
                                color = if (isSelected) WanasAmberGold else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else WanasAmberGold.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { selectedEmoji = emoji }
                                    .testTag("avatar_emoji_option_$emoji")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Edit Display Name, Custom Avatar URL/URI, and Bio Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("profile_edit_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) WanasCardBg else WanasDayLightSurface
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isDarkMode) WanasTealCalm.copy(alpha = 0.5f) else WanasDayDarkHeader.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isDarkMode) WanasTealCalm else WanasDayDarkHeader
                        )
                        Text(
                            text = "تعديل اسم العرض والصورة الرمزية والنبذة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) WanasTealCalm else WanasDayDarkHeader
                        )
                    }

                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("الاسم أو اللقب (Display Name)") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input")
                    )

                    OutlinedTextField(
                        value = avatarUriInput,
                        onValueChange = { avatarUriInput = it },
                        label = { Text("رابط أو مسار الصورة الرمزية المخصصة (Custom Avatar URL / URI)") },
                        placeholder = { Text("https://example.com/avatar.png أو اختر من المعرض") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_avatar_uri_input")
                    )

                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("النبذة الشخصية (Bio)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    Button(
                        onClick = {
                            onSaveProfile(displayNameInput, avatarUriInput, selectedEmoji, bioInput)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_changes_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) WanasEmeraldOnline else WanasDayDarkHeader,
                            contentColor = if (isDarkMode) Color.White else WanasAmberGold
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التعديلات في الملف الشخصي", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 3. Personal Statistics Grid Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("personal_statistics_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkMode) Color(0xFF231746) else WanasDayDarkHeader
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "إحصائيات العضو",
                            tint = WanasAmberGold
                        )
                        Text(
                            text = "إحصائياتي الشخصية (Personal Statistics)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = WanasAmberGold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatTile(
                            label = "الغرف المنشأة",
                            value = stats.roomsCreatedCount.toString(),
                            accentColor = WanasAmberGold,
                            testTag = "stat_rooms_created",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "مرات دخول الغرف",
                            value = stats.roomsJoinedCount.toString(),
                            accentColor = WanasTealCalm,
                            testTag = "stat_rooms_joined",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileStatTile(
                            label = "الرسائل والنشاط",
                            value = stats.messagesSentCount.toString(),
                            accentColor = WanasEmeraldOnline,
                            testTag = "stat_messages_sent",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatTile(
                            label = "نقاط التفاعل",
                            value = "${stats.totalActivityScore} ⭐",
                            accentColor = WanasCoralWarm,
                            testTag = "stat_activity_score",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Personal Chat & Room Activity History Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("chat_history_section_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) WanasCardBg else WanasDayLightSurface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = WanasVioletAccent.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "سجل الدردشة والنشاط",
                            tint = if (isDarkMode) WanasAmberGold else WanasDayDarkHeader
                        )
                        Text(
                            text = "سجل الدردشة والغرف (Chat & Room History)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) WanasAmberGold else WanasDayDarkHeader
                        )
                    }

                    // Quick chat message composer to log a message in the active/general room
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quickMessageInput,
                            onValueChange = { quickMessageInput = it },
                            label = { Text("إرسال رسالة سريعة إلى سجل الدردشة") },
                            placeholder = { Text("اكتب رسالتك هنا...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_quick_message_input")
                        )

                        Button(
                            onClick = {
                                if (quickMessageInput.isNotBlank()) {
                                    onSendQuickChatMessage(quickMessageInput)
                                    quickMessageInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) WanasTealCalm else WanasDayDarkEmerald,
                                contentColor = if (isDarkMode) Color(0xFF00201D) else Color.White
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("profile_send_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
                        }
                    }

                    if (actionState.chatHistory.isEmpty()) {
                        Surface(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else WanasDaySurfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_chat_history_placeholder")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = if (isDarkMode) WanasTealCalm else WanasTealDeep
                                )
                                Text(
                                    text = "لا توجد رسائل أو نشاطات مسجلة بعد. قم بإنشاء غرفة أو الدخول إليها أو إرسال رسالة لتظهر هنا.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else WanasDayTextSecondary
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("chat_history_items_column")
                        ) {
                            actionState.chatHistory.take(20).forEach { entry ->
                                ChatHistoryItemRow(
                                    entry = entry,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChatHistoryItemRow(
    entry: MemberChatHistoryEntry,
    isDarkMode: Boolean
) {
    val formattedTime = remember(entry.timestampMillis) {
        val ms = if (entry.timestampMillis > 0L) entry.timestampMillis else System.currentTimeMillis()
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
    }

    val badgeLabel = when (entry.activityType) {
        "CREATE_ROOM" -> "إنشاء غرفة"
        "JOIN_ROOM" -> "دخول غرفة"
        "LEAVE_ROOM" -> "مغادرة غرفة"
        else -> "رسالة دردشة"
    }

    val badgeColor = when (entry.activityType) {
        "CREATE_ROOM" -> WanasAmberGold
        "JOIN_ROOM" -> WanasEmeraldOnline
        "LEAVE_ROOM" -> WanasCoralWarm
        else -> WanasTealCalm
    }

    Surface(
        color = if (isDarkMode) Color.White.copy(alpha = 0.06f) else WanasDaySurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${entry.entryId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = if (isDarkMode) WanasAmberGold else WanasDayDarkHeader,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = entry.roomName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) WanasAmberGold else WanasDayDarkHeader
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = badgeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) badgeColor else WanasDayTextPrimary
                    )
                }
            }

            Text(
                text = entry.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) Color.White else WanasDayTextPrimary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkMode) WanasTealCalm else WanasTealDeep
            )
        }
    }
}
