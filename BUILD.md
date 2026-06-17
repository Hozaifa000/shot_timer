# Shot Timer — البناء

مشروع Capacitor كامل لتطبيق Shot Timer بنظام أندرويد. هيكل مجلد `android/` مُولّد بالكامل وجاهز للبناء بدون الحاجة لتشغيل `npx cap add android`.

## البناء التلقائي عبر GitHub Actions (الموصى به)

ادفع هذا المستودع لـ GitHub، ثم:

- ادخل تاب **Actions** → اختر workflow **Build Shot Timer APK** → اضغط **Run workflow**.
- بعد 5-8 دقايق سيظهر artifact باسم `shot-timer-debug-apk` في أسفل الـ run.
- نزّل ZIP الـ artifact → فك الضغط → `app-debug.apk` جاهز.

الـ workflow يقوم بـ:
- تنزيل JDK 17 + Android SDK + Gradle.
- `npm install` لجلب `@capacitor/android` (مطلوب لـ `settings.gradle`).
- نسخ `www/` إلى `android/app/src/main/assets/public/`.
- توليد `gradle-wrapper.jar` (الملف الثنائي الوحيد غير المُلتزَم به).
- `./gradlew assembleDebug`.
- رفع الـ APK كـ artifact.

## البناء المحلي

المتطلبات:
- JDK 17
- Android SDK (API 34) — يأتي مع Android Studio
- Gradle 8.2.1 محلياً (لتوليد الـ wrapper jar أول مرة)
- Node.js + npm

الخطوات:

```bash
cd shot-timer-android
npm install

cp -r www/. android/app/src/main/assets/public/

cd android
gradle wrapper --gradle-version 8.2.1
chmod +x gradlew
./gradlew assembleDebug
```

الناتج: `android/app/build/outputs/apk/debug/app-debug.apk`.

## تثبيت الـ APK

### عبر USB (ADB)
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

### يدوياً
1. انقل ملف الـ APK للموبايل (Telegram / Drive / USB).
2. الإعدادات ← الأمان ← **تثبيت تطبيقات غير معروفة** ← فعّلها للبرنامج الذي ستفتح منه الـ APK.
3. افتح الـ APK واضغط تثبيت.
4. عند أول تشغيل، اسمح بإذن الميكروفون.

## ملاحظات

- `applicationId` و `namespace` و package paths كلها `com.shottimer.app` ومتطابقة.
- `MicAudioPlugin.kt` يستخدم `MediaRecorder.AudioSource.UNPROCESSED` (يتجاوز AGC/NS/AEC على مستوى OS) مع fallback لـ `VOICE_RECOGNITION` ثم `MIC`.
- `minSdkVersion` = 26 (Android 8.0+) — مطلوب لدعم adaptive icons بدون PNGs.
- الشاشة تظل صاحية تلقائياً وقت تشغيل المؤقت عبر `FLAG_KEEP_SCREEN_ON` من الـ plugin، وتُطفأ عند `stop()`.
