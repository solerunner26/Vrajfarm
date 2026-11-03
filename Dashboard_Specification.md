\# Default Home Dashboard Specification



The default app screen upon unlocking (via 4-digit PIN or biometrics) is the \*\*Home Dashboard (`HomeScreen.kt`)\*\*. It serves as the primary command center providing a real-time overview of everything without requiring navigation deep into sub-menus.



\## 1. Screen Hierarchy \& Layout Structure



\### Top Live Banner \& Security Status

\- \*\*Date \& Panchang Strip:\*\* Displays today's English Date, Gujarati Weekday, and the live \*\*Gujarati Panchang Tithi\*\* (calculated offline via the Vikram Samvat Amanta engine).

\- \*\*Status Pills:\*\* GoDaddy Nightly Backup Status indicator (`☁️ બેકઅપ OK`) and the Settings Icon (`⚙️`).



\### Top Alert \& Action Banners (Priority Order)

\- \*\*Pending Udhaar Alert Card:\*\* Highlights unpaid shop bills (દેવાના બાકી) and outstanding market receivables (લેવાના બાકી) with a 1-tap shortcut to settle.

\- \*\*Overdue Asset Lending Warning:\*\* Displays red alert badges if any lented tool/asset has passed its expected return date.

\- \*\*Urgent Reminders Banner:\*\* Highlights high-percent/high-priority task deadlines.



\### 5 Color-Coded Section Cards (2x2 Grid + 1 Full Width)

1\. \*\*🌾 ૮ ખેતર હિસાબ (Farms P\&L):\*\* Summary of total farming income, farm expenses, and net profit/loss (strictly isolated from personal finances).

2\. \*\*👨‍🌾 પરિવાર A અને B (ઉપાડ ખાતું):\*\* Current month cash advance totals for Family A and Family B with 1-tap WhatsApp receipt triggers.

3\. \*\*⚡ સામાન્ય ખેતી ખર્ચ (General Expenses):\*\* Electricity bills, water management, and CCTV installation expenses.

4\. \*\*🏠 અંગત / ઘર હિસાબ (Personal Account):\*\* 100% isolated personal income and personal expenses.

5\. \*\*🛒 ખરીદી અને સ્થાયી મિલકત (Shopping \& Assets):\*\* Active personal and farm shopping list items plus asset inventory status.



\### Recent Activity Log Stream

\- A chronological feed showing recent entries with English dates, Gujarati Tithis, categories, and amounts. Items with attached paper bills display a `📷 પાવતી જુઓ` badge.



\### Bottom Action Dock (Thumb-Friendly on S22)

\- Fixed bottom bar with two oversized buttons:

&#x20; - `- ખર્ચ / ઉપાડ લખો` (Add Expense/Advance)

&#x20; - `+ આવક / વેચાણ લખો` (Add Crop Sale/Income)

