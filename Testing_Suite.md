\# Automated Testing Suite Specification



\### 1. Unit Tests (`src/test/`)

\- `GujaratiPanchangHelperTest`: Validates Vikram Samvat Amanta months, Tithis, and weekdays.

\- `FarmAccountingMathTest`: Validates APMC Mann calculation (`30.5 Mann \* ₹1,600 = ₹48,800`) and verifies Profit-Per-Vigha safely returns `0.0` when Vigha is `0.0` (zero division prevention).



\### 2. Instrumented Room DAO Tests (`src/androidTest/`)

\- `AppDaoTest`: Uses `Room.inMemoryDatabaseBuilder()`.

&#x20; - \*\*Test 1:\*\* Verifies personal expenses and personal shopping items are strictly excluded from Farm P\&L.

&#x20; - \*\*Test 2:\*\* Verifies Asset lending overdue queries and shopping item state changes.

&#x20; - \*\*Test 3:\*\* Verifies Family A \& B advance isolation.



\### 3. Compose UI Tests (`src/androidTest/`)

\- `KhetHisabUiTest`: Validates 4-digit PIN lock screen, 3-tap transaction flow, and Glance widget interactions.

