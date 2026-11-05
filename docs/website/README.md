# Kheti Hisab launch website

A static, interactive showcase of the Kheti Hisab Android app for client events. English, Gujarati and Hindi; the app's five themes; generic sample data only (no owner, family or borrower names).

## What it contains

- **Launch film** – plays automatically on load and loops: 8 scenes of real app screens with captions, pause/play, previous/next, replay, scene dots and full screen. It is an animated HTML "film", not a video file, so it needs no player and stays sharp on any screen.
- **Four promises** – interactive panels: made-to-measure (tap modules to add them to a live phone), security (six real safeguards plus a working PIN-lock demo), PDF reports (pick any of the 7 reports to preview it, share buttons) and "you choose, we build".
- **Features grid** – 12 module cards; each opens its screen in the tour.
- **Screen-by-screen tour** – all 30 app screens, grouped, each with purpose and three steps. Glowing parts inside the screen are tappable and move through the app like the real thing. Mobile and web (desktop) views, auto play, keyboard arrows.
- **Feature chooser** – 16 modules plus free text; copy the list or share it on WhatsApp (opens WhatsApp ready, never sends by itself). Kept in the visitor's browser only.
- **Theme and language switchers** – the whole site and every mock screen re-colour with the app's exact theme tokens (`design/vk-theme.css`).

## Files

| File | Purpose |
|---|---|
| `index.html` | Page structure |
| `site.css` | Theme tokens, site layout and the mock app components (`m-*`) |
| `i18n.js` | All copy in three languages |
| `app.js` | Screen specs (30 screens as compact data), one renderer, film, tour, chooser |
| `assets/icons/` | The app's own 25 two-tone icons |
| `assets/fonts/` | Noto Sans Gujarati, Noto Sans Devanagari, Baloo Bhai 2 (OFL licences included) |

To add or change a screen, edit its entry in `S` in `app.js` and its texts in `sc` in `i18n.js`.

## GoDaddy cPanel upload

Static only: no PHP, database or server code. Upload `docs/Farm-Ledger-Website.zip` in cPanel File Manager to the domain's document root and extract it there so `index.html` is at the top level. Check the HTTPS address on a phone and a desktop afterwards.

Open `index.html` locally, or run `python -m http.server` in this folder, to preview.
