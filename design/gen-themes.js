// Recolours the Vraj Green icon set + logo into the 5 app themes.
const fs = require('fs');
const path = require('path');
const root = process.argv[2]; // .../design
const themes = {
  green: { T1: '#E6F3EB', T2: '#CBE6D6', A1: '#2E7D5B', A2: '#174A37', B1: '#9ED3B3', B2: '#5FA580', H: '#EEF7F1', L: '#123D2D' },
  mati:  { T1: '#FBEAE2', T2: '#F2D0C1', A1: '#B5562E', A2: '#7A3218', B1: '#F0B79A', B2: '#D9805A', H: '#FDF3EE', L: '#5E2410' },
  akash: { T1: '#EEF0FF', T2: '#D6DBFA', A1: '#3F51B5', A2: '#1A237E', B1: '#A9B4FF', B2: '#6F7FE0', H: '#F4F5FF', L: '#141B61' },
  kesar: { T1: '#FFF3DC', T2: '#F8DFAE', A1: '#A86F0A', A2: '#5E3B00', B1: '#F5C76A', B2: '#D99A26', H: '#FFF8EA', L: '#4A2E00' },
  night: { T1: '#22302A', T2: '#18231D', A1: '#8FD3AE', A2: '#4E9A72', B1: '#5FA580', B2: '#2F6B50', H: '#1C2822', L: '#0E2A20' },
};
const G = themes.green;
const iconKeys = ['T1', 'T2', 'A1', 'A2', 'B1', 'B2', 'H'];
const recolor = (svg, map) => svg.replace(/#[0-9A-F]{6}/gi, (hex) => {
  const k = Object.keys(map).find((key) => G[key] && G[key].toUpperCase() === hex.toUpperCase());
  return k ? map[k] : hex;
});
const iconsDir = path.join(root, 'icons');
const logoSrc = fs.readFileSync(path.join(root, 'logo', 'logo-mark.svg'), 'utf8');
for (const [name, pal] of Object.entries(themes)) {
  const out = path.join(root, 'themes', name);
  fs.mkdirSync(out, { recursive: true });
  const iconMap = Object.fromEntries(iconKeys.map((k) => [k, pal[k]]));
  for (const f of fs.readdirSync(iconsDir).filter((f) => f.endsWith('.svg'))) {
    fs.writeFileSync(path.join(out, f), recolor(fs.readFileSync(path.join(iconsDir, f), 'utf8'), iconMap));
  }
  // Logo: background gradient follows the theme; on night the leaves stay light.
  const logoMap = name === 'night'
    ? { A1: '#245E47', L: pal.L }
    : { A1: pal.A1, L: pal.L, B1: pal.B1, B2: pal.B2, T1: pal.T1, T2: pal.T2 };
  let logo = logoSrc.replace(/#123D2D/gi, '@L@');
  logo = recolor(logo, logoMap).replace(/@L@/g, logoMap.L);
  fs.writeFileSync(path.join(out, 'logo.svg'), logo);
}
console.log('themes written:', Object.keys(themes).join(', '));
