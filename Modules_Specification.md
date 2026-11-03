\# Complete Functional Modules Specification



\## Module 1: Project Setup, Panchang Engine \& Theme

\- Kotlin 2.x, Jetpack Compose Material 3, KSP Room, WorkManager, Glance.

\- `GujaratiPanchangHelper`: Offline Vikram Samvat Amanta lunar calculations based on Ahmedabad coordinates (23.0225° N, 72.5714° E).



\## Module 2: Room Database Schema \& DAOs

\- 9 Core Entities: `FarmEntity`, `FamilyEntity`, `CropMasterEntity`, `CategoryMasterEntity`, `FarmCropCycleEntity`, `TransactionEntity`, `ShoppingItemEntity`, `AssetEntity`, `ReminderEntity`.

\- Foreign key cascading, Kotlin Flow queries for P\&L, Personal isolation, pending shopping, and overdue assets.



\## Module 3: Home Dashboard \& 3-Tap Entry Flow

\- Live Gujarati Panchang top banner, GoDaddy backup status pill, Pending Udhaar alert card, Overdue asset warning banner.

\- 5 Section Cards, two bottom action buttons (`- ખર્ચ / ઉપાડ`, `+ આવક / વેચાણ`).

\- `BillPhotoHelper`: Auto-compression to WebP (<85 KB) and pinch-to-zoom viewer. Gujarati Voice-to-Text (`RecognizerIntent`).



\## Module 4: Shopping List \& Asset Management

\- Two Categories: Personal vs Farm.

\- Date-wise entry, "Bought" checkbox (removes from active list, triggers weekly reminder WorkManager).

\- \*\*Physical Asset Converter:\*\* When a purchased shopping item is a physical tool, prompts to convert into an `AssetEntity`.

\- \*\*Asset Lending \& Borrowing:\*\* Tracks Asset ID, borrower name, handing over date, expected return date, and triggers overdue notifications if not returned on time.



\## Module 5: Reminder \& Push Notification Module

\- Text and Image reminders with priority (`HIGH`/`NORMAL`) and deadlines.

\- System push notifications in pure Gujarati when deadlines approach.

\- "DONE" action moves tasks to Archive.



\## Module 6: 8 Farms, APMC Mann Calculator \& Profit-Per-Vigha

\- Farm land size in Vigha (`areaInVigha`), active crop badge.

\- APMC Mann Calculator: `TotalMann = Mann + (ExtraKg / 20.0)`, `TotalAmount = TotalMann \* RatePerMann`.

\- Scorecard ranking farms by Net Profit per Vigha.



\## Module 7: Family A \& B Advance Tracker \& WhatsApp Receipts

\- Monthly/yearly cash advance summary (`ઉપાડ`).

\- 1-Tap \*\*"📲 WhatsApp પાવતી મોકલો"\*\* launching WhatsApp directly with pre-formatted Gujarati statement.



\## Module 8: Isolated Personal Account \& PDF Report Engine (7 Reports)

\- Standalone Personal account screen (Income \& Expenses separated from farm P\&L).

\- `GujaratiPdfGenerator`: Uses `StaticLayout` with `NotoSansGujarati` to prevent broken conjuncts (જોડાક્ષર). Generates 7 A4 Gujarati PDF reports.



\## Module 9: Home Screen App Widgets (Jetpack Glance)

\- `ShoppingListGlanceWidget`: Displays pending shopping items with checkboxes on Galaxy S22 home screen.

\- `ReminderGlanceWidget`: Displays high-priority reminders with quick "DONE" action.



\## Module 10: 11:45 PM GoDaddy Nightly Backup \& Sync

\- `backup\_receiver.php` script for GoDaddy cPanel hosting with secret API key verification and email dispatch.

\- Android `WorkManager` + AlarmManager exact alarm with `setWindow()` fallback for Android 14+.



\## Module 11: 4-Digit PIN Lock \& Master Settings

\- `FLAG\_SECURE` window flag, EncryptedSharedPreferences, `ProcessLifecycleOwner` auto-lock, and S22 Biometric fingerprint fallback.

\- `MasterSettingsScreen`: Dynamic editing of farms, Vigha, family WhatsApp numbers, crops, and categories.

