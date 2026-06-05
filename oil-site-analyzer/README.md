# محلل المواقع النفطية — Oil Site Analyzer

تطبيق Android لتحليل صور معدات الحقول النفطية باستخدام Claude Vision API مع منطق هندسي حقيقي.

## المميزات

### المعدات المدعومة
| المعدة | القراءات |
|--------|---------|
| **الهيترات (Heaters)** | حرارة الباث، الدخول، الخروج، حساب ΔT |
| **الفواصل (Separators)** | الضغط، الحرارة، المستوى |
| **الخزانات (Tanks)** | مستوى الخزان % |
| **عدادات الضغط** | Bar, barg, psi, kPa |
| **عدادات التدفق** | m³/h, MMSCFD, bbl/d |
| **لوحات التحكم** | جميع القراءات المرئية |

### المنطق الهندسي
```
حرارة الباث (Water Bath Heater):
  ✓ طبيعي:      55 – 88 °C
  ⚠ تحذير:      88 – 95 °C (قلل قدرة الموقد)
  ⛔ حرج:       > 95 °C   (خطر غليان، أوقف فوراً)
  🔵 برودة زائدة: < 55 °C   (تحقق من الموقد)

فرق الحرارة (ΔT = خروج - دخول):
  ✓ ممتاز:  ΔT ≥ 20 °C
  ✓ جيد:    ΔT 10–20 °C
  ⚠ متوسط:  ΔT 5–10 °C
  ⚠ ضعيف:   ΔT < 5 °C (ترسبات أو معدل تدفق عالٍ)
  ⛔ حرج:    ΔT < 0   (خطأ في المستشعر)

مستوى الخزان:
  ✓ طبيعي:  20 – 80 %
  ⚠ منخفض:  10 – 20 %
  ⛔ LL:     < 10 %  (أوقف المضخة)
  ⚠ مرتفع:  80 – 85 %
  ⛔ HH:     > 92 %  (خطر فيضان)
```

## البناء

### المتطلبات
- Android Studio Hedgehog أو أحدث
- Android SDK 34
- JDK 17

### الخطوات
```bash
git clone <repo>
cd oil-site-analyzer
./gradlew assembleDebug
```

### مفتاح API
1. افتح التطبيق → الإعدادات
2. أدخل مفتاح Claude API من `console.anthropic.com`
3. يُخزَّن مشفراً باستخدام EncryptedSharedPreferences

## البنية التقنية

```
app/src/main/java/com/oilsite/analyzer/
├── data/
│   ├── api/              ← Retrofit + Claude API
│   ├── model/            ← OilReading, ValidationResult, AnalysisReport
│   └── repository/       ← AnalysisRepository (API key storage)
├── domain/
│   ├── engineering/      ← المنطق الهندسي (HeaterValidator, PressureValidator...)
│   └── usecase/          ← AnalyzeImageUseCase (ترتيب عملية التحليل)
└── ui/
    ├── home/             ← الشاشة الرئيسية
    ├── camera/           ← CameraX
    ├── analysis/         ← عرض النتائج
    └── settings/         ← إدارة مفتاح API
```

## سير العمل

```
صورة → Claude Vision API
         ↓
      JSON منظم (قراءات + نوع المعدة)
         ↓
   محدد نوع المعدة (heater / separator / tank)
         ↓
   مُحقق هندسي مخصص (HeaterValidator, PressureValidator...)
         ↓
   ValidationResult (حالة + رسالة + توصية) بالعربي
         ↓
   واجهة ملونة (أخضر/أصفر/أحمر/أزرق)
```
