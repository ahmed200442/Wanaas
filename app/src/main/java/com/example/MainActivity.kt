package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.data.ChatRoomMetadata
import com.example.data.ChatRoomRepository
import com.example.data.PushNotificationEvent
import com.example.data.RoomActiveMember
import com.example.data.RoomPresenceBannerEvent
import com.example.data.SupabaseAccountService
import com.example.data.SupabaseMemberAccount
import com.example.notifications.PushNotificationHelper
import com.example.ui.screens.FadfadaAnonymousScreen
import com.example.ui.screens.PushNotificationsAndPaymentScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WanasAmberGold
import com.example.ui.theme.WanasCardBg
import com.example.ui.theme.WanasCoralWarm
import com.example.ui.theme.WanasDayDarkEmerald
import com.example.ui.theme.WanasDayDarkHeader
import com.example.ui.theme.WanasDayLightBg
import com.example.ui.theme.WanasDayLightSurface
import com.example.ui.theme.WanasDaySurfaceVariant
import com.example.ui.theme.WanasDayTextPrimary
import com.example.ui.theme.WanasDayTextSecondary
import com.example.ui.theme.WanasDeepBg
import com.example.ui.theme.WanasEmeraldOnline
import com.example.ui.theme.WanasTealCalm
import com.example.ui.theme.WanasTealDeep
import com.example.ui.theme.WanasVioletAccent
import com.example.viewmodel.ChatRoomViewModel
import com.example.viewmodel.UiState
import com.example.viewmodel.attemptAutoSignIn
import com.example.viewmodel.onGoogleSignInClicked
import com.example.viewmodel.signOut
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(false) } // Default to Daytime Mode (الوضع النهاري)
            MyApplicationTheme(darkTheme = isDarkMode) {
                AppNavigation(
                    isDarkMode = isDarkMode,
                    onToggleTheme = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    auth: FirebaseAuth? = null
) {
    val context = LocalContext.current
    val supabaseService = remember(context) { SupabaseAccountService(context) }

    // Firebase may be unavailable when google-services.json is intentionally
    // omitted. Never evaluate Firebase.auth as a default parameter during the
    // first composition because that can crash before the first frame.
    val safeFirebaseAuth = remember {
        auth ?: runCatching { Firebase.auth }.getOrNull()
    }
    val currentUser by if (safeFirebaseAuth != null) {
        safeFirebaseAuth.authStateFlow().collectAsStateWithLifecycle(
            initialValue = safeFirebaseAuth.currentUser
        )
    } else {
        remember { mutableStateOf<FirebaseUser?>(null) }
    }

    // Automatically restore saved member account on startup so the user enters directly without typing again
    var savedMemberAccount by remember {
        mutableStateOf(supabaseService.getSavedAccountForAutoLogin())
    }

    val activeUserId = currentUser?.uid ?: savedMemberAccount?.userId
    val activeDisplayName = savedMemberAccount?.displayName
        ?: currentUser?.displayName
        ?: supabaseService.getLastSavedNickname()
    val activeEmail = savedMemberAccount?.email
        ?: currentUser?.email
        ?: supabaseService.getLastSavedEmail()

    if (activeUserId == null) {
        AuthGateScreen(
            isDarkMode = isDarkMode,
            onToggleTheme = onToggleTheme,
            supabaseService = supabaseService,
            onMemberAuthenticated = { memberAccount ->
                savedMemberAccount = memberAccount
            }
        )
    } else {
        ChatRoomMetadataScreen(
            currentUserId = activeUserId,
            currentUserDisplayName = activeDisplayName,
            currentUserEmail = activeEmail,
            isDarkMode = isDarkMode,
            onToggleTheme = onToggleTheme,
            supabaseService = supabaseService,
            onSignedOut = {
                savedMemberAccount = null
            }
        )
    }
}

@Composable
fun AuthGateScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    supabaseService: SupabaseAccountService,
    onMemberAuthenticated: (SupabaseMemberAccount) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isRegisterTab by rememberSaveable { mutableStateOf(true) }
    var nicknameInput by rememberSaveable { mutableStateOf(supabaseService.getLastSavedNickname()) }
    var emailInput by rememberSaveable { mutableStateOf(supabaseService.getLastSavedEmail()) }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val backgroundBrush = if (isDarkMode) {
        Brush.verticalGradient(listOf(WanasDeepBg, WanasCardBg))
    } else {
        Brush.verticalGradient(listOf(WanasDayLightBg, Color(0xFFEDE9FE), Color(0xFFFFFBEB)))
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = WanasAmberGold,
        unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
        focusedLabelColor = WanasAmberGold,
        unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
        cursorColor = WanasAmberGold
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen_root"),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkMode) WanasCardBg else WanasDayDarkHeader
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = "غرف الدردشة",
                                tint = WanasAmberGold
                            )
                            Text(
                                text = "وَنَس — تسجيل حساب جديد والدخول التلقائي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = WanasAmberGold
                            )
                        }

                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("auth_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تبديل الوضع النهاري والليلي",
                                tint = WanasAmberGold
                            )
                        }
                    }

                    // Mode Switcher: تسجيل حساب جديد vs تسجيل الدخول
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isRegisterTab = true
                                authError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("tab_register_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegisterTab) WanasAmberGold else Color.White.copy(alpha = 0.12f),
                                contentColor = if (isRegisterTab) Color(0xFF1A103C) else Color.White
                            )
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل حساب جديد", fontWeight = FontWeight.ExtraBold)
                        }

                        Button(
                            onClick = {
                                isRegisterTab = false
                                authError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("tab_login_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRegisterTab) WanasTealCalm else Color.White.copy(alpha = 0.12f),
                                contentColor = if (!isRegisterTab) Color(0xFF00201D) else Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الدخول", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // 1. Name or Nickname field (اسم أو لقب)
                    if (isRegisterTab) {
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text("الاسم أو اللقب") },
                            placeholder = { Text("مثال: أحمد المصري") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = WanasAmberGold)
                            },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_nickname_input")
                        )
                    }

                    // 2. Email field (الإيميل)
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("البريد الإلكتروني (الإيميل)") },
                        placeholder = { Text("example@email.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = WanasAmberGold)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email_input")
                    )

                    // 3. Password field (الباسورد)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("كلمة المرور (الباسورد)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = WanasAmberGold)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password_input")
                    )

                    // 4. Confirm Password field (تأكيد الباسورد)
                    if (isRegisterTab) {
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            label = { Text("تأكيد كلمة المرور (تأكيد الباسورد)") },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WanasTealCalm)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )
                    }

                    Text(
                        text = "✅ يتم حفظ بيانات حسابك تلقائياً للدخول أوتوماتيك في المرات القادمة بدون كتابتها مرة أخرى.",
                        style = MaterialTheme.typography.labelSmall,
                        color = WanasEmeraldOnline,
                        fontWeight = FontWeight.Bold
                    )

                    // Submit Register / Login Button
                    Button(
                        onClick = {
                            isLoading = true
                            authError = null
                            scope.launch {
                                val result = if (isRegisterTab) {
                                    supabaseService.registerMemberAccount(
                                        nickname = nicknameInput,
                                        email = emailInput,
                                        password = passwordInput,
                                        confirmPassword = confirmPasswordInput,
                                        preferredUid = Firebase.auth.currentUser?.uid
                                    )
                                } else {
                                    supabaseService.loginMemberAccount(
                                        email = emailInput,
                                        password = passwordInput,
                                        fallbackNickname = nicknameInput
                                    )
                                }
                                isLoading = false
                                result.fold(
                                    onSuccess = { account ->
                                        onMemberAuthenticated(account)
                                    },
                                    onFailure = { err ->
                                        authError = err.localizedMessage ?: "تعذر إتمام العملية"
                                    }
                                )
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_member_account_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WanasEmeraldOnline,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isRegisterTab) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRegisterTab) {
                                    "تسجيل حساب جديد وحفظ الدخول التلقائي"
                                } else {
                                    "دخول وحفظ البيانات تلقائياً"
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    // Google Sign-In Button
                    Button(
                        onClick = {
                            isLoading = true
                            authError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    val fbUser = Firebase.auth.currentUser
                                    if (fbUser != null) {
                                        val googleAccount = SupabaseMemberAccount(
                                            userId = fbUser.uid,
                                            displayName = fbUser.displayName?.ifBlank { null }
                                                ?: nicknameInput.ifBlank { "عضو ونس" },
                                            email = fbUser.email ?: emailInput,
                                            supabaseSynced = true
                                        )
                                        supabaseService.saveAccountForAutoLogin(googleAccount)
                                        onMemberAuthenticated(googleAccount)
                                    }
                                },
                                onAuthError = { err ->
                                    isLoading = false
                                    authError = err
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WanasAmberGold,
                            contentColor = Color(0xFF1A103C)
                        )
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Google")
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Sign in with Google", fontWeight = FontWeight.ExtraBold)
                    }

                    if (authError != null) {
                        Surface(
                            color = WanasCoralWarm.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Text(
                                text = authError!!,
                                modifier = Modifier.padding(12.dp),
                                color = WanasCoralWarm,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomMetadataScreen(
    currentUserId: String,
    currentUserDisplayName: String = "",
    currentUserEmail: String = "",
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    supabaseService: SupabaseAccountService = SupabaseAccountService(LocalContext.current),
    onSignedOut: () -> Unit = {},
    viewModel: ChatRoomViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChatRoomViewModel(
                    repository = ChatRoomRepository(db),
                    currentUserId = currentUserId,
                    currentUserDisplayName = currentUserDisplayName,
                    currentUserEmail = currentUserEmail,
                    supabaseService = SupabaseAccountService(app),
                    pushNotificationHelper = PushNotificationHelper(app)
                )
            }
        }
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val roomsState by viewModel.roomsState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    var roomNameInput by remember { mutableStateOf("") }
    var memberNameInput by remember(actionState.memberDisplayName) {
        mutableStateOf(actionState.memberDisplayName)
    }
    var showProfileView by rememberSaveable { mutableStateOf(false) }
    var showFadfadaView by rememberSaveable { mutableStateOf(false) }
    var showNotificationsPaymentView by rememberSaveable { mutableStateOf(false) }
    var notificationsPaymentInitialTab by rememberSaveable { mutableStateOf(0) }

    // Handle system Back press when inside an active room
    if (actionState.activeRoom != null && !showProfileView && !showFadfadaView && !showNotificationsPaymentView) {
        BackHandler {
            viewModel.leaveCurrentRoom()
        }
    }

    val screenBackgroundBrush = if (isDarkMode) {
        Brush.verticalGradient(listOf(WanasDeepBg, Color(0xFF171033)))
    } else {
        Brush.verticalGradient(
            listOf(
                WanasDayLightBg,
                Color(0xFFEDE9FE),
                Color(0xFFFFFBEB)
            )
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_room_screen_root"),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush)
                .padding(innerPadding)
        ) {
            if (showProfileView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    UserProfileScreen(
                        userId = currentUserId,
                        actionState = actionState,
                        isDarkMode = isDarkMode,
                        onSaveProfile = { name, uri, emoji, bio ->
                            viewModel.updateFullMemberProfile(name, uri, emoji, bio)
                        },
                        onSendQuickChatMessage = { msg ->
                            viewModel.sendChatMessageInActiveRoom(msg)
                        },
                        onBackToRooms = { showProfileView = false }
                    )
                }
            } else if (showFadfadaView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    FadfadaAnonymousScreen(
                        currentUserId = currentUserId,
                        actionState = actionState,
                        isDarkMode = isDarkMode,
                        onPublishFadfada = { content, moodTag ->
                            viewModel.publishAnonymousFadfada(content, moodTag)
                        },
                        onSendHeart = { post ->
                            viewModel.sendHeartToFadfada(post)
                        },
                        onDeletePost = { postId ->
                            viewModel.deleteFadfadaPost(postId)
                        },
                        onToggleAdminOwnerMode = { enabled ->
                            viewModel.setAdminOwnerViewMode(enabled)
                        },
                        onBackToRooms = { showFadfadaView = false }
                    )
                }
            } else if (showNotificationsPaymentView) {
                val availableRooms = (roomsState as? UiState.Success<List<ChatRoomMetadata>>)?.data.orEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    PushNotificationsAndPaymentScreen(
                        actionState = actionState,
                        availableRooms = availableRooms,
                        isDarkMode = isDarkMode,
                        initialTabIndex = notificationsPaymentInitialTab,
                        onSendFriendInvitePush = { friend, room, msg ->
                            viewModel.inviteFriendToChatRoom(friend, room, msg)
                        },
                        onSendRoomMessagePush = { msg ->
                            viewModel.sendChatMessageInActiveRoom(msg)
                        },
                        onProcessRealPayment = { plan, method, holder, card, expiry, cvv, phone, ref ->
                            viewModel.processRealPaymentCheckout(
                                plan = plan,
                                paymentMethod = method,
                                cardHolderName = holder,
                                cardNumber = card,
                                expiryMmYy = expiry,
                                cvv = cvv,
                                walletPhone = phone,
                                transferReferenceNumber = ref
                            )
                        },
                        onBackToRooms = { showNotificationsPaymentView = false }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Dark Royal Header Card (contrasts vividly in Daytime Mode and Night Mode)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("header_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isDarkMode) WanasCardBg else WanasDayDarkHeader
                        )
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
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = "غرف الدردشة",
                                        tint = WanasAmberGold
                                    )
                                    Column {
                                        Text(
                                            text = "وَنَس — غرف الدردشة المباشرة",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = WanasAmberGold
                                        )
                                        Text(
                                            text = if (isDarkMode) {
                                                "🌙 الوضع الليلي الفاخر"
                                            } else {
                                                "☀️ الوضع النهاري (تباين ألوان غامقة وفاتحة)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { showFadfadaView = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = WanasVioletAccent,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("open_fadfada_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VisibilityOff,
                                            contentDescription = "قسم فضفضة بدون اسم",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "فضفضة",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { showProfileView = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = WanasTealCalm,
                                            contentColor = Color(0xFF00201D)
                                        ),
                                        modifier = Modifier.testTag("open_profile_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "الملف الشخصي والإحصائيات",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ملفي",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = onToggleTheme,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = WanasAmberGold,
                                            contentColor = Color(0xFF1A103C)
                                        ),
                                        modifier = Modifier.testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "تغيير ألوان التطبيق",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isDarkMode) "نهاري" else "ليلي",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            signOut(
                                                credentialManager = credentialManager,
                                                supabaseService = supabaseService,
                                                onSignOutComplete = onSignedOut,
                                                scope = scope
                                            )
                                        },
                                        modifier = Modifier.testTag("sign_out_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "تسجيل الخروج",
                                            tint = WanasCoralWarm
                                        )
                                    }
                                }
                            }

                            // Supabase Member Account Link Bar
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, WanasTealCalm.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showProfileView = true }
                                    .testTag("supabase_account_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(WanasAmberGold.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (actionState.memberAvatarUri.isNotBlank()) {
                                                    AsyncImage(
                                                        model = actionState.memberAvatarUri,
                                                        contentDescription = "صورة العضو",
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(
                                                        text = actionState.memberAvatarEmoji,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = "العضو: ${actionState.memberDisplayName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = actionState.supabaseStatusText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = WanasEmeraldOnline
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.syncMemberAccountWithSupabaseAndCloud() },
                                            modifier = Modifier.testTag("sync_supabase_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "مزامنة حساب العضو مع Supabase",
                                                tint = WanasTealCalm
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = memberNameInput,
                                            onValueChange = { memberNameInput = it },
                                            label = { Text("الاسم أو اللقب في الغرف", color = Color.White.copy(alpha = 0.8f)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("member_name_input")
                                        )
                                        Button(
                                            onClick = { viewModel.updateMemberProfileName(memberNameInput) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = WanasTealCalm,
                                                contentColor = Color(0xFF00201D)
                                            ),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("save_member_name_button")
                                        ) {
                                            Text("تحديث", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }

                                    // V4 Modern Hub: AI + Smart Rooms + Messenger + Games + Friends + Voice + Economy
                                    FilledTonalButton(
                                        onClick = { context.startActivity(android.content.Intent(context, WanasV4Activity::class.java)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("open_v4_hub_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = WanasVioletAccent,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = "✨ وَنَس V4 — AI • غرف ذكية • Messenger • ألعاب • أصدقاء",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // Quick Actions: Push Notifications / Friend Invite & Real VIP Payment Checkout
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                notificationsPaymentInitialTab = 0
                                                showNotificationsPaymentView = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("open_push_notifications_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = WanasAmberGold,
                                                contentColor = Color(0xFF1A103C)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = "الإشعارات الفورية ودعوة صديق",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "الإشعارات ودعوة صديق",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                notificationsPaymentInitialTab = 1
                                                showNotificationsPaymentView = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .testTag("open_real_payment_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = WanasEmeraldOnline,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = "الدفع الحقيقي VIP",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (actionState.isPaidVip) "✅ باقة VIP مفعلة" else "الدفع الحقيقي VIP",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Transient Auto-Hiding Join / Leave Banner ("دخل العضو فلان" / "خرج العضو فلان")
                    AnimatedVisibility(
                        visible = actionState.transientPresenceBanner != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        val event = actionState.transientPresenceBanner
                        if (event != null) {
                            RoomPresenceTransientBanner(
                                event = event,
                                onDismiss = { viewModel.dismissPresenceBanner() }
                            )
                        }
                    }

                    // 3. Active Room Stage & Present Members Panel (when user has entered a room)
                    val currentActiveRoom = actionState.activeRoom
                    if (currentActiveRoom != null) {
                        ActiveRoomMembersPanel(
                            room = currentActiveRoom,
                            activeMembers = actionState.activeRoomMembers,
                            isDarkMode = isDarkMode,
                            onLeaveRoom = { viewModel.leaveCurrentRoom() }
                        )
                    }

                    // 4. Create New Room Card (Light surface in Daytime Mode with Deep Teal/Dark accents)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("create_room_card"),
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
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "إضافة غرفة دردشة جديدة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) WanasTealCalm else WanasDayDarkHeader
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = roomNameInput,
                                    onValueChange = { roomNameInput = it },
                                    label = { Text("اسم الغرفة (roomName)") },
                                    placeholder = { Text("مثال: 🌙 سهرة مصرية ونس") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("room_name_input")
                                )

                                Button(
                                    onClick = {
                                        viewModel.createChatRoom(roomNameInput)
                                        roomNameInput = ""
                                    },
                                    enabled = !actionState.isSubmitting,
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("create_room_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkMode) WanasEmeraldOnline else WanasDayDarkHeader,
                                        contentColor = if (isDarkMode) Color.White else WanasAmberGold
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ الغرفة", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    // 5. Status / Error Feedback
                    val feedback = actionState.errorMessage ?: actionState.statusMessage
                    if (feedback != null) {
                        Surface(
                            color = if (actionState.errorMessage != null) {
                                WanasCoralWarm.copy(alpha = 0.18f)
                            } else if (isDarkMode) {
                                WanasEmeraldOnline.copy(alpha = 0.2f)
                            } else {
                                WanasDayDarkEmerald
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .testTag("status_banner")
                        ) {
                            Text(
                                text = feedback,
                                modifier = Modifier.padding(12.dp),
                                color = if (actionState.errorMessage != null) {
                                    WanasCoralWarm
                                } else if (isDarkMode) {
                                    WanasEmeraldOnline
                                } else {
                                    Color.White
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // 6. Real-time List of Chat Rooms (Alternating Dark & Light Cards in Daytime Mode)
                    when (val state = roomsState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = if (isDarkMode) WanasAmberGold else WanasDayDarkHeader
                                )
                            }
                        }
                        is UiState.Error -> {
                            Surface(
                                color = WanasCoralWarm.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                                    .testTag("rooms_error_banner")
                            ) {
                                Text(
                                    text = state.message,
                                    modifier = Modifier.padding(12.dp),
                                    color = WanasCoralWarm,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is UiState.Success -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 600.dp)
                                    .weight(1f)
                                    .testTag("chat_rooms_list"),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.data, key = { it.roomId }) { room ->
                                    val isInsideThisRoom = actionState.activeRoom?.roomId == room.roomId
                                    ChatRoomMetadataItemCard(
                                        room = room,
                                        isDarkMode = isDarkMode,
                                        isInsideThisRoom = isInsideThisRoom,
                                        canDelete = room.creatorId == currentUserId,
                                        onEnterRoom = { viewModel.enterChatRoom(room) },
                                        onLeaveRoom = { viewModel.leaveCurrentRoom() },
                                        onDelete = { viewModel.deleteChatRoom(room.roomId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Floating auto-dismissing notification banner when a member enters ("دخل العضو فلان")
 * or leaves ("خرج العضو فلان") a chat room.
 */
@Composable
private fun RoomPresenceTransientBanner(
    event: RoomPresenceBannerEvent,
    onDismiss: () -> Unit
) {
    val isJoin = event.isJoinEvent
    val bannerBg = if (isJoin) WanasDayDarkEmerald else Color(0xFF4C0519)
    val accentColor = if (isJoin) WanasAmberGold else WanasCoralWarm

    Surface(
        color = bannerBg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.5.dp, accentColor),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable { onDismiss() }
            .testTag("presence_event_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = event.avatarEmoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column {
                    Text(
                        text = event.bannerText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("presence_event_text")
                    )
                    Text(
                        text = if (isJoin) {
                            "✨ إشعار دخول للغرفة (يختفي تلقائياً)"
                        } else {
                            "👋 إشعار مغادرة الغرفة (يختفي تلقائياً)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }
            }

            Icon(
                imageVector = if (isJoin) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = accentColor
            )
        }
    }
}

/**
 * Displays the active room header and the live list of members currently inside the room.
 */
@Composable
private fun ActiveRoomMembersPanel(
    room: ChatRoomMetadata,
    activeMembers: List<RoomActiveMember>,
    isDarkMode: Boolean,
    onLeaveRoom: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("active_room_members_panel"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) Color(0xFF231746) else WanasDayDarkHeader
        )
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
                        imageVector = Icons.Default.Group,
                        contentDescription = "الأعضاء المتواجدون",
                        tint = WanasAmberGold
                    )
                    Column {
                        Text(
                            text = "داخل الغرفة الآن: ${room.roomName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = WanasAmberGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "الأعضاء الموجودين بالغرفة (${activeMembers.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = WanasEmeraldOnline,
                            modifier = Modifier.testTag("active_members_count")
                        )
                    }
                }

                Button(
                    onClick = onLeaveRoom,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WanasCoralWarm,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("leave_active_room_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "خروج من الغرفة",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروج من الغرفة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Horizontal list of members currently present inside the room
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_members_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeMembers, key = { it.userId }) { member ->
                    Surface(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else WanasDayLightSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, WanasAmberGold.copy(alpha = 0.65f)),
                        modifier = Modifier.testTag("active_member_chip_${member.userId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(WanasAmberGold.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = member.avatarEmoji)
                            }
                            Column {
                                Text(
                                    text = member.memberName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else WanasDayTextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(WanasEmeraldOnline)
                                    )
                                    Text(
                                        text = "${member.roleBadge} • Supabase",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDarkMode) WanasTealCalm else WanasTealDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRoomMetadataItemCard(
    room: ChatRoomMetadata,
    isDarkMode: Boolean,
    isInsideThisRoom: Boolean,
    canDelete: Boolean,
    onEnterRoom: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(room.timestampMillis) {
        val ms = if (room.timestampMillis > 0L) room.timestampMillis else System.currentTimeMillis()
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
    }

    val cardBackground = when {
        isInsideThisRoom && !isDarkMode -> WanasDayDarkEmerald
        isInsideThisRoom && isDarkMode -> Color(0xFF064E3B)
        !isDarkMode -> WanasDaySurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val titleColor = when {
        isInsideThisRoom -> WanasAmberGold
        !isDarkMode -> WanasDayDarkHeader
        else -> WanasAmberGold
    }

    val subtitleColor = when {
        isInsideThisRoom -> Color.White.copy(alpha = 0.9f)
        !isDarkMode -> WanasDayTextSecondary
        else -> Color.White.copy(alpha = 0.85f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_item_${room.roomId}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(
            width = if (isInsideThisRoom) 2.dp else 1.dp,
            color = if (isInsideThisRoom) WanasAmberGold else WanasVioletAccent.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = room.roomName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                    Text(
                        text = "Creator ID: ${room.creatorId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor
                    )
                    Text(
                        text = "Timestamp: $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isInsideThisRoom || isDarkMode) WanasTealCalm else WanasTealDeep
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_room_${room.roomId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الغرفة",
                            tint = WanasCoralWarm
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInsideThisRoom) {
                    OutlinedButton(
                        onClick = onLeaveRoom,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, WanasCoralWarm),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.testTag("leave_room_${room.roomId}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = WanasCoralWarm,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مغادرة الغرفة", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onEnterRoom,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) WanasAmberGold else WanasDayDarkHeader,
                            contentColor = if (isDarkMode) Color(0xFF1A103C) else WanasAmberGold
                        ),
                        modifier = Modifier.testTag("enter_room_${room.roomId}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("دخول الغرفة", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
