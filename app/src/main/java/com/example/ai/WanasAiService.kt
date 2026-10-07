package com.example.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

object WanasAiService {
    private const val MODEL = "gemini-3.8-flash"
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(MODEL)
    }

    suspend fun generate(prompt: String): Result<String> = runCatching {
        val response = model.generateContent(prompt)
        response.text?.trim().takeUnless { it.isNullOrEmpty() } ?: "لم يصل رد من وَنَس AI."
    }

    suspend fun assistant(userName: String, message: String, history: String) = generate(
        """أنت «وَنَس AI»، مساعد اجتماعي عربي داخل تطبيق وَنَس.
تحدث بالعربية المصرية الودودة، وكن مختصرًا ومفيدًا.
المستخدم اسمه: $userName.
سجل المحادثة:
$history
رسالة المستخدم:
$message"""
    )

    suspend fun roomAssistant(roomTitle: String, transcript: String) = generate(
        """أنت مساعد غرفة صوتية في تطبيق وَنَس.
اسم الغرفة: $roomTitle.
حلل الحوار التالي:
$transcript
أعد النتيجة في 4 أجزاء:
1) ملخص سريع
2) أهم 3 نقاط
3) سؤال تفاعلي جديد
4) اقتراح لاستمرار الحوار"""
    )

    suspend fun moderate(text: String) = generate(
        """أنت مساعد إشراف آمن لتطبيق اجتماعي.
حلل الرسالة دون إصدار عقوبة تلقائية.
صنّفها إلى: آمنة / تحتاج مراجعة / مخالفة واضحة.
اذكر السبب باختصار واقترح الإجراء المناسب للمشرف.
الرسالة:
$text"""
    )

    suspend fun makeGame(topic: String) = generate(
        """أنشئ سؤال لعبة تفاعلية عربية لمستخدمي وَنَس.
الموضوع: $topic.
أرجع السؤال ثم A/B/C/D ثم في سطر منفصل:
الإجابة الصحيحة: الحرف فقط."""
    )

    suspend fun buildProfile(interests: String) = generate(
        """أنشئ Bio جذابًا وقصيرًا لعضو في تطبيق وَنَس.
الاهتمامات: $interests.
اكتب 3 أسطر كحد أقصى بدون مبالغة أو معلومات غير مذكورة."""
    )
}
