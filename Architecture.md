\# Architecture \& Data Flow Specification



\## 1. Offline-First Local Architecture

\- \*\*Local Storage:\*\* Room Database (SQLite) running locally on the Samsung Galaxy S22. All CRUD operations execute in < 15ms on the Snapdragon 8 Gen 1 / Exynos 2200 chipset.

\- \*\*Coroutines \& Flow:\*\* Asynchronous data streaming via Kotlin `StateFlow` and `Flow` from Room DAOs to Jetpack Compose UI.

\- \*\*Zero Cloud Reliance:\*\* No third-party BaaS (Firebase/Supabase). Complete data sovereignty and privacy.



\## 2. Strict Financial Isolation

\- \*\*Farming P\&L Boundary:\*\* Personal expenses, personal shopping items, and personal income sources are \*\*strictly segregated\*\* from Farming Profit \& Loss calculations via `accountSection` enums (`FARM`, `FAMILY`, `GENERAL` vs `PERSONAL`).

\- \*\*Profit-Per-Vigha Formula:\*\* 

&#x20; $$\\text{Profit Per Vigha} = \\frac{\\sum(\\text{Crop Sale Income}) - \\sum(\\text{Direct Farm Expenses})}{\\text{Farm Area in Vigha}}$$

&#x20; \*(Protected against divide-by-zero when Vigha is 0).\*



\## 3. GoDaddy Automated Nightly Vault \& Email Sync

\- \*\*Trigger:\*\* `WorkManager` paired with `AlarmManager` exact alarm (with Android 14+ `canScheduleExactAlarms()` check and `setWindow()` fallback).

\- \*\*Execution Time:\*\* Every night at \*\*11:45 PM\*\* (before date change).

\- \*\*Package Contents:\*\* 

&#x20; 1. Room SQLite database checkpointed via `PRAGMA wal\_checkpoint(FULL)`.

&#x20; 2. Compressed WebP bill photos folder (`filesDir/bill\_photos/`).

&#x20; 3. Daily Gujarati PDF Report generated via `PdfDocument` and `StaticLayout`.

\- \*\*Destination:\*\* Uploads via `OkHttp` multipart POST to a secure PHP script (`backup\_receiver.php`) hosted on the user's GoDaddy cPanel hosting, which automatically dispatches an email backup and maintains a 365-day archive.

