# Generic farm ledger showcase

A static, responsive, multilingual product showcase for presenting the Android app's documented features. The public-facing demo uses generic branding and fictional sample records; it does not include owner, family, farm, or borrower names from the app/repository.

## Included

- English, Gujarati and Hindi translations for the landing page, feature cards, screen selector and all screen previews.
- 33 interactive screen previews: PIN setup and lock; dashboard; entry; farm list/detail; manager advances; personal and general accounts; milk register; Panchang alert, calendar, alert settings and guidance; shopping; asset register and lending; reminders and widgets; profiles; settings, themes, security, master data and sub-settings; WhatsApp templates; reports; backup/restore; help; and update notice.
- Responsive mobile layout, animated presentation, keyboard navigation through the tour, and sample-only preview actions.
- A single 30-second, playable launch story on the home page: timed screen scenes, captions in all three languages, pause/resume, previous/next and replay. It is an interactive HTML ad, not a video or audio recording.
- Six guided workflows—entry, farm-season review, asset lending, PDF reporting, milk/payment comparison and app security—with step-by-step instructions and links to the matching screens.
- A feature-set picker to prepare a local discussion list. It does not submit or store a request on a server.
- Customization, security safeguards and shareable built-in PDF reports are highlighted. Security claims are limited to the controls documented in the Android source; no absolute-security or certification claim is made. The current app has seven built-in PDF report types; other requested reports/features require an agreed custom scope.
- Locally bundled fonts and favicon. No build step, server-side code, API, database, analytics, or external runtime dependency is required.

## GoDaddy cPanel launch

This is a static website. It does **not** need Node, PHP, a database, or a GoDaddy backup endpoint.

1. In cPanel, open **File Manager** and the domain's document root (commonly `public_html/`; an addon domain may have a different document root).
2. Upload the **contents of this `website/` folder** so `index.html` is directly in that document root. Preserve the `assets/` folder and its subfolders. Do not upload the Android source repository as the website.
3. Enable the domain's SSL certificate, then visit the HTTPS URL on a phone and desktop. The page adapts to narrow screens and honors the device's reduced-motion setting.
4. If replacing an existing site, keep a copy of its files first. A browser/CDN cache may need clearing after deployment.

## Scope and accuracy

- Preview totals, dates, records and labels are illustrative sample data, not claims about a real farm or live account.
- The tour demonstrates interface concepts only. Save, WhatsApp, report, backup, restore and update controls show an explanatory sample response; they do not run those app functions or send data.
- The Android app's backup destinations and automatic backup require their own in-app configuration. This showcase does not configure or provide an app backup server.
- Supported app functionality is described from this repository's README, navigation/screens, feature specs and report types. Keep public content aligned with the app if those capabilities change.

## Local check

Open `index.html` in a modern browser, or run a static server from this directory (for example `python3 -m http.server 8080`). The site requires no compilation.
