# Shot Timer — بناء APK أندرويد

## المتطلبات

- Node.js 18+ و npm
- JDK 17 (Capacitor 6 يتطلب Java 17)
- Android Studio (لتثبيت Android SDK + Build Tools + Platform 34)
- جهاز أندرويد بنظام 7.0+ (API 24+ لدعم `UNPROCESSED` audio source)

## الخطوات

### 1) تثبيت الاعتماديات

```bash
cd shot-timer-android
npm install
```

### 2) تهيئة منصة أندرويد

```bash
npx cap add android
```

سيتم إنشاء مجلد `android/` بالكامل.

### 3) نسخ ملفات الـ Plugin والإعدادات

من مجلد `android-snippets/` انسخ كالتالي:

| المصدر | الوجهة |
|---|---|
| `MicAudioPlugin.kt` | `android/app/src/main/java/com/shottimer/app/MicAudioPlugin.kt` |
| `MainActivity.java` | `android/app/src/main/java/com/shottimer/app/MainActivity.java` (يستبدل القائم) |
| `AndroidManifest.xml` | `android/app/src/main/AndroidManifest.xml` (يستبدل القائم) |

ملاحظة: إذا كان مسار الـ package مختلف بعد `cap add android`، عدّل أول سطر `package` في الملفات .kt و .java ليطابق المسار الفعلي.

### 4) مزامنة الـ web assets مع المشروع الأندرويد

```bash
npx cap sync android
```

كرّر هذا الأمر بعد أي تعديل في `www/`.

### 5) بناء APK تجريبي (Debug)

```bash
cd android
.\gradlew.bat assembleDebug
```

الناتج:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

### 6) (بديل) فتح في Android Studio

```bash
npx cap open android
```

ثم Build → Build Bundle(s) / APK(s) → Build APK(s).

## تثبيت الـ APK على الموبايل

### الطريقة 1 — عبر USB (ADB)

1. فعّل **خيارات المطور** في الموبايل: الإعدادات ← حول الهاتف ← اضغط رقم البناء 7 مرات.
2. الإعدادات ← خيارات المطور ← فعّل **USB debugging**.
3. وصّل الموبايل بكابل USB واسمح بـ "السماح للحاسوب بـ debug" على شاشة الموبايل.
4. شغّل:

```bash
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

### الطريقة 2 — نقل ملف APK يدوياً

1. انسخ `app-debug.apk` للموبايل (USB / Google Drive / Telegram).
2. الإعدادات ← الأمان ← **تثبيت تطبيقات غير معروفة** ← اختر التطبيق الذي ستفتح منه الـ APK (مدير الملفات / المتصفح) ← فعّل **السماح**.
3. افتح ملف الـ APK من مدير الملفات واضغط **تثبيت**.
4. عند أول تشغيل سيطلب التطبيق إذن الميكروفون — اضغط **السماح**.

## تشخيص المشاكل

| المشكلة | الحل |
|---|---|
| `gradlew not found` | تأكد أنك داخل مجلد `android/` |
| فشل البناء بسبب Java | استخدم JDK 17 (`java -version` للتأكد) |
| التطبيق يفتح ثم يغلق | افتح Android Studio Logcat لمعرفة السبب |
| الميكروفون لا يستجيب | تأكد من السماح يدوياً من إعدادات التطبيق |
| الصوت مكتوم/مضغوط | تحقق أن `source` المعروض هو `UNPROCESSED` وليس `MIC` |

## التحقق من مصدر الصوت

في أسفل واجهة التطبيق ستظهر سطر صغير عند بدء الجلسة يوضح المصدر النشط، مثلاً:

```
audio source: UNPROCESSED · 44100 Hz
```

`UNPROCESSED` = أفضل دقة (بدون AGC/NS/AEC على مستوى النظام).
`VOICE_RECOGNITION` = fallback مقبول (AGC/NS معطلين).
`MIC` = آخر fallback (قد يكون فيه AGC مفعّل).
