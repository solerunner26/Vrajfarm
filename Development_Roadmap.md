\# Step-by-Step Development Roadmap for Claude Code



\### Phase 1: Foundation \& Database (Days 1–2)

1\. Initialize Android project with Min SDK 29, Target SDK 35/36, and Gradle dependencies.

2\. Implement `GujaratiPanchangHelper` and test leap years and Amanta lunar boundaries.

3\. Build all 9 Room `@Entity` classes and `AppDao` with pre-seeding callbacks.



\### Phase 2: Design System \& Security (Day 3)

1\. Create Material 3 Glassmorphic theme (`Theme.kt`, `Color.kt`, `Type.kt`).

2\. Implement `FLAG\_SECURE`, `ProcessLifecycleOwner` app lock, and 4-digit PIN screen with biometric fallback.



\### Phase 3: Core Transaction \& Farm Modules (Days 4–5)

1\. Build Home Dashboard and 3-Tap Daily Entry flow.

2\. Implement APMC Mann Calculator, Bill Photo compressor, and Gujarati Voice-to-Text.

3\. Build 8 Farms P\&L and Profit-Per-Vigha scorecard.



\### Phase 4: Shopping, Assets \& Reminders (Days 6–7)

1\. Build Shopping List (Personal vs Farm) and Weekly Notification Worker.

2\. Implement Asset Lending/Borrowing tracker with overdue reminders.

3\. Build Reminder Module, Archive, and Glance App Widgets.



\### Phase 5: PDF Reports \& GoDaddy Nightly Backup (Days 8–9)

1\. Implement `GujaratiPdfGenerator` with `StaticLayout` for 7 PDF reports.

2\. Deploy `backup\_receiver.php` to GoDaddy cPanel and build Android `NightlyBackupWorker`.



\### Phase 6: Testing \& Optimization (Day 10)

1\. Run Unit tests, Room DAO in-memory tests, and Compose UI tests.

2\. Verify S22 sunlight readability and performance under low memory conditions.

