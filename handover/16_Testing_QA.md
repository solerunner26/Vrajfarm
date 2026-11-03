# 16 · Testing & QA Matrix

**Owning agent:** qa-tester (uses the `android-build-verify` skill and the `mobile-mcp` server) · **Depends on:** all docs

## Purpose
Prove that the money maths, the isolation rules, the Panchang engine and alerts, the security, the lending/WhatsApp flows and the visual design are all correct before the owner relies on the app.

## Test layers
| Layer | Location | Runner |
|---|---|---|
| Unit (pure Kotlin) | `app/src/test/` | `./gradlew testDebugUnitTest` |
| Robolectric (alarms, prefs) | `app/src/test/` | same |
| Room in-memory, workers, intents, Compose UI | `app/src/androidTest/` | `./gradlew connectedDebugAndroidTest` |
| Device walkthrough + screenshots | mobile-mcp on an emulator (Pixel API 35) and the owner's S22 | manual / agent |
| Server | `server/tests/backup_receiver_test.sh` | `bash` + `php -S` |

## Matrix

### Unit tests
| ID | Test | Doc | Expectation |
|---|---|---|---|
| U1 | `FarmAccountingMathTest.apmc` | 06 | 30 મણ + 10 kg × ₹1,600 = ₹48,800; kg = 20 normalises |
| U2 | `FarmScoreTest.zeroVigha` | 07 | per-Vigha = 0, ranked last, shown as "—" |
| U3 | `GujaratiPanchangHelperTest` | 02 | all vectors (03-11-2026 આસો વદ નોમ VS 2082; 08-11 અમાસ; 09-11 VS 2083; 10-11 કારતક સુદ બીજ); weekday map; sunrise ±3 min |
| U4 | `FestivalRulesTest` | 10 | each rule; kshaya; vriddhi; pradosh; adhik |
| U5 | `PanchangAlertEngineTest` | 10 | merge order; disabled → null; custom fixed date |
| U6 | `WhatsAppIntentBuilderTest` | 12 | normaliser cases; encoding; overdue line |
| U7 | `FamilyStatementTemplateTest` | 08 | ≤ 10 lines + "…અને N વધુ"; ₹0 month |
| U8 | `LoanSearchQueryParserTest` | 11 | `23-11-2025`, `23/11/2025`, `3-1-2026` |
| U9 | `ToolKeywordTest` | 11 | પંપ, ટ્રેક્ટર → tool; કરિયાણું → not a tool |
| U10 | `SeasonRangeTest` | 07 | Rabi crosses the year |
| U11 | `PinStoreTest` / `LockManagerTest` | 04 | hash; lockout doubling; 30 s re-lock |
| U12 | `ReminderSchedulerTest` | 13 | exact vs window; HIGH 1 h early |
| U13 | `PanchangAlarmSchedulerTest` | 10 | 19:00 schedule; reschedule on boot |
| U14 | `CreditOnlyInHelpTest` | 14 | `credit_*` used only in HelpScreen.kt |

### Instrumented tests
| ID | Test | Doc | Expectation |
|---|---|---|---|
| I1 | `AppDaoTest.personalExcludedFromFarmPnl` | 03 | personal rows never change farm totals |
| I2 | `AppDaoTest.familyIsolation` | 03 | A ≠ B |
| I3 | `AppDaoTest.pendingUdhaarExcludesPersonal` | 03 | |
| I4 | `AssetLoanDaoTest` | 03/11 | overdue excludes returned; the search finds the 23-11-2025 tractor |
| I5 | `ProfileDaoTest.uniqueRoles` | 03/14 | |
| I6 | `BillPhotoHelperTest` | 06 | ≤ 85 KB, longest side ≤ 1600 |
| I7 | `PersonPhotoCaptureTest` | 11/14 | profile 512² ≤ 60 KB; borrower ≤ 120 KB with timestamp |
| I8 | `GujaratiPdfGeneratorTest` | 15 | multi-page valid PDF |
| I9 | `NightlyBackupWorkerTest` (MockWebServer) | 15 | 200 → status OK; 500 → retry |
| I10 | `OverdueLoanWorkerTest` | 11 | N notifications |
| I11 | `KhetHisabUiTest.pinLock` | 04 | unlock and lockout |
| I12 | `KhetHisabUiTest.threeTapExpense` | 06 | shows on Home recent and changes the totals |
| I13 | `KhetHisabUiTest.festivalDialogOncePerDay` | 10 | dismiss → not shown again |
| I14 | `WhatsAppIntentTest` (espresso-intents) | 12 | ACTION_VIEW `wa.me/91…?text=…` |
| I15 | `MigrationTest` (from v2 onward) | 03 | MigrationTestHelper |

## Device walkthrough (mobile-mcp)
Run on the S22 or emulator with the debug build (`ALLOW_SCREENSHOTS = true`). Take a screenshot at each step and compare it with the canvas artboard of the same name.
1. Fresh install → set PIN 1-2-3-4 → enable fingerprint.
2. Home: date, tithi and backup pill visible. If the date is special, the festival dialog shows once.
3. Expense: ખર્ચ → ખેતર → ખેતર 1 → ખાતર → 5400 → ઉધાર બાકી → સાચવો. The Home udhaar card shows ₹5,400 દેવાના.
4. Income with APMC: 30 મણ, 10 kg, ₹1,600 → ₹48,800 → save with a bill photo → "પાવતી જુઓ" opens the zoom viewer.
5. Personal expense ₹2,000 → the farm card is unchanged.
6. Lend the tractor: photo → name → number → 3 days → સાચવો + મોકલો. WhatsApp opens with the Gujarati text and **nothing is sent automatically**. Return to the app; no re-lock prompt.
7. Change the device date by +4 days → the overdue card and notification appear → "યાદ અપાવો" → "પરત મળ્યું" → the loan appears in history. Search "ટ્રેક્ટર".
8. Shopping: add "દવા છાંટવાનો પંપ" → tick it → the convert sheet appears → હા → it shows in the assets.
9. Reminder HIGH due in 2 min → the notification arrives → DONE from the notification → it is archived.
10. Widgets: add all 3 → tick a shopping item from the widget → the app reflects it.
11. Panchang:
    - set the evening time to now + 2 min → the "આવતીકાલે …" notification arrives on a day before a special tithi (use a fake clock build flag `DEBUG_FAKE_TODAY`)
    - open the calendar → the markers render as different shapes
12. Settings:
    - edit Vigha → the scorecard updates
    - edit the Amas guidance → the dialog shows the new text
    - turn Amas off → no dialog or notification
13. Backup: "હમણાં બેકઅપ લો" against a staging endpoint → OK pill. Wrong key → failure pill.
14. Profiles: set owner and manager photos → the Home avatar updates → the manager WhatsApp opens the chat.
15. Help: the credit is visible here. Grep the screenshots of other screens: it must not appear.

## Non-functional checks
- **Sunlight/contrast**: every text/background pair is ≥ 4.5:1. Run the Accessibility Scanner on each screen. Use max brightness outdoors for a manual check.
- **Touch targets** ≥ 60dp for rows and main actions, 64dp keypad (Accessibility Scanner + layout inspector).
- **Font scale 1.3 and 1.5**: no clipping. The dock wraps.
- **Gujarati rendering**: conjuncts in the UI and PDFs: ક્ષ, જ્ઞ, શ્ર, દ્ધ, ત્ર, સ્વ, દ્ર.
- **Performance**: cold start to Home < 1.5 s on the S22; Home with 5 000 txns renders < 500 ms; Panchang month computation < 150 ms.
- **Low memory**: `adb shell am send-trim-memory com.vraj.khetihisab RUNNING_CRITICAL` → no crash; state is restored.
- **Security**: release build → screenshots blocked; app switcher blurred; no secrets in the APK (`apkanalyzer` / `strings` grep for "key").
- **Visual consistency**: single hue. Screens show no colours outside the roles in doc 01. Spot-check with a palette extractor on the screenshots.

## Panchang verification sheet (mandatory before release)
Produce `qa/panchang_check_VS2082_2083.md`. For every Bij, Agiyaras, Poonam, Amas and listed festival from 01-11-2026 to 31-12-2027, add a table:

| date | engine tithi | printed jantri tithi | match? | nearBoundary |

Any mismatch blocks the release until it is fixed or explained.

## Reporting format
Report each run as:
- the commands run
- pass/fail counts per layer
- failed test names with stack excerpts
- the screenshot list with the artboard comparison notes
- product bugs filed as `qa/bugs/<id>.md` (steps, expected, actual, screenshot)

The QA agent fixes only test code, never product code.

## Acceptance criteria
- [ ] All the unit and instrumented tests above exist and pass.
- [ ] The walkthrough is completed with screenshots.
- [ ] The Panchang verification sheet is complete, with zero unexplained mismatches.
- [ ] The non-functional checks pass.
