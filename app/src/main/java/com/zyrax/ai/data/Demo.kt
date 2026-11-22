package com.zyrax.ai.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simple offline replies used when no API key is set. */
object Demo {
    fun reply(p: Persona, l: AppLanguage, input: String): String {
        val words = input.lowercase().split(Regex("[\\s,.!?؟،]+")).filter { it.isNotEmpty() }
        fun has(vararg keys: String) = words.any { it in keys }
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val name = p.displayName
        val female = p == Persona.ZYRAX

        val isTime = has("time", "সময়", "কয়টা", "الساعة", "وقت")
        val isHello = has("hello", "hi", "hey", "হ্যালো", "হাই", "সালাম", "السلام", "مرحبا", "مرحبًا", "أهلا")
        val isName = has("name", "who", "নাম", "اسمك")

        return when (l) {
            AppLanguage.EN -> when {
                isTime -> "It is $time right now."
                isHello -> if (female) "Hello! I'm ZYRAX. Nice to hear from you." else "Greetings. JARVIS at your service."
                isName -> "I'm $name, your personal assistant."
                else -> "I heard you say: \"$input\". I'm in demo mode, so I can only give simple answers. Add your API key in Settings to unlock real AI replies."
            }
            AppLanguage.BN -> when {
                isTime -> "এখন সময় $time।"
                isHello -> if (female) "হ্যালো! আমি ZYRAX। আপনার সাথে কথা বলে ভালো লাগছে।" else "শুভেচ্ছা। JARVIS আপনার সেবায় প্রস্তুত।"
                isName -> "আমি $name, আপনার ব্যক্তিগত সহকারী।"
                else -> "আপনি বললেন: \"$input\"। আমি ডেমো মোডে আছি, তাই শুধু সহজ উত্তর দিতে পারি। আসল AI উত্তরের জন্য সেটিংসে API কী যোগ করুন।"
            }
            AppLanguage.AR -> when {
                isTime -> "الساعة الآن $time."
                isHello -> if (female) "مرحبًا! أنا ZYRAX. سعيدة بالتحدث معك." else "تحياتي. JARVIS في خدمتك."
                isName -> if (female) "أنا $name، مساعدتك الشخصية." else "أنا $name، مساعدك الشخصي."
                else -> "سمعتك تقول: \"$input\". أنا في الوضع التجريبي، لذا أستطيع تقديم إجابات بسيطة فقط. أضف مفتاح API في الإعدادات للحصول على ردود ذكاء اصطناعي حقيقية."
            }
        }
    }
}
