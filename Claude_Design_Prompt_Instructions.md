\# Dedicated Claude Design Prompt File

\## Feed this file directly into Claude Design or AI Visual Assistants for Pixel-Perfect UI/UX Generation



\### UI/UX Design Directives for KhetHisab (Samsung Galaxy S22)

You are an expert Android UI/UX Designer specializing in Jetpack Compose, Material 3, and iOS-style Glassmorphism / Liquid Crystal design systems. Your task is to design and generate the visual layout code for the Gujarati Farm \& Personal Accounting App ("ખેત અને ઘર હિસાબ").



\#### 1. Core Visual Style \& Theme

\- \*\*Glassmorphic / Liquid Crystal Aesthetics:\*\* Translucent frosted glass surfaces (`Color.White.copy(alpha = 0.85)` or dark glass `#1E293B.copy(alpha = 0.9)`), subtle borders (`1dp solid rgba(255,255,255,0.2)`), soft multi-stop radial gradients, and crisp liquid crystal typography.

\- \*\*Sunlight Readability:\*\* Optimized for outdoor farm usage on a Samsung Galaxy S22 (6.1" screen). High-contrast dark text on bright pastel/emerald cards, or pure white text on deep slate glass.

\- \*\*Uniformity \& Consistency:\*\* All cards, dialogs, and navigation headers must share identical border radii (`16dp`), padding (`16dp`), and elevation shadows.



\#### 2. Screen Specifications to Design

1\. \*\*Default Home Dashboard (`HomeScreen.kt`):\*\*

&#x20;  - Top Bar: Live Gujarati Panchang date banner (`03 નવેમ્બર 2026 | મંગળવાર | આસો વદ નોમ`), GoDaddy Backup status pill, and Settings icon.

&#x20;  - Alert Banners: Red Pending Udhaar card, Overdue Asset Lending warning, and Urgent Reminders banner.

&#x20;  - 5 Section Cards (2x2 grid + 1 full width): ૮ ખેતર હિસાબ (Farm P\&L), પરિવાર A/B ઉપાડ, સામાન્ય ખેતી ખર્ચ, અંગત / ઘર હિસાબ, and ખરીદી તથા મિલકત.

&#x20;  - Recent Log Stream: Chronological entries with `📷 પાવતી જુઓ` badges.

&#x20;  - Bottom Action Bar: Two oversized thumb-friendly action buttons (`- ખર્ચ / ઉપાડ લખો` in amber, `+ આવક / વેચાણ લખો` in emerald green).

2\. \*\*3-Tap Transaction \& APMC Mann Calculator Screen:\*\*

&#x20;  - Giant numeric keypad (64dp buttons).

&#x20;  - Mann + Extra Kg + Rate per Mann inputs with live total calculation.

&#x20;  - Paid vs. Pending Udhaar toggle.

3\. \*\*Shopping List \& Asset Lending Screen:\*\*

&#x20;  - Personal vs Farm tabs.

&#x20;  - Checkbox action items and physical tool conversion dialog.

&#x20;  - Lending tracker with borrower details and overdue red badges.

4\. \*\*Reminder \& Glance Widget Layouts:\*\*

&#x20;  - Priority tags, deadline timestamps, and "DONE" archive action.



\#### 3. Typography \& Accessibility

\- Font Family: `NotoSansGujarati` (bundled in assets).

\- Font Sizes: Minimum 18sp body text, 22sp card headers, 26sp bold currency numbers (`₹`).

\- Minimum touch target size: 60dp for all actionable elements.

