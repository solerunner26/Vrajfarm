# 02 · Foundation, Theme & Gujarati Panchang Engine

**Owning agent:** android-foundation · **Canvas:** Brand · **Depends on:** nothing (Wave 1)

## Purpose
Create the Android project skeleton, the two-tone Vraj Green Material 3 theme and shared `Vk*` components (doc 01), the icon and launcher assets, and an offline engine that turns any date into its Gujarati Panchang. The engine output includes Vikram Samvat year, Amanta month, paksha, tithi, weekday, moon phase and festival flags. Docs 05 (Home), 10 (alerts) and 13 (widgets) all consume it.

## User stories
- As the owner, every screen and report shows the English date with the Gujarati tithi, so I can relate entries to the Hindu calendar.
- As a developer, I build every screen from one theme and one component set, so the app looks uniform.

## 1. Project setup
- `minSdk 29`, `targetSdk 35`, `compileSdk 35`, JVM 17, package `com.vraj.khetihisab`, single `app` module.
- `gradle/libs.versions.toml` uses the latest stable versions. Verify them with the context7 MCP before pinning. Known-good baseline:

| Library | Version |
|---|---|
| Kotlin / KSP | 2.0.21 / 2.0.21-1.0.28 |
| AGP | 8.7.3 |
| Compose BOM | 2024.12.01 (material3, ui, ui-tooling, material-icons-extended) |
| activity-compose / navigation-compose / lifecycle | 1.9.3 / 2.8.5 / 2.8.7 |
| Room (runtime, ktx, compiler via KSP) | 2.6.1 |
| WorkManager | 2.10.0 |
| Glance appwidget / material3 | 1.1.1 |
| security-crypto | 1.1.0-alpha06 |
| biometric | 1.1.0 |
| OkHttp | 4.12.0 |
| Coil compose | 2.7.0 |
| Test | junit 4.13.2, androidx.test.ext 1.2.1, espresso 3.6.1, compose ui-test-junit4, room-testing, work-testing, kotlinx-coroutines-test |

- Haze and glass blur are **not used**. The design is now flat Material 3 (doc 01).
- `buildConfigField("boolean","ALLOW_SCREENSHOTS", "true")` in debug and `"false"` in release (doc 04).
- Manifest permissions: `CAMERA`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_BIOMETRIC`, `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `READ_CONTACTS` (optional, for the contact picker).
- `FileProvider` authority `${applicationId}.files`. Paths XML exposes `bill_photos/`, `people_photos/`, `borrower_photos/`, `reminder_photos/`, `reports/`, `tmp/`.

## 2. Theme (`ui/theme/`)
- `Color.kt`: every role from doc 01 §1 as `lightColorScheme(...)` plus the derived `darkColorScheme(...)`. Also `val VkBrandGradient = Brush.linearGradient(listOf(Color(0xFF2E7D5B), Color(0xFF174A37)))`.
- `Type.kt`: `FontFamily(Font(R.font.noto_sans_gujarati_regular, W400), …_medium W500, …_semibold W600, …_bold W700, …_extrabold W800)`. Map the styles exactly as in doc 01 §3. `VkWordmark = FontFamily(Font(R.font.baloo_bhai2_extrabold, W800))`. Both fonts are OFL. Put the TTFs in `res/font`.
- `Shape.kt`: small 12dp (chips), medium 16dp (cards), large 28dp (dialogs, sheets). Buttons use `CircleShape`.
- `Theme.kt`: `VrajTheme(darkTheme = isSystemInDarkTheme())`. **No dynamic colour**: `dynamicColor = false` is mandatory, otherwise Samsung wallpaper colours would break the single-hue design.

## 3. Components (`ui/components/`) — build all of these here
Use the signatures below. Everything else composes them.
```kotlin
@Composable fun VkTopHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}, hero: Boolean = false, content: @Composable ColumnScope.() -> Unit = {})
// hero=true → 170dp brand-gradient band with VkFieldMotif + VkToranEdge at bottom
@Composable fun VkCard(modifier: Modifier = Modifier, tone: VkTone = VkTone.Neutral, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)
enum class VkTone { Neutral, Primary /*secondaryContainer tint*/, Panchang /*tertiaryContainer*/, Error /*errorContainer*/ }
@Composable fun VkSectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) // leaf divider before text
@Composable fun VkPrimaryButton(text: String, onClick: () -> Unit, icon: Int? = null, modifier: Modifier = Modifier) // 60dp, gradient pill
@Composable fun VkTonalButton(text: String, onClick: () -> Unit, icon: Int? = null, modifier: Modifier = Modifier)
@Composable fun VkOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun <T> VkSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) // M3 SingleChoiceSegmentedButtonRow, 56dp
@Composable fun VkChip(text: String, selected: Boolean, onClick: () -> Unit) // FilterChip, 48dp
@Composable fun VkListRow(title: String, meta: String? = null, leadingIcon: Int? = null, trailing: @Composable () -> Unit = {}, onClick: (() -> Unit)? = null) // ≥60dp
@Composable fun VkAmount(paise: Long, kind: AmountKind, style: TextStyle = MaterialTheme.typography.titleMedium) // + primary / − onSurface / pending error
@Composable fun VkStatusBadge(status: VkStatus) // Ok, DueSoon(days), Overdue(days)
@Composable fun VkNumericKeypad(onKey: (Char) -> Unit, onBackspace: () -> Unit, allowDecimal: Boolean = true) // 64dp keys, 3x4
@Composable fun VkPhotoSlot(path: String?, placeholderText: String, onCapture: () -> Unit, size: Dp = 96.dp, shape: Shape = CircleShape)
@Composable fun VkPanchangCard(day: PanchangDay, onClick: () -> Unit) // tertiaryContainer, VkMoonPhase glyph
@Composable fun VkFestivalDialog(alert: PanchangAlert, onDismiss: () -> Unit, onCreateReminder: () -> Unit, onOpenCalendar: () -> Unit) // doc 10
@Composable fun VkBottomDock(onExpense: () -> Unit, onIncome: () -> Unit) // tonal "− ખર્ચ / ઉપાડ" + gradient "+ આવક / વેચાણ"
```
Decor (`ui/decor/`, all `ImageVector`, white at 10–14% or gold at 25%): `VkFieldMotif` (3 curved furrows + sun arc), `VkToranEdge` (scallop path, 12 arcs across the width), `VkLeafDivider`, `VkMoonPhase(illumination: Float, waxing: Boolean)`, `VkRangoliDots`.

## 4. Icons & launcher
- Import the 22 icons from `design/icons/*.svg` with Android Studio ▸ New ▸ Vector Asset. Name them `res/drawable/ic_vk_<name>.xml`. Gradients need `aapt:attr` `<gradient>`, which works on minSdk 29.
- Render with `Image(painterResource(id), contentDescription = null)`. **Never** use `Icon()`, because it tints.
- Adaptive launcher icon `mipmap-anydpi-v26/ic_launcher.xml`:
  - background: brand gradient vector
  - foreground: the sprout + ledger lines from `logo-mark.svg`, scaled into the 66dp safe zone
  - monochrome: the same paths in a single fill
- `app_name` = `વ્રજ ખેતી હિસાબ`.

## 5. Panchang engine (`panchang/`)

### 5.1 Public API
```kotlin
data class PanchangDay(
    val date: LocalDate, val weekdayGu: String,             // મંગળવાર
    val vikramSamvat: Int,                                   // 2082
    val monthIndex: Int, val isAdhik: Boolean, val monthGu: String, // 0=ચૈત્ર … 11=ફાગણ; "અધિક શ્રાવણ"
    val paksha: Paksha, val tithiInPaksha: Int,              // SUD/VAD, 1..15
    val tithiIndex: Int,                                     // 1..30 (1–15 sud, 16–30 vad; 30 = amas)
    val tithiGu: String,                                     // "આસો વદ નોમ"
    val tithiEndsAt: ZonedDateTime,                          // IST
    val sunrise: ZonedDateTime, val sunset: ZonedDateTime,
    val moonIllumination: Float, val waxing: Boolean,
    val nearBoundary: Boolean                                // tithi changes within ±10 min of sunrise → QA flag
)
enum class Paksha { SUD, VAD }
object GujaratiPanchangHelper {
    fun day(date: LocalDate): PanchangDay
    fun month(year: Int, month: Int): List<PanchangDay>      // calendar grid (doc 10)
    fun shortLabel(date: LocalDate): String                  // "આસો વદ નોમ" for list rows
}
```
Location constants: Ahmedabad φ = 23.0225°N, λ = 72.5714°E, zone `Asia/Kolkata`. Cache results in an LRU (366 entries). All calculation is pure Kotlin `Double`, with no network.

### 5.2 Astronomy (`Astro.kt`)
- **Julian Day** from `Instant`: `jd = epochMillis / 86_400_000.0 + 2440587.5`. Then `T = (jd − 2451545.0) / 36525`.
- **Sun apparent longitude** (Meeus ch. 25, low precision):
  - `L0 = 280.46646 + 36000.76983T + 0.0003032T²`
  - `M = 357.52911 + 35999.05029T − 0.0001537T²`
  - `C = (1.914602 − 0.004817T − 0.000014T²)·sin M + (0.019993 − 0.000101T)·sin 2M + 0.000289·sin 3M`
  - `Ω = 125.04 − 1934.136T`
  - `λsun = L0 + C − 0.00569 − 0.00478·sin Ω`
- **Moon longitude** (Meeus ch. 47, the largest 24 terms):
  - Fundamental arguments:
    - `L′ = 218.3164477 + 481267.88123421T`
    - `D = 297.8501921 + 445267.1114034T`
    - `M = 357.5291092 + 35999.0502909T`
    - `M′ = 134.9633964 + 477198.8675055T`
    - `F = 93.2720950 + 483202.0175233T`
    - `E = 1 − 0.002516T`
  - `Σl` (units of 1e-6°) = 6288774 sin M′ + 1274027 sin(2D−M′) + 658314 sin 2D + 213618 sin 2M′ − 185116E sin M − 114332 sin 2F + 58793 sin(2D−2M′) + 57066E sin(2D−M−M′) + 53322 sin(2D+M′) + 45758E sin(2D−M) − 40923E sin(M−M′) − 34720 sin D − 30383E sin(M+M′) + 15327 sin(2D−2F) − 12528 sin(M′+2F) + 10980 sin(M′−2F) + 10675 sin(4D−M′) + 10034 sin 3M′ + 8548 sin(4D−2M′) − 7888E sin(2D+M−M′) − 6766E sin(2D+M) − 5163 sin(D−M′) + 4987E sin(D+M) + 4036E sin(2D−M+M′)
  - `λmoon = L′ + Σl/1e6 − 0.00478·sin Ω`

  This is accurate to about 0.01–0.02°, which puts tithi boundaries within about ±2 minutes.
- **Elongation**: `e(t) = norm360(λmoon − λsun)`. Ayanamsa cancels here, so tithi needs no sidereal correction.
- **Tithi at instant**: `tithiIndex = floor(e / 12) + 1` (1..30).
- **Instant when elongation reaches a target** (tithi end, new moon). Newton iteration with the mean synodic rate 12.1907°/day:
  ```kotlin
  fun solveElongation(target: Double, guess: Instant): Instant { var t = guess
    repeat(8) { val d = wrap180(target - e(t)); t = t.plusMillis((d / 12.1907 * 86_400_000).toLong()); if (abs(d) < 1e-5) return t }
    return t }
  ```
  For the tithi end, `target = tithiIndex * 12.0`.
- **Sunrise / sunset** (sunrise equation, h0 = −0.833°). Inputs: `n = ceil(jd(dateNoonUTC) − 2451545.0 + 0.0008)` and `lw = −72.5714`.
  1. `J* = n − lw/360`
  2. `M = norm360(357.5291 + 0.98560028·J*)`
  3. `C = 1.9148 sin M + 0.02 sin 2M + 0.0003 sin 3M`
  4. `λ = norm360(M + C + 180 + 102.9372)`
  5. `Jtransit = 2451545 + J* + 0.0053 sin M − 0.0069 sin 2λ`
  6. `sin δ = sin λ · sin 23.4397°`
  7. `cos ω0 = (sin(−0.833°) − sin φ sin δ) / (cos φ cos δ)`
  8. `Jrise = Jtransit − ω0/360` and `Jset = Jtransit + ω0/360`

  Convert to IST.
- **Sidereal sun** (for month naming): `λsid = norm360(λsun − ayanamsa)`, with Lahiri `ayanamsa = 23.853 + 0.013969·(yearFraction − 2000)`.
- **Moon illumination**: `k = (1 − cos e) / 2`. `waxing = e < 180`.

### 5.3 Calendar rules (`PanchangRules.kt`)
1. **Day's tithi** = the tithi at **local sunrise** of that civil date (Udaya tithi).
   - A tithi that starts and ends between two sunrises is **kshaya**. It has no day of its own; doc 10 handles festivals on such tithis.
   - A tithi present at two consecutive sunrises is **vriddhi**. Both days show the same tithi.
2. **Paksha**: tithi 1–15 → `SUD` (શુદ/સુદ), tithiInPaksha = index. Tithi 16–30 → `VAD`, tithiInPaksha = index − 15. Tithi 15 is પૂનમ and tithi 30 is અમાસ.
3. **Amanta month** (Gujarati months end on amas):
   1. Find the new moon **before** the date's sunrise: `nm0 = solveElongation(0.0, sunrise − e(sunrise)/12.19 days)`.
   2. Find the next new moon: `nm1 = solveElongation(360.0 ≡ 0, nm0 + 29.53 d)`.
   3. Compute `r0 = floor(λsid(nm0)/30)` and `r1 = floor(λsid(nm1)/30)`.
   4. If `r1 == r0`, no sankranti falls in this lunation, so it is an **Adhik maas**. Name it after the next month, with `isAdhik = true` and the prefix "અધિક ".
   5. Otherwise `monthIndex = (r0 + 1) % 12`. Mesha entered → ચૈત્ર (0), Vrishabha → વૈશાખ (1), …, Tula → આસો (6), Vrischika → કારતક (7).
   6. If `r1 == r0 + 2` (a kshaya maas, very rare), still use `(r0+1)` and log a warning.
4. **Vikram Samvat** (Gujarati, Kartik-based): `vs = year + 56`. Add 1 if the date is on or after **કારતક સુદ 1** of that Gregorian year. Equivalent: monthIndex ≥ 7 (Kartik…), or monthIndex in 7..11 when the date is in Oct–Dec. Implement it as "find this year's Kartik sud 1 date once and compare". Example: 03-11-2026 → 2082; 09-11-2026 → 2083.
5. **Weekday**: `date.dayOfWeek` mapped to Gujarati.

### 5.4 Name tables (`PanchangNames.kt`)
- Months: ચૈત્ર, વૈશાખ, જેઠ, અષાઢ, શ્રાવણ, ભાદરવો, આસો, કારતક, માગશર, પોષ, મહા, ફાગણ
- Tithi in paksha 1..15: એકમ, બીજ, ત્રીજ, ચોથ, પાંચમ, છઠ, સાતમ, આઠમ, નોમ, દશમ, અગિયારસ, બારસ, તેરસ, ચૌદશ, પૂનમ. In vad, 15 is **અમાસ**.
- Paksha: સુદ, વદ
- Weekdays (Mon→Sun): સોમવાર, મંગળવાર, બુધવાર, ગુરુવાર, શુક્રવાર, શનિવાર, રવિવાર
- English month names in Gujarati: જાન્યુઆરી, ફેબ્રુઆરી, માર્ચ, એપ્રિલ, મે, જૂન, જુલાઈ, ઓગસ્ટ, સપ્ટેમ્બર, ઓક્ટોબર, નવેમ્બર, ડિસેમ્બર
- `tithiGu = "$monthGu ${paksha.gu} ${tithiName}"` → "આસો વદ નોમ"

### 5.5 Display formats
- Home banner: `03 નવેમ્બર 2026 | મંગળવાર | આસો વદ નોમ`
- List rows: `02-11-2026 · આસો વદ આઠમ`
- Header chip: `વિ.સં. 2082`

## Edge cases
- Midnight vs sunrise: an entry made at 02:00 belongs to the civil date, but its tithi is the previous sunrise's tithi. Always pass the civil `LocalDate`.
- DST does not apply (IST is fixed). The device timezone may differ, so always compute in `Asia/Kolkata`.
- Dates are supported from 1990 to 2100. Outside that range, throw `IllegalArgumentException`.
- `nearBoundary = true` when |tithiStart or tithiEnd − sunrise| < 10 min. QA uses it to verify against a printed panchang.

## Test vectors (`GujaratiPanchangHelperTest`)

> ⚠️ These vectors are the expected values from the design brief and the known 2026 festival dates. **They must be cross-checked against a published Gujarati panchang (e.g., Sandesh/Gujarat Samachar or a Mahuva/Bhavnagar jantri) before release.** Any mismatch at a `nearBoundary` day must be resolved with the printed panchang.

| Date | Expected |
|---|---|
| 03-11-2026 | મંગળવાર, આસો વદ નોમ, VS 2082 |
| 05-11-2026 | આસો વદ અગિયારસ (Rama Ekadashi) |
| 08-11-2026 | આસો વદ અમાસ (Diwali), VS 2082 |
| 09-11-2026 | કારતક સુદ એકમ, VS 2083 (Bestu Varas) |
| 10-11-2026 | કારતક સુદ બીજ (Bhai Bij) |

Also test:
- Weekday mapping for 7 consecutive days.
- The `tithiIndex` → paksha/tithiInPaksha mapping for 1, 2, 11, 15, 16, 26, 30.
- Moon illumination: ≈1.0 on a poonam and ≈0.0 on an amas.
- Sunrise at Ahmedabad on 21-06-2026 is around 05:55–06:00 IST, and on 21-12-2026 around 07:15–07:20 IST (±3 min).
- Adhik detection: build a synthetic lunation with `r0 == r1`.

## Gujarati strings
`app_name`=વ્રજ ખેતી હિસાબ · `vs_prefix`=વિ.સં. · `paksha_sud`=સુદ · `paksha_vad`=વદ · `adhik`=અધિક

## Acceptance criteria
- [ ] Project builds with the catalog. `VrajTheme` is applied in `MainActivity`, and dynamic colour is off.
- [ ] All `Vk*` components exist, with previews, in light and dark.
- [ ] 22 `ic_vk_*` vectors and the adaptive launcher icon are in place, and the icons are never tinted.
- [ ] `GujaratiPanchangHelper.day()` runs in < 5 ms when cached and < 40 ms when cold on an S22.
- [ ] All test vectors pass, or are documented as corrected after the printed-panchang check.
