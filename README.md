# ZYRAX AI

Personal Android AI assistant (Kotlin + Jetpack Compose, MVVM).

Features (v0.1): dark UI, ZYRAX (female) and JARVIS (male) personas, AI text chat with your own API key
(Claude or OpenAI), voice input, spoken replies, Bengali / English / Arabic, chat history, basic memory,
settings screen, and a demo mode when no API key is set.

## Get the APK
Push this project to GitHub. Open the repo -> **Actions** -> **Build ZYRAX AI debug APK** -> latest run ->
download the **ZYRAX-AI-debug-apk** artifact (a ZIP containing `app-debug.apk`).

## Open in Android Studio
Open this folder, let Gradle sync, then Run. (Android Studio creates the Gradle wrapper files itself.)

## Notes
- The API key is stored in the app's private storage on the phone. Never commit a key to GitHub.
- Voice quality, gender and available languages depend on the voices installed on your phone.
