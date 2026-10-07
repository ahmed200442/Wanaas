package com.example.ui.screens

import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.WanasAiService
import kotlinx.coroutines.launch
import java.util.Locale

private data class V4Feature(val title: String, val subtitle: String, val emoji: String)
private data class V4ChatMessage(val fromMe: Boolean, val text: String)

@Composable
fun WanasV4HubScreen(currentUserName: String, onBack: () -> Unit) {
    var selectedFeature by remember { mutableStateOf<String?>(null) }
    if (selectedFeature == null) {
        WanasV4Home(currentUserName, onBack) { selectedFeature = it }
    } else {
        WanasV4FeatureScreen(selectedFeature!!, currentUserName) { selectedFeature = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WanasV4Home(currentUserName: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val features = listOf(
        V4Feature("وَنَس AI", "مساعدك الذكي بالنص والصوت", "🤖"),
        V4Feature("AI للغرف", "تلخيص الحوار واقتراح مواضيع", "🧠"),
        V4Feature("الغرف الذكية", "اقتراح غرف مناسبة لك", "🎯"),
        V4Feature("AI Moderator", "مساعدة المشرف في حماية الغرفة", "🛡️"),
        V4Feature("Messenger", "رسائل + Reactions + Buzz", "💬"),
        V4Feature("الأصدقاء و AI Match", "اكتشف أشخاصًا باهتمامات مشابهة", "👥"),
        V4Feature("صوتيات ذكية", "تحويل الكلام لنص ورد صوتي", "🎙️"),
        V4Feature("AI Profile", "أنشئ Bio ذكيًا", "✨"),
        V4Feature("AI Games", "أسئلة وتحديات لا تنتهي", "🎮"),
        V4Feature("Levels & Badges", "خبرة ومراتب وشارات", "🏆"),
        V4Feature("Coins & Gifts", "اقتصاد اجتماعي داخل وَنَس", "🎁"),
        V4Feature("Premium", "مزايا VIP قابلة للتوسع", "💎")
    )
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text("وَنَس V4", fontWeight = FontWeight.ExtraBold)
                    Text("الجيل الجديد من الونسة الذكية", style = MaterialTheme.typography.labelSmall)
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أهلًا " + currentUserName + " 👋", fontWeight = FontWeight.ExtraBold)
                        Text("AI، غرف ذكية، رسائل، ألعاب واقتصاد اجتماعي في مكان واحد.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { onOpen("وَنَس AI") }, label = { Text("جرّب AI") })
                            AssistChip(onClick = { onOpen("AI للغرف") }, label = { Text("حلّل غرفة") })
                            AssistChip(onClick = { onOpen("AI Games") }, label = { Text("ابدأ لعبة") })
                        }
                    }
                }
            }
            item { Text("مركز الميزات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
            items(features) { item ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(item.title) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Text(item.emoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.ExtraBold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WanasV4FeatureScreen(feature: String, currentUserName: String, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(feature, fontWeight = FontWeight.ExtraBold) },
            navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } }
        )
    }) { padding ->
        when (feature) {
            "وَنَس AI" -> AiAssistantPanel(currentUserName, Modifier.padding(padding))
            "AI للغرف" -> AiRoomPanel(Modifier.padding(padding))
            "AI Moderator" -> AiModeratorPanel(Modifier.padding(padding))
            "AI Games" -> AiGamesPanel(Modifier.padding(padding))
            "AI Profile" -> AiProfilePanel(Modifier.padding(padding))
            "صوتيات ذكية" -> SmartVoicePanel(Modifier.padding(padding))
            "Messenger" -> MessengerPanel(Modifier.padding(padding))
            "الأصدقاء و AI Match" -> FriendsPanel(Modifier.padding(padding))
            "الغرف الذكية" -> SmartRoomsPanel(Modifier.padding(padding))
            "Levels & Badges" -> GamificationPanel(Modifier.padding(padding))
            "Coins & Gifts" -> EconomyPanel(Modifier.padding(padding))
            "Premium" -> PremiumPanel(Modifier.padding(padding))
            else -> Text("الميزة غير متاحة", modifier = Modifier.padding(24.dp))
        }
    }
}

@Composable
private fun AiAssistantPanel(name: String, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf(V4ChatMessage(false, "أنا وَنَس AI 🤖 اسألني عن الغرف أو الألعاب أو أي موضوع.")) }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (msg.fromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(msg.text, Modifier.padding(14.dp)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), label = { Text("اكتب رسالتك") }, singleLine = true)
            FilledTonalButton(enabled = input.isNotBlank() && !loading, onClick = {
                val userText = input.trim()
                input = ""
                messages += V4ChatMessage(true, userText)
                loading = true
                scope.launch {
                    val history = messages.takeLast(8).joinToString("\n") {
                        if (it.fromMe) "المستخدم: " + it.text else "AI: " + it.text
                    }
                    val result = WanasAiService.assistant(name, userText, history)
                    messages += V4ChatMessage(false, result.getOrElse { "تعذر الاتصال بـAI حاليًا: " + it.message })
                    loading = false
                }
            }) { Icon(Icons.Default.AutoAwesome, null) }
        }
    }
}

@Composable
private fun AiRoomPanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var roomTitle by remember { mutableStateOf("سهرة وَنَس") }
    var transcript by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(roomTitle, { roomTitle = it }, Modifier.fillMaxWidth(), label = { Text("اسم الغرفة") })
        OutlinedTextField(transcript, { transcript = it }, Modifier.fillMaxWidth().height(180.dp),
            label = { Text("الصق نص الحوار أو التفريغ الصوتي") })
        Button(enabled = transcript.isNotBlank() && !loading, onClick = {
            loading = true
            scope.launch {
                answer = WanasAiService.roomAssistant(roomTitle, transcript).getOrElse { "خطأ: " + it.message }
                loading = false
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Summarize, null)
            Spacer(Modifier.width(8.dp))
            Text("حلّل ولخّص الغرفة")
        }
        if (answer.isNotBlank()) {
            HorizontalDivider()
            Text(answer)
        }
    }
}

@Composable
private fun AiModeratorPanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("المشرف الذكي يقترح الإجراء، ولا ينفذ العقوبة تلقائيًا.", fontWeight = FontWeight.Bold)
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().height(150.dp), label = { Text("رسالة العضو") })
        Button(enabled = text.isNotBlank() && !loading, onClick = {
            loading = true
            scope.launch {
                result = WanasAiService.moderate(text).getOrElse { "تعذر التحليل: " + it.message }
                loading = false
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Security, null)
            Spacer(Modifier.width(8.dp))
            Text("تحليل الرسالة")
        }
        if (result.isNotBlank()) Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        ) { Text(result, Modifier.padding(16.dp)) }
    }
}

@Composable
private fun AiGamesPanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var topic by remember { mutableStateOf("معلومات عامة") }
    var question by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎮 بنك أسئلة AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), label = { Text("موضوع اللعبة") })
        Button(enabled = !loading, onClick = {
            loading = true
            scope.launch {
                question = WanasAiService.makeGame(topic).getOrElse { "تعذر إنشاء سؤال: " + it.message }
                loading = false
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Quiz, null)
            Spacer(Modifier.width(8.dp))
            Text("ولّد سؤالًا جديدًا")
        }
        Text("النقاط الحالية: " + score, fontWeight = FontWeight.Bold)
        if (question.isNotBlank()) Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(question)
                FilledTonalButton(onClick = { score += 10 }) { Text("✅ أجب") }
            }
        }
    }
}

@Composable
private fun AiProfilePanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var interests by remember { mutableStateOf("ألعاب، كرة قدم، موسيقى") }
    var bio by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("✨ AI Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(interests, { interests = it }, Modifier.fillMaxWidth(), label = { Text("اهتماماتك") })
        Button(enabled = interests.isNotBlank() && !loading, onClick = {
            loading = true
            scope.launch {
                bio = WanasAiService.buildProfile(interests).getOrElse { "تعذر الإنشاء: " + it.message }
                loading = false
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AutoAwesome, null)
            Spacer(Modifier.width(8.dp))
            Text("أنشئ Bio ذكي")
        }
        if (bio.isNotBlank()) Text(bio)
    }
}

@Composable
private fun SmartVoicePanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember { TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS } }
    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    DisposableEffect(recognizer) {
        onDispose {
            recognizer?.destroy()
            tts.stop()
            tts.shutdown()
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎙️ صوتيات ذكية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("تكلم باللهجة المصرية وسيتم تحويل كلامك إلى نص، ثم يمكن تشغيل النص بصوت.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = recognizer != null, onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                recognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { listening = true }
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() { listening = false }
                    override fun onError(error: Int) { listening = false }
                    override fun onResults(results: android.os.Bundle?) {
                        transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        listening = false
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        transcript = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
                })
                recognizer?.startListening(intent)
            }) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(6.dp))
                Text(if (listening) "جاري الاستماع…" else "ابدأ الكلام")
            }
            FilledTonalButton(enabled = transcript.isNotBlank() && ttsReady, onClick = {
                tts.language = Locale("ar", "EG")
                tts.speak(transcript, TextToSpeech.QUEUE_FLUSH, null, "wanas-voice")
            }) {
                Icon(Icons.Default.RecordVoiceOver, null)
                Spacer(Modifier.width(6.dp))
                Text("تشغيل صوتي")
            }
        }
        if (transcript.isNotBlank()) Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
        ) { Text(transcript, Modifier.padding(16.dp)) }
    }
}

@Composable
private fun MessengerPanel(modifier: Modifier = Modifier) {
    val messages = remember { mutableStateListOf(V4ChatMessage(false, "أهلًا! دي محادثة وَنَس الجديدة 💬")) }
    var input by remember { mutableStateOf("") }
    var buzzCount by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Messenger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            FilledTonalButton(onClick = { buzzCount++ }) {
                Icon(Icons.Default.Bolt, null)
                Spacer(Modifier.width(4.dp))
                Text("Buzz " + buzzCount)
            }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { msg ->
                Surface(shape = RoundedCornerShape(18.dp),
                    color = if (msg.fromMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(msg.text, Modifier.padding(12.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), label = { Text("رسالة") }, singleLine = true)
            IconButton(onClick = {
                if (input.isNotBlank()) {
                    messages += V4ChatMessage(true, input.trim())
                    input = ""
                }
            }) { Icon(Icons.Default.Add, "إرسال") }
        }
    }
}

@Composable
private fun FriendsPanel(modifier: Modifier = Modifier) {
    val suggestions = listOf("🎮 كريم — ألعاب", "⚽ محمود — كرة قدم", "🎵 سارة — موسيقى", "📚 يوسف — ثقافة")
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("👥 AI Match", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("اقتراحات أولية ويمكن لاحقًا ربطها بتفضيلات العضو.")
        suggestions.forEach { item ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, null)
                    Spacer(Modifier.width(10.dp))
                    Text(item, Modifier.weight(1f))
                    FilledTonalButton(onClick = {}) { Text("اتصال") }
                }
            }
        }
    }
}

@Composable
private fun SmartRoomsPanel(modifier: Modifier = Modifier) {
    val rooms = listOf(
        "🔥 سهرة بعد منتصف الليل — 42 مستمعًا",
        "⚽ الدوري والكورة — 31 مستمعًا",
        "🎮 Gaming Night — 18 مستمعًا",
        "🎵 موسيقى وطلبات — 27 مستمعًا"
    )
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🎯 الغرف الذكية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("المحرك الذكي يقترح غرفًا حسب نشاطك واهتماماتك.")
        rooms.forEach { room ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HeadsetMic, null)
                    Spacer(Modifier.width(10.dp))
                    Text(room, Modifier.weight(1f))
                    FilledTonalButton(onClick = {}) { Text("دخول") }
                }
            }
        }
    }
}

@Composable
private fun GamificationPanel(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🏆 Levels & Badges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المستوى 12", fontWeight = FontWeight.ExtraBold)
                Text("XP 7,240 / 8,000")
                Text("🥉 New • 🥈 Active • 🥇 Popular • 💎 VIP • 👑 Wanas Star")
            }
        }
    }
}

@Composable
private fun EconomyPanel(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎁 Coins & Gifts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Paid, null)
                    Spacer(Modifier.width(8.dp))
                    Text("رصيدك: 2,450 Wanas Coins", fontWeight = FontWeight.ExtraBold)
                }
                HorizontalDivider()
                Text("الهدايا: ❤️ 50 • 🎉 15 • 👑 3")
            }
        }
    }
}

@Composable
private fun PremiumPanel(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("💎 Premium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wanas Premium", fontWeight = FontWeight.ExtraBold)
                Text("شارة Premium، تخصيص البروفايل، أولوية اكتشاف الغرف، ومزايا AI موسعة.")
                Button(onClick = {}) {
                    Icon(Icons.Default.WorkspacePremium, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ترقية")
                }
            }
        }
    }
}
