\# iOS-Style Glassomorphic \& Liquid Crystal Design System



\## 1. Design Philosophy

\- \*\*Glassmorphism \& Liquid Crystal:\*\* Translucent surfaces with soft blur (`RenderEffect` / translucent gradients), subtle borders (`1px solid rgba(255,255,255,0.18)`), glowing multi-stop gradients, and liquid crystal typography.

\- \*\*Sunlight Readability on S22:\*\* High-contrast pure white/emerald cards against dark slate/glass backgrounds so the screen remains crisp under bright farm sunlight.

\- \*\*Uniformity \& Consistency:\*\* All buttons, cards, dialogs, and navigation headers share identical border radii (`16dp`), padding (`16dp`), elevation shadow styling, and Gujarati font typography (`Noto Sans Gujarati`).



\## 2. Color Palette (Material 3 + Glass Glows)

\- \*\*Primary Background (Liquid Slate):\*\* `#0F172A` (Dark Mode Glass) / `#F1F5F9` (Light Sunlight Mode)

\- \*\*Farm Emerald Green:\*\* `#2E7D32` (Income / Profit)

\- \*\*Earthy Amber Orange:\*\* `#E65100` (Farm Expenses / Shopping)

\- \*\*Royal Crystal Blue:\*\* `#1565C0` (Family Advances)

\- \*\*Deep Velvet Purple:\*\* `#6A1B9A` (General Farm Utilities: Electricity/CCTV)

\- \*\*Aqua Teal:\*\* `#00838F` (Personal Account)

\- \*\*Alert Red:\*\* `#C62828` (Pending Udhaar / Overdue Assets)

\- \*\*Glass Card Surface:\*\* `Color.White.copy(alpha = 0.85)` with `BlurMaskFilter` / border stroke.



\## 3. UI Component Standards

\- \*\*Minimum Touch Targets:\*\* 60dp height for all list items and primary buttons to accommodate farm work conditions.

\- \*\*Typography:\*\* Minimum 18sp body text, 22sp card headers, 26sp bold currency numbers (`₹`).

\- \*\*Keypads:\*\* Custom 64dp numeric keypads for financial entry and APMC Mann calculations.

