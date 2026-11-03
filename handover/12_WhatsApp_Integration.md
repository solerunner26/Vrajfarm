# 12 · WhatsApp Integration (shared `WhatsAppLauncher`)

**Owning agent:** assets-shopping. Build this **first in Wave 2**, because docs 05, 08, 11 and 14 call it. **Canvas:** 05 Family, 10 Lend Asset, 14 Profiles

## Purpose
One helper opens WhatsApp on the right chat with a ready Gujarati message. It is used for loan reminders, family advance statements and messages to managers. There is no WhatsApp Business API and no server, and the app **never sends automatically**: the user always taps Send inside WhatsApp.

## User stories
- I tap "યાદ અપાવો" on an overdue loan, and WhatsApp opens on Rameshbhai's chat with the reminder typed.
- If WhatsApp is missing, the Android share sheet opens with the same text.

## API (`share/WhatsAppLauncher.kt`)
```kotlin
class WhatsAppLauncher(private val context: Context, private val profiles: ProfileRepo, private val loans: AssetLoanRepo) {
  fun open(phone: String?, text: String): LaunchResult
  suspend fun loanReminder(loan: LoanWithAsset): LaunchResult      // builds template, opens, then markReminded()
  suspend fun familyStatement(familyId: Long, month: YearMonth): LaunchResult
  fun toManager(profile: PersonProfileEntity, text: String = ""): LaunchResult
}
sealed interface LaunchResult { object Opened : LaunchResult; object SharedFallback : LaunchResult; object NoNumber : LaunchResult; object NoApp : LaunchResult }
object PhoneNormalizer { fun toE164NoPlus(raw: String, defaultCc: String = "91"): String? }
object Templates { fun loanReminder(...): String; fun familyStatement(...): String }
```
These are plain classes created in `AppContainer`. The UI calls them from the ViewModel through an event, so that `startActivity` runs with an Activity context.

## Business logic
1. **Normalise the phone**:
   1. Strip everything except digits.
   2. Remove a leading `00`.
   3. If the number is 10 digits starting with 6–9, prepend `91`.
   4. If it is 11 digits starting with `0`, drop the 0 and prepend `91`.
   5. If it is 12 digits starting with `91`, keep it.
   6. Otherwise return null and treat it as `NoNumber`.
2. **Build the intent**:
   ```kotlin
   val uri = Uri.parse("https://wa.me/$num?text=" + Uri.encode(text))
   val pkgs = listOf("com.whatsapp", "com.whatsapp.w4b")
   val pm = context.packageManager
   val pkg = pkgs.firstOrNull { runCatching { pm.getPackageInfo(it, 0) }.isSuccess }
   val intent = Intent(Intent.ACTION_VIEW, uri).apply { pkg?.let(::setPackage); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
   ```
   `AndroidManifest` needs `<queries><package android:name="com.whatsapp"/><package android:name="com.whatsapp.w4b"/></queries>` for package visibility (API 30+).
3. **Fallbacks**:
   - If there is no WhatsApp package, use `Intent.createChooser(Intent(ACTION_SEND).setType("text/plain").putExtra(EXTRA_TEXT, text), "મોકલો")` → `SharedFallback`.
   - If the number is null, open the share sheet the same way and show a toast "WhatsApp નંબર નથી".
4. Before `startActivity`, set `LockManager.suppressNextLock = true` (doc 04).
5. **After opening a loan reminder**, call `AssetDao.markReminded(loanId, now)`. This records that the reminder was opened, not that it was sent; WhatsApp gives no delivery callback. The UI label is therefore "છેલ્લે યાદ અપાવ્યું".

## Templates. The text is editable in Settings › "WhatsApp મેસેજ નમૂના" and stored in `SharedPreferences`; the placeholders are fixed.
**Loan reminder** (default):
```
નમસ્તે {borrower},
તમે તા. {handedOver} ના રોજ અમારું {asset} લઈ ગયા છો.
પરત આપવાની તારીખ: {expectedReturn}
કૃપા કરી સમયસર પરત કરશો.
— {ownerName}, {village}
```
If the loan is overdue, append the line `({n} દિવસ મોડું થયું છે)` before the signature.

**Family statement**: see doc 08.

**Manager message**: empty text, which opens the chat only.

`ownerName` and `village` come from the OWNER profile. The village is the first comma part of the address with the "મુ. પો." prefix stripped, giving "થોરાળા".

## Edge cases
- The text contains `&`, `?`, `#` or newlines: `Uri.encode` handles them. Test with Gujarati conjuncts.
- The `wa.me` link opens in a browser if WhatsApp is disabled. Detect the disabled state via `ApplicationInfo.enabled` and use the share fallback.
- Dual-SIM or multiple WhatsApp accounts: let the system handle it.

## Gujarati strings
| Key | Text |
|---|---|
| `wa_no_number` | WhatsApp નંબર નથી |
| `wa_share` | મોકલો |
| `wa_templates` | WhatsApp મેસેજ નમૂના |
| `wa_late_line` | (%d દિવસ મોડું થયું છે) |

## Acceptance criteria
- [ ] All callers use `WhatsAppLauncher`. Nothing builds a `wa.me` URL elsewhere.
- [ ] The correct chat opens with the exact preview text.
- [ ] Nothing is ever sent without the user pressing Send in WhatsApp.
- [ ] The fallbacks work when WhatsApp is not installed or the number is missing.

## Tests
- `WhatsAppIntentBuilderTest`:
  - normaliser cases: `9876543210` → `919876543210`; `+91 98765 43210` → same; `09876543210` → same; `0091…` → same; `12345` → null
  - the Gujarati text round-trips through `Uri.encode` / `decode`
  - the overdue line is appended
- Instrumented test with `Intents.intending` (espresso-intents): asserts the ACTION_VIEW data URI and the package.
