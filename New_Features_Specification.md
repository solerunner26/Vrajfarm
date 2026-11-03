# New Features Specification — Vraj Kheti Hisab (v1.1 additions)

App name changes from "KhetHisab" to **Vraj Kheti Hisab (વ્રજ ખેતી હિસાબ)**. Everything in the earlier spec files still applies; this file adds 5 features and the brand.

Design reference: the Design canvas "Vraj Kheti Hisab — UI/UX" (artboards 06, 07, 11, 14 and Brand). Icon and logo sources: `design/icons/*.svg`, `design/logo/logo-mark.svg`.

---

## F1. Owner & Farm Manager Profiles (with photos)

- One **Owner** profile + exactly **two Farm Manager** profiles.
- Fields: photo, name, role, mobile (WhatsApp), address (owner), assigned farms (managers).
- Seed values: Owner = **ગિરવાનસિંહ વાળા (Girwansinh Vala)**, મુ. પો. થોરાળા, તા. મહુવા, જિ. ભાવનગર (At Post Thorala, Taluka Mahuva, Dist. Bhavnagar). Manager names and phone numbers are entered by the user.
- Photo from camera (`ActivityResultContracts.TakePicture` + `FileProvider`) or gallery (`PickVisualMedia`), center-cropped to 512×512, saved as WebP (< 60 KB) under `filesDir/people_photos/`.
- Owner avatar shows on the Home header and opens the Profiles screen. Manager cards have WhatsApp / Call / Edit actions.

**New entity** `PersonProfileEntity(id, role: OWNER|MANAGER_1|MANAGER_2, name, phone, address, photoPath, assignedFarmIds: String /*CSV*/, updatedAt)`.

## F2. Borrower photo when an asset is lent

- The "Lend asset" flow (`LendAssetScreen`) has 4 steps on one screen: **1 photo of the borrower → 2 name + WhatsApp number (or pick contact) → 3 which asset → 4 handover date (with Tithi) + expected return (1 / 3 / 7 / 15 days or custom)**.
- The photo is required for new loans (you can skip it, but a warning chip stays on the loan card). It is stamped with date and time and stored as WebP under `filesDir/borrower_photos/`.
- Every loan is kept forever in **history**, so the search box ("કોણ લઈ ગયું? નામ, તારીખ, મિલકત") answers questions like *"who took the tractor on 23-11-2025?"*. It matches the borrower name, asset name and date (dd-MM-yyyy).
- "પરત મળ્યું ✓" closes the loan (records `returnedAt`), and the loan moves to history.

**New entity** `AssetLoanEntity(id, assetId FK→AssetEntity CASCADE, borrowerName, borrowerPhone, borrowerPhotoPath, handedOverAt, handedOverTithi, expectedReturnAt, returnedAt?, notifyOnDue: Boolean, lastWhatsAppReminderAt?)`.
`AssetEntity` keeps only inventory fields plus a derived `isLent`. DAO queries: `activeLoans()`, `overdueLoans(now)`, `searchLoanHistory(query)`, `loansForAsset(assetId)`.

Total entity count goes from 9 to **11**.

## F3. WhatsApp reminder straight from the app

- Buttons: Home overdue banner, loan card "યાદ અપાવો", Lend screen "સાચવો + મોકલો", Family "WhatsApp પાવતી મોકલો", Manager card "WhatsApp".
- Implementation (no WhatsApp API, no server): `Intent(ACTION_VIEW, Uri.parse("https://wa.me/91XXXXXXXXXX?text=" + Uri.encode(msg)))`. Try `com.whatsapp` first, then `com.whatsapp.w4b`. If neither is installed, use `ACTION_SEND` with plain text (share sheet).
- Normalise phone numbers to E.164 without "+" (default country code 91).
- Gujarati reminder template (editable in Settings):

```
નમસ્તે {borrower},
તમે તા. {handedOver} ના રોજ અમારું {asset} લઈ ગયા છો.
પરત આપવાની તારીખ: {expectedReturn}
કૃપા કરી સમયસર પરત કરશો.
— {ownerName}, {village}
```

- Save `lastWhatsAppReminderAt` on the loan, and show "છેલ્લે યાદ અપાવ્યું: …" on the card.
- The app opens WhatsApp with the message ready, and the user taps Send. The app never sends anything without that tap.

## F4. Two-tone gradient icon system

- 22 icons on a 96-unit grid. Each icon is a rounded tile (rx 28) with a light tinted gradient, plus **two overlapping glyph shapes, each with its own gradient** (primary hue + an accent hue). The style follows the idea of the new Google app icons, but with this app's own colours.
- Files: `design/icons/{farm,family,utility,personal,shopping,asset,lend,reminder,income,expense,message,camera,bill,calculator,backup,settings,help,profile,panchang,lock,mic,report}.svg`.
- Android: import each with Android Studio ▸ *New ▸ Vector Asset* (gradients are supported, `minSdk 29`). Name them `ic_vk_<name>.xml`. Render with `Image(painterResource(...))`, not `Icon()`, so the gradients are not tinted.
- Standard sizes: 40dp on cards, 32dp inside buttons, 16–20dp in badges.

## F5. Logo, app name and credits

- Logo mark: `design/logo/logo-mark.svg`. A two-leaf sprout (a lime-green crop leaf and a saffron harvest leaf) rising over two ledger lines, on an emerald-to-teal squircle.
- Adaptive launcher icon: foreground = sprout + ledger lines (inside the 66dp safe zone), background = emerald gradient. Monochrome layer for themed icons.
- `app_name` = "વ્રજ ખેતી હિસાબ". The Latin wordmark "VRAJ KHETI HISAB" appears under the Gujarati one.
- Wordmark font: **Baloo Bhai 2** (OFL, used for the wordmark only). UI font stays **Noto Sans Gujarati**.
- **Help & About** screen shows the logo, version, FAQ, the owner block, and the credit "Design and Developed by Jaldip Vala". The credit appears **only** on this screen.

---

## Backup impact

The nightly backup ZIP now also contains `people_photos/` and `borrower_photos/`. The PDF report "મિલકત અને ઉછીનું" embeds the borrower thumbnails.

## Tests to add

- `WhatsAppIntentBuilderTest`: phone normalisation (`98XXXXXXXX`, `+91 98…`, `098…`), URL-encodes Gujarati text, falls back to the share sheet.
- `AssetLoanDaoTest`: an overdue query excludes returned loans; history search finds "ટ્રેક્ટર" + "23-11-2025".
- `ProfileDaoTest`: only one OWNER and at most two MANAGER rows.
