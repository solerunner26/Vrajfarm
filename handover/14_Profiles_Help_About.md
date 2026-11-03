# 14 · Owner & Farm Manager Profiles, Help & About

**Owning agent:** compose-screens · **Canvas:** 14 Profiles, 16 Help & About · **Depends on:** 03, 12

## Purpose
- **Profiles**: the owner's own photo and details, plus the two farm managers' photos, phones and assigned farms. These personalise the app (the Home avatar and greeting, the WhatsApp signatures) and give one-tap contact with the managers.
- **Help & About**: the app identity, a Gujarati FAQ, the owner block, and the **only** place for the credit "Design and Developed by Jaldip Vala".

## User stories
- I set my photo, and it shows on Home.
- I add Manager 1 and Manager 2 with photos and numbers, and assign farms 1–4 and 5–8.
- I can call or WhatsApp a manager from their card.
- I open Help to learn how the Mann calculation works, who made the app, and whose app it is.

## ProfilesScreen (artboard 14)
1. `VkTopHeader("પ્રોફાઇલ", onBack)`.
2. **Owner card** (`VkCard`, centred):
   - `VkPhotoSlot(size = 124.dp, CircleShape)` with a camera badge. It shows the photo, or the initial "ગિ" on a primaryContainer circle. Tapping it opens the photo sheet.
   - the role chip "માલિક" (primaryContainer)
   - the name "ગિરવાનસિંહ વાળા" (24sp/800)
   - the address "મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર"
   - the phone
   - `VkOutlinedButton("પ્રોફાઇલ સુધારો")`
3. `VkSectionTitle("ફાર્મ મેનેજર")`.
4. Two **manager cards**, each with:
   - `VkPhotoSlot(84.dp)`
   - the role "ફાર્મ મેનેજર 1"
   - the name, or the placeholder "નામ ઉમેરો"
   - the phone
   - "જવાબદારી:" chips listing the farms whose `managerRole` matches
   - a 3-button row: `VkPrimaryButton("WhatsApp")` (→ `WhatsAppLauncher.toManager`), `VkTonalButton("ફોન કરો")` (`ACTION_DIAL tel:`) and `VkOutlinedButton("સુધારો")`
5. **Photo sheet** (`ModalBottomSheet`):
   - "કેમેરાથી ફોટો પાડો": `TakePicture`
   - "ગેલેરીમાંથી પસંદ કરો": `PickVisualMedia(ImageOnly)`
   - "ફોટો કાઢો", shown if a photo exists
   - "રદ કરો"
6. **Edit dialog / screen**: name, phone and address (owner only). Managers also get farm assignment checkboxes for ખેતર 1–8; assigning a farm to one manager unassigns it from the other.

### Photo processing (`media/PersonPhotoCapture.processProfile`)
1. Decode with `ImageDecoder`, applying the EXIF rotation.
2. Centre-crop to a square and scale to **512×512**.
3. Save as WebP quality 80, ≤ 60 KB, at `filesDir/people_photos/<role>.webp`. The file is overwritten each time.
4. Bump `updatedAt`. Coil uses it as a cache key: `ImageRequest.Builder.memoryCacheKey("$path@$updatedAt")`.

## HelpScreen (artboard 16)
1. `VkTopHeader("મદદ અને માહિતી", onBack)`.
2. **App card**:
   - the logo (96dp)
   - "વ્રજ ખેતી હિસાબ" (Baloo Bhai 2, 30sp)
   - "VRAJ KHETI HISAB" (letter-spaced, onSurfaceVariant)
   - "આવૃત્તિ {versionName} · ઓફલાઇન · ખાનગી"
3. `VkSectionTitle("વારંવાર પુછાતા પ્રશ્નો")` and an accordion. Each item is a `VkCard` with an expandable `VkListRow` (icon, question, +/−), `animateContentSize()`, and `semantics { stateDescription = "ખુલ્લું" / "બંધ" }`.

| # | Question | Answer |
|---|---|---|
| 1 | એન્ટ્રી કેવી રીતે લખવી? | હોમ પર નીચેનું બટન દબાવો. વિભાગ, ખેતર અને રકમ — માત્ર 3 ટેપમાં એન્ટ્રી થઈ જાય. |
| 2 | મણ ગણતરી કેવી રીતે થાય? | કુલ મણ = મણ + (વધારાના કિલો ÷ 20). કુલ રકમ = કુલ મણ × મણનો ભાવ. |
| 3 | મિલકત ઉછીની આપતી વખતે ફોટો કેમ? | લઈ જનારનો ફોટો તારીખ સાથે સચવાય છે, એટલે પછી શોધી શકો કે કોણ, ક્યારે, શું લઈ ગયું. |
| 4 | WhatsApp યાદી કેવી રીતે મોકલવી? | મિલકત કાર્ડ પર "યાદ અપાવો" દબાવો. તૈયાર ગુજરાતી મેસેજ સાથે WhatsApp ખુલશે — ફક્ત મોકલો દબાવો. |
| 5 | અમાસ, પૂનમ, અગિયારસ, બીજની સૂચના કેમ આવે છે? | પંચાંગ પ્રમાણે ખાસ દિવસોએ આગલી સાંજે અને તે દિવસે સૂચના આવે છે. સલાહ માન્યતા પ્રમાણે છે અને સેટિંગ્સમાં બદલી શકાય છે. |
| 6 | મારો ડેટા સુરક્ષિત છે? | બધો ડેટા ફોનમાં જ રહે છે. દરરોજ રાત્રે 11:45 વાગ્યે તમારા પોતાના સર્વર પર બેકઅપ જાય છે. |
| 7 | PIN ભૂલી જાઉં તો? | એપનો ડેટા સાફ કરી, બેકઅપમાંથી પાછો લાવવો પડે. PIN કોઈ સાથે શેર ન કરો. |

4. **Owner block** (`VkCard`):
   - "માલિક"
   - "ગિરવાનસિંહ વાળા"
   - "મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર"
   - the English line "Owner: Girwansinh Vala · At Post Thorala, Taluka Mahuva, Dist. Bhavnagar"

   The values are read from the OWNER profile, so edits reflect here. The English line is a static string resource.
5. **Credit block** (`VkCard(tone = Primary)`, centred, the last item):
   - "Design and Developed by" (bodySmall)
   - **"Jaldip Vala"** (titleLarge/800)
   - "ડિઝાઇન અને ડેવલપમેન્ટ: જલદીપ વાળા"

   **Do not show this credit anywhere else.** That includes the splash, the settings and the PDF footers.
6. Optional "ઓપન-સોર્સ લાઇસન્સ" row → a simple list of OFL fonts and Apache libraries. This is a static screen.

## Data
- `ProfileDao.all()` and `upsert()`.
- Farm assignment via `MasterDao.updateFarmManager(farmId, role)`.

## Edge cases
- If the manager name is blank, the WhatsApp and Call buttons are disabled with the hint "પહેલાં નંબર ઉમેરો".
- Photo processing fails (corrupt image): show a snackbar "ફોટો ખોલી શકાયો નહીં" and keep the old photo.
- Removing the owner photo falls back to the initial avatar on Home.

## Gujarati strings
| Key | Text |
|---|---|
| `profile_title` | પ્રોફાઇલ |
| `role_owner` | માલિક |
| `role_manager` | ફાર્મ મેનેજર %d |
| `edit_profile` | પ્રોફાઇલ સુધારો |
| `managers` | ફાર્મ મેનેજર |
| `responsibility` | જવાબદારી: |
| `call` | ફોન કરો |
| `edit` | સુધારો |
| `add_name` | નામ ઉમેરો |
| `photo_camera` | કેમેરાથી ફોટો પાડો |
| `photo_gallery` | ગેલેરીમાંથી પસંદ કરો |
| `photo_remove` | ફોટો કાઢો |
| `cancel` | રદ કરો |
| `help_title` | મદદ અને માહિતી |
| `faq` | વારંવાર પુછાતા પ્રશ્નો |
| `version_line` | આવૃત્તિ %s · ઓફલાઇન · ખાનગી |
| `credit_en` | Design and Developed by |
| `credit_name` | Jaldip Vala |
| `credit_gu` | ડિઝાઇન અને ડેવલપમેન્ટ: જલદીપ વાળા |

## Acceptance criteria
- [ ] Owner and manager photos persist, appear on Home and in the cards, and are included in the backup (doc 15).
- [ ] Farm assignment is exclusive between the two managers.
- [ ] The credit text appears only on the Help screen (grep check in QA).
- [ ] The FAQ text is in Gujarati and works with TalkBack.

## Tests
- `ProfileDaoTest` (doc 03).
- `PersonPhotoCaptureTest`: the output is 512×512 and ≤ 60 KB.
- `CreditOnlyInHelpTest`: a unit test that scans `res/values/strings.xml` usages. The `credit_*` keys are referenced only from `HelpScreen.kt`. Implement it as a simple file-scan test over `src/main/java`.
