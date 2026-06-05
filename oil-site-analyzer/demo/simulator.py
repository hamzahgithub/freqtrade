#!/usr/bin/env python3
"""
محاكاة محلل المواقع النفطية
Oil Site Analyzer — Engineering Logic Demo (no API key needed)
"""

import json
import math
from dataclasses import dataclass, field
from enum import Enum
from typing import Optional
from tabulate import tabulate

# ─── ANSI Colors ───────────────────────────────────────────────────────────────
class C:
    RESET  = "\033[0m"
    BOLD   = "\033[1m"
    GREEN  = "\033[92m"
    YELLOW = "\033[93m"
    RED    = "\033[91m"
    BLUE   = "\033[94m"
    CYAN   = "\033[96m"
    GREY   = "\033[90m"
    WHITE  = "\033[97m"
    BG_RED    = "\033[41m"
    BG_YELLOW = "\033[43m"
    BG_GREEN  = "\033[42m"
    BG_BLUE   = "\033[44m"

# ─── Data Models ───────────────────────────────────────────────────────────────
class ReadingStatus(Enum):
    NORMAL    = ("طبيعي",      C.GREEN,  "✓")
    WARNING   = ("تحذير",      C.YELLOW, "⚠")
    CRITICAL  = ("حرج",        C.RED,    "⛔")
    UNDERHEAT = ("برودة زائدة", C.BLUE,  "❄")
    UNKNOWN   = ("غير معروف",  C.GREY,   "?")

    def label(self):   return self.value[0]
    def color(self):   return self.value[1]
    def icon(self):    return self.value[2]

@dataclass
class ValidationResult:
    parameter_ar: str
    parameter_en: str
    value: float
    unit: str
    status: ReadingStatus
    message_ar: str
    recommendation_ar: Optional[str] = None

@dataclass
class OilReading:
    type: str          # temperature | pressure | flow | level
    value: float
    unit: str
    label: str
    location: str      # inlet | outlet | bath | general

@dataclass
class AnalysisReport:
    equipment_type: str
    equipment_details: str
    readings: list
    validation_results: list
    notes: str = ""


# ─── Engineering Thresholds ────────────────────────────────────────────────────
class HeaterThresholds:
    BATH_UNDERHEAT  = 55.0
    BATH_NORMAL_MAX = 88.0
    BATH_WARNING    = 95.0
    BATH_CRITICAL   = 99.0

    DT_EXCELLENT = 20.0
    DT_GOOD      = 10.0
    DT_FAIR      = 5.0

class TankThresholds:
    LL = 10.0; L  = 20.0
    H  = 85.0; HH = 92.0

class PressureThresholds:
    LOW_WARNING  = 2.0
    NORMAL_MAX   = 50.0


# ─── Validators ────────────────────────────────────────────────────────────────
def validate_bath_temp(t: float) -> ValidationResult:
    th = HeaterThresholds
    if t < th.BATH_UNDERHEAT:
        return ValidationResult(
            "حرارة الباث", "Bath Temperature", t, "°C",
            ReadingStatus.UNDERHEAT,
            f"حرارة الباث {t:.1f}°C أقل من الحد الأدنى ({th.BATH_UNDERHEAT}°C) — الهيتر لا يعمل بكفاءة",
            "تحقق من إشعال الموقد ومصدر الوقود والثيرموستات"
        )
    elif t <= th.BATH_NORMAL_MAX:
        return ValidationResult(
            "حرارة الباث", "Bath Temperature", t, "°C",
            ReadingStatus.NORMAL,
            f"حرارة الباث {t:.1f}°C ضمن نطاق التشغيل الآمن ({th.BATH_UNDERHEAT}–{th.BATH_NORMAL_MAX}°C)"
        )
    elif t <= th.BATH_WARNING:
        return ValidationResult(
            "حرارة الباث", "Bath Temperature", t, "°C",
            ReadingStatus.WARNING,
            f"حرارة الباث {t:.1f}°C تقترب من الحد الأعلى",
            f"قلل قدرة الموقد، راقب كل 10 دقائق"
        )
    else:
        return ValidationResult(
            "حرارة الباث", "Bath Temperature", t, "°C",
            ReadingStatus.CRITICAL,
            f"حرج: حرارة الباث {t:.1f}°C — خطر الغليان (نقطة غليان الماء 100°C)",
            "⚡ أوقف الموقد فوراً — لا تفتح صمام الباث حتى تنخفض الحرارة تحت 80°C"
        )

def validate_delta_t(inlet: float, outlet: float) -> ValidationResult:
    dt = outlet - inlet
    th = HeaterThresholds
    if dt < 0:
        return ValidationResult(
            "فرق الحرارة (ΔT)", "Heat Transfer ΔT", dt, "°C",
            ReadingStatus.CRITICAL,
            f"ΔT = {dt:.1f}°C — حرارة الخروج أقل من الدخول",
            "تحقق فوراً من توصيلات المستشعرات واتجاه التدفق"
        )
    elif dt < th.DT_FAIR:
        return ValidationResult(
            "فرق الحرارة (ΔT)", "Heat Transfer ΔT", dt, "°C",
            ReadingStatus.WARNING,
            f"نقل حرارة ضعيف: ΔT = {dt:.1f}°C (المطلوب ≥ {th.DT_FAIR}°C)",
            "أسباب محتملة: ترسبات على الأنابيب، تدفق عالٍ، حرارة باث منخفضة"
        )
    elif dt < th.DT_GOOD:
        return ValidationResult(
            "فرق الحرارة (ΔT)", "Heat Transfer ΔT", dt, "°C",
            ReadingStatus.WARNING,
            f"نقل حرارة متوسط: ΔT = {dt:.1f}°C",
            "راقب الاتجاه، جدول فحص الأنابيب إذا استمر الانخفاض"
        )
    elif dt < th.DT_EXCELLENT:
        return ValidationResult(
            "فرق الحرارة (ΔT)", "Heat Transfer ΔT", dt, "°C",
            ReadingStatus.NORMAL,
            f"نقل حرارة جيد: ΔT = {dt:.1f}°C"
        )
    else:
        return ValidationResult(
            "فرق الحرارة (ΔT)", "Heat Transfer ΔT", dt, "°C",
            ReadingStatus.NORMAL,
            f"نقل حرارة ممتاز: ΔT = {dt:.1f}°C ✓"
        )

def validate_inlet_temp(t: float) -> ValidationResult:
    if t < 15.0:
        return ValidationResult(
            "حرارة الدخول", "Inlet Temperature", t, "°C",
            ReadingStatus.WARNING,
            f"حرارة الدخول {t:.1f}°C — خطر تكون هيدرات أقل من 15°C",
            "تأكد من تشغيل الهيتر وحقن مانع الهيدرات"
        )
    elif t > 90.0:
        return ValidationResult(
            "حرارة الدخول", "Inlet Temperature", t, "°C",
            ReadingStatus.WARNING,
            f"حرارة الدخول مرتفعة: {t:.1f}°C",
            "تحقق من مصدر السائل الداخل"
        )
    else:
        return ValidationResult(
            "حرارة الدخول", "Inlet Temperature", t, "°C",
            ReadingStatus.NORMAL,
            f"حرارة الدخول طبيعية: {t:.1f}°C"
        )

def validate_outlet_temp(t: float) -> ValidationResult:
    if t < 15.0:
        return ValidationResult(
            "حرارة الخروج", "Outlet Temperature", t, "°C",
            ReadingStatus.CRITICAL,
            f"حرارة الخروج {t:.1f}°C منخفضة جداً — الهيتر لا يؤدي وظيفته",
            "تحقق من: اللهب، الوقود، مستوى الباث، سلامة الأنابيب"
        )
    elif t > 90.0:
        return ValidationResult(
            "حرارة الخروج", "Outlet Temperature", t, "°C",
            ReadingStatus.WARNING,
            f"حرارة الخروج مرتفعة: {t:.1f}°C",
            "خفض حرارة الباث أو زيادة معدل التدفق"
        )
    else:
        return ValidationResult(
            "حرارة الخروج", "Outlet Temperature", t, "°C",
            ReadingStatus.NORMAL,
            f"حرارة الخروج طبيعية: {t:.1f}°C"
        )

def validate_pressure(p: float, unit: str = "barg") -> ValidationResult:
    th = PressureThresholds
    if p < 0:
        return ValidationResult(
            "الضغط", "Pressure", p, unit,
            ReadingStatus.CRITICAL,
            f"ضغط سالب {p:.1f} {unit} — تفريغ هواء أو خلل في المستشعر",
            "تحقق من معايرة المستشعر"
        )
    elif p < th.LOW_WARNING:
        return ValidationResult(
            "الضغط", "Pressure", p, unit,
            ReadingStatus.WARNING,
            f"ضغط منخفض: {p:.1f} {unit} — خطر تسرب الغاز",
            "تحقق من صمام الخنق ومصدر الغاز"
        )
    elif p <= th.NORMAL_MAX:
        return ValidationResult(
            "الضغط", "Pressure", p, unit,
            ReadingStatus.NORMAL,
            f"الضغط طبيعي: {p:.1f} {unit}"
        )
    else:
        return ValidationResult(
            "الضغط", "Pressure", p, unit,
            ReadingStatus.WARNING,
            f"ضغط مرتفع: {p:.1f} {unit} — تحقق من الضغط الأقصى للمعدة (MAWP)",
            "راقب صمام الأمان"
        )

def validate_tank_level(level: float) -> ValidationResult:
    th = TankThresholds
    if level < th.LL:
        return ValidationResult(
            "مستوى الخزان", "Tank Level", level, "%",
            ReadingStatus.CRITICAL,
            f"مستوى LL منخفض جداً: {level:.1f}% — خطر تجويف المضخة",
            "أوقف مضخة النقل فوراً — تحقق من مصدر الإمداد"
        )
    elif level < th.L:
        return ValidationResult(
            "مستوى الخزان", "Tank Level", level, "%",
            ReadingStatus.WARNING,
            f"مستوى منخفض: {level:.1f}%",
            "قلل معدل الضخ أو زيادة الإمداد"
        )
    elif level <= th.H:
        return ValidationResult(
            "مستوى الخزان", "Tank Level", level, "%",
            ReadingStatus.NORMAL,
            f"مستوى الخزان طبيعي: {level:.1f}%"
        )
    elif level <= th.HH:
        return ValidationResult(
            "مستوى الخزان", "Tank Level", level, "%",
            ReadingStatus.WARNING,
            f"مستوى مرتفع: {level:.1f}%",
            "زيادة معدل التصدير"
        )
    else:
        return ValidationResult(
            "مستوى الخزان", "Tank Level", level, "%",
            ReadingStatus.CRITICAL,
            f"مستوى HH مرتفع جداً: {level:.1f}% — خطر الفيضان",
            "أوقف جميع مصادر الإمداد فوراً"
        )


# ─── Display ───────────────────────────────────────────────────────────────────
def print_banner(text: str, color: str = C.CYAN):
    w = 64
    print(f"\n{color}{C.BOLD}{'━' * w}")
    print(f"  {text}")
    print(f"{'━' * w}{C.RESET}")

def print_result(r: ValidationResult):
    s = r.status
    color = s.color()
    print(f"\n  {color}{C.BOLD}{s.icon()} {r.parameter_ar}  ({r.parameter_en}){C.RESET}")
    print(f"  {C.WHITE}{C.BOLD}  القيمة: {r.value:.1f} {r.unit}  ← [{color}{s.label()}{C.RESET}]{C.RESET}")
    print(f"  {color}  {r.message_ar}{C.RESET}")
    if r.recommendation_ar:
        print(f"  {C.YELLOW}  💡 {r.recommendation_ar}{C.RESET}")

def print_overall_status(results):
    statuses = [r.status for r in results]
    n = statuses.count(ReadingStatus.NORMAL)
    w = statuses.count(ReadingStatus.WARNING)
    cr = statuses.count(ReadingStatus.CRITICAL)
    uh = statuses.count(ReadingStatus.UNDERHEAT)

    print(f"\n  {C.GREEN}✓ طبيعي: {n}{C.RESET}   "
          f"{C.YELLOW}⚠ تحذير: {w}{C.RESET}   "
          f"{C.RED}⛔ حرج: {cr}{C.RESET}   "
          f"{C.BLUE}❄ برودة: {uh}{C.RESET}")

    if cr > 0:
        print(f"\n  {C.BG_RED}{C.WHITE}{C.BOLD}  ⛔  حالة حرجة — تدخل فوري مطلوب  {C.RESET}")
    elif w > 0 or uh > 0:
        print(f"\n  {C.BG_YELLOW}  ⚠  تحذيرات نشطة — مراجعة مطلوبة  {C.RESET}")
    else:
        print(f"\n  {C.BG_GREEN}{C.WHITE}  ✓  جميع القراءات ضمن الحدود الطبيعية  {C.RESET}")


# ─── Test Scenarios ─────────────────────────────────────────────────────────────
SCENARIOS = [
    {
        "name": "سيناريو 1: هيتر يعمل بشكل طبيعي",
        "name_en": "Scenario 1 — Normal Heater Operation",
        "equipment": "هيتر غير مباشر (Indirect Fired Heater)",
        "details": "هيتر خط النفط الخام — محطة الضخ رقم 3",
        "notes": "صورة واضحة — معدل تدفق 850 m³/d",
        "readings": [
            OilReading("temperature", 72.5, "°C", "BATH TEMP", "bath"),
            OilReading("temperature", 18.2, "°C", "INLET",     "inlet"),
            OilReading("temperature", 55.8, "°C", "OUTLET",    "outlet"),
            OilReading("pressure",    12.4, "barg", "WO PRESS", "general"),
        ]
    },
    {
        "name": "سيناريو 2: هيتر — حرارة باث مرتفعة (تحذير)",
        "name_en": "Scenario 2 — High Bath Temperature Warning",
        "equipment": "هيتر خط الغاز (Line Heater)",
        "details": "محطة فصل الغاز — خط إمداد المدينة",
        "notes": "الحرارة ترتفع تدريجياً منذ 2 ساعة",
        "readings": [
            OilReading("temperature", 91.3, "°C", "BATH TEMP", "bath"),
            OilReading("temperature",  8.5, "°C", "INLET",     "inlet"),
            OilReading("temperature", 62.1, "°C", "OUTLET",    "outlet"),
            OilReading("pressure",    18.7, "barg", "LINE PRESS", "general"),
        ]
    },
    {
        "name": "سيناريو 3: هيتر — وضع حرج (خطر غليان)",
        "name_en": "Scenario 3 — CRITICAL: Bath Near Boiling",
        "equipment": "هيتر بحوض ماء",
        "details": "خط إمداد الهيدروكربونات — GOSP-5",
        "notes": "⚠ عطل في الثيرموستات — الموقد لم يُطفأ تلقائياً",
        "readings": [
            OilReading("temperature", 97.8, "°C", "BATH TEMP", "bath"),
            OilReading("temperature", 14.2, "°C", "INLET",     "inlet"),
            OilReading("temperature", 68.4, "°C", "OUTLET",    "outlet"),
            OilReading("pressure",    22.1, "barg", "PRESS",   "general"),
        ]
    },
    {
        "name": "سيناريو 4: هيتر بارد — برودة زائدة",
        "name_en": "Scenario 4 — Underheat (Heater Not Firing)",
        "equipment": "هيتر غير مباشر",
        "details": "محطة حقل أم الشيف — بعد إيقاف الصيانة",
        "notes": "الموقد أُعيد تشغيله للتو بعد صيانة",
        "readings": [
            OilReading("temperature", 38.5, "°C", "BATH",   "bath"),
            OilReading("temperature",  9.1, "°C", "INLET",  "inlet"),
            OilReading("temperature", 11.3, "°C", "OUTLET", "outlet"),
            OilReading("pressure",     5.6, "barg", "PRESS", "general"),
        ]
    },
    {
        "name": "سيناريو 5: خزان + فاصل (Separator + Tank)",
        "name_en": "Scenario 5 — Separator & Tank Readings",
        "equipment": "فاصل ثلاثي المراحل + خزان تخزين",
        "details": "GOSP — فاصل تجميع النفط والغاز والماء",
        "notes": "إنتاج عالٍ — اليوم الثالث من الموجة الحرارية",
        "readings": [
            OilReading("pressure",  8.4, "barg", "SEP PRESS",  "general"),
            OilReading("level",    88.2, "%",    "OIL LEVEL",  "general"),
            OilReading("level",     7.5, "%",    "WATER LEVEL","general"),
            OilReading("temperature", 45.0, "°C","SEP TEMP",   "inlet"),
            OilReading("pressure",  0.8, "barg", "LP SEP",     "general"),
        ]
    },
]

def analyze_scenario(scenario: dict) -> AnalysisReport:
    """Apply engineering validation to a scenario."""
    readings = scenario["readings"]
    results = []

    bath_r   = next((r for r in readings if r.location == "bath"   and r.type == "temperature"), None)
    inlet_r  = next((r for r in readings if r.location == "inlet"  and r.type == "temperature"), None)
    outlet_r = next((r for r in readings if r.location == "outlet" and r.type == "temperature"), None)

    if bath_r:
        results.append(validate_bath_temp(bath_r.value))
    if inlet_r:
        results.append(validate_inlet_temp(inlet_r.value))
    if outlet_r:
        results.append(validate_outlet_temp(outlet_r.value))
    if inlet_r and outlet_r:
        results.append(validate_delta_t(inlet_r.value, outlet_r.value))

    for r in readings:
        if r.type == "pressure":
            results.append(validate_pressure(r.value, r.unit))
        elif r.type == "level":
            results.append(validate_tank_level(r.value))

    return AnalysisReport(
        equipment_type=scenario["equipment"],
        equipment_details=scenario["details"],
        readings=readings,
        validation_results=results,
        notes=scenario["notes"]
    )

def run_demo():
    print(f"\n{C.CYAN}{C.BOLD}")
    print("  ╔══════════════════════════════════════════════════════════╗")
    print("  ║         محلل المواقع النفطية — OIL SITE ANALYZER        ║")
    print("  ║       تحليل هندسي بالذكاء الاصطناعي — محاكاة كاملة     ║")
    print("  ╚══════════════════════════════════════════════════════════╝")
    print(C.RESET)

    for i, scenario in enumerate(SCENARIOS, 1):
        print_banner(f"{scenario['name']}  ▸  {scenario['name_en']}", C.CYAN)

        report = analyze_scenario(scenario)

        print(f"\n  {C.WHITE}{C.BOLD}المعدة:{C.RESET}  {report.equipment_type}")
        print(f"  {C.WHITE}{C.BOLD}الموقع:{C.RESET}  {report.equipment_details}")
        print(f"  {C.GREY}ملاحظة: {report.notes}{C.RESET}")

        # Raw readings table
        table_data = []
        for r in report.readings:
            loc_ar = {"bath":"باث","inlet":"دخول","outlet":"خروج","general":"عام"}.get(r.location, r.location)
            table_data.append([r.label, r.type, f"{r.value:.1f} {r.unit}", loc_ar])

        print(f"\n  {C.WHITE}{C.BOLD}القراءات المُستخرجة من الصورة:{C.RESET}")
        print("  " + tabulate(
            table_data,
            headers=["التسمية", "النوع", "القيمة", "الموقع"],
            tablefmt="simple",
            colalign=("left","left","right","left")
        ).replace("\n", "\n  "))

        # Engineering results
        print(f"\n  {C.WHITE}{C.BOLD}النتائج الهندسية:{C.RESET}")
        for result in report.validation_results:
            print_result(result)

        print_overall_status(report.validation_results)

        if i < len(SCENARIOS):
            print(f"\n  {C.GREY}{'─' * 60}{C.RESET}")

    print(f"\n\n  {C.GREEN}{C.BOLD}✓ انتهت المحاكاة — 5 سيناريوهات هندسية{C.RESET}\n")


if __name__ == "__main__":
    run_demo()
