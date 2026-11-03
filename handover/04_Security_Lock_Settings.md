# 04 · Security, App Lock & Master Settings

**Owning agent:** android-foundation · **Canvas:** 01 PIN Lock, 15 Settings · **Depends on:** 02, 03

## Purpose
Keep the owner's private ledger safe. A 4-digit PIN with fingerprint fallback protects the app, it re-locks when the app goes to the background, and screenshots are blocked. Master Settings is the one place to edit farms, Vigha, family numbers, crops, categories, Panchang alert preferences (doc 10) and backup (doc 15).

## User stories
- On first launch I set a 4-digit PIN and can enable fingerprint.
- When I come back to the app after more than 30 seconds, it asks for my PIN or fingerprint.
- No one can take a screenshot or see the app content in the recent-apps preview.
- I can edit the Vigha area of each farm, the family WhatsApp numbers, crops and categories without a developer.

## Screens & components
- **LockScreen** (artboard 01):
  - logo (96dp)
  - wordmark (Baloo Bhai 2)
  - 4 dot indicators (primary fill when entered)
  - `VkNumericKeypad(allowDecimal = false)` with 76dp round keys
  - `VkTonalButton("ફિંગરપ્રિન્ટથી ખોલો", icon = ic_vk_lock)`

  The background is `surface` with the `VkFieldMotif` at 6% in primary.
- **SetPinScreen**: same layout. Two steps: enter, then confirm.
- **SettingsScreen** (artboard 15). `VkTopHeader("સેટિંગ્સ")` followed by these groups of `VkListRow`:
  1. પ્રોફાઇલ (doc 14)
  2. ખેતર, વીઘા અને પાક
  3. પરિવાર WhatsApp નંબર
  4. ખર્ચના પ્રકાર
  5. પંચાંગ ચેતવણી (doc 10)
  6. PIN + ફિંગરપ્રિન્ટ (Switch)
  7. બેકઅપ (doc 15)
  8. PDF રિપોર્ટ (doc 15)
  9. મદદ અને માહિતી (doc 14)
- Sub-screens:
  - `FarmsSettingsScreen`: list of 8 farms. Edit dialog with name, Vigha (decimal keypad), manager and active crop cycle (crop + season + year).
  - `FamilySettingsScreen`
  - `CropsSettingsScreen`
  - `CategoriesSettingsScreen`: list per section, add/rename/deactivate. Never hard-delete a category that is in use.

## Data
- `security/PinStore`: `EncryptedSharedPreferences` (MasterKey AES256_GCM).
  - `pin_hash`: PBKDF2WithHmacSHA256, 120 000 iterations, 16-byte random salt
  - `pin_salt`
  - `biometric_enabled: Boolean`
  - `failed_attempts: Int`
  - `lock_until: Long`
- Master data uses `MasterDao` (doc 03).

## Business logic
1. **FLAG_SECURE**. In `MainActivity.onCreate`:
   ```kotlin
   if (!BuildConfig.ALLOW_SCREENSHOTS) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
   ```
   Also set `setRecentsScreenshotEnabled(false)` on API 33+.
2. **Auto-lock** (`security/LockManager`, a `DefaultLifecycleObserver` on `ProcessLifecycleOwner.get().lifecycle`):
   - `onStop` records `backgroundedAt = SystemClock.elapsedRealtime()`.
   - `onStart`: if `elapsed − backgroundedAt > 30_000` or the app is cold-starting, set `locked = true`.
   - `LockManager.locked: StateFlow<Boolean>`. The root `NavHost` shows `LockScreen` above everything while it is true. Nothing else composes underneath, so there is no flash of content.
   - The camera, voice and WhatsApp intents launched from the app set `LockManager.suppressNextLock = true`, so returning from them does not re-prompt.
3. **PIN verify**:
   - Hash the input and compare with `MessageDigest.isEqual`.
   - After 5 wrong attempts, lock out for 30 s. The lockout doubles on each further 5 failures, up to a maximum of 15 minutes.
   - Show the remaining time as "ફરી પ્રયાસ કરો: 0:28".
4. **Biometric**:
   - `BiometricManager.from(ctx).canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS` → show the button and auto-prompt when the lock screen opens.
   - `BiometricPrompt` (`FragmentActivity`). `MainActivity` extends `FragmentActivity` / `AppCompatActivity`.
   - Prompt info: title "વ્રજ ખેતી હિસાબ ખોલો", negative button "PIN વાપરો".
   - On success, unlock.
5. **Forgot PIN**: there is no remote reset, by design. The option "PIN ભૂલી ગયા?" explains that the PIN can only be reset by clearing app data, then restoring from the nightly backup (doc 15). Show it as a confirm dialog with that explanation.
6. **Farm Vigha edit**: validate `0.0 ≤ vigha ≤ 500`. Show `0` as "વીઘા સેટ કરો" across the app.

## Edge cases
- If the device has no biometric enrolled, hide the button and do not show an error.
- If the device date or time changes, it does not affect the lock. `elapsedRealtime` is immune to that.
- In multi-window or when the screen turns off, `onStop` fires and the lock applies.
- Accessibility: TalkBack announces "4 માંથી 2 અંક દાખલ થયા". The PIN digits are never spoken.

## Gujarati strings
| Key | Text |
|---|---|
| `lock_enter_pin` | 4 અંકનો PIN દાખલ કરો |
| `lock_wrong` | ખોટો PIN |
| `lock_retry_in` | ફરી પ્રયાસ કરો: %s |
| `lock_bio` | ફિંગરપ્રિન્ટથી ખોલો |
| `lock_set_new` | નવો PIN બનાવો |
| `lock_confirm` | PIN ફરી દાખલ કરો |
| `lock_mismatch` | PIN મેળ ખાતો નથી |
| `settings_title` | સેટિંગ્સ |
| `settings_farms` | ખેતર, વીઘા અને પાક |
| `settings_family` | પરિવાર WhatsApp નંબર |
| `settings_categories` | ખર્ચના પ્રકાર |
| `settings_lock` | PIN + ફિંગરપ્રિન્ટ લોક |
| `vigha_not_set` | વીઘા સેટ કરો |

## Acceptance criteria
- [ ] First run forces PIN setup before Home.
- [ ] Release builds block screenshots and recents previews.
- [ ] The app re-locks after 30 s in the background and does not re-lock after camera, voice or WhatsApp return.
- [ ] The lockout schedule works as specified.
- [ ] All Master Settings edits update every screen live through `Flow`.

## Tests
- `PinStoreTest` (Robolectric or instrumented): hash/verify works, the salt is random, the lockout doubles.
- `LockManagerTest`: simulate stop/start with a fake clock.
- `KhetHisabUiTest.pinLock`: enter 1-2-3-4 → Home visible. Enter a wrong PIN 5 times → lockout text shown.
