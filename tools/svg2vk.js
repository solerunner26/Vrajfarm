// Converts design/icons/*.svg + design/logo/logo-mark.svg into:
//  - app/src/main/java/com/vraj/khetihisab/ui/theme/VkIconSet.kt (path data + ink roles)
//  - app/src/main/res/drawable/ic_vk_<name>.xml (green palette, for Glance / non-Compose use)
//  - adaptive launcher icon drawables + mipmap-anydpi-v26 xml
// Usage: node svg2vk.js <projectRoot>
const fs = require('fs');
const path = require('path');
const root = process.argv[2];
const iconsDir = path.join(root, 'design', 'icons');
const outKt = path.join(root, 'app/src/main/java/com/vraj/khetihisab/ui/theme/VkIconSet.kt');
const resDir = path.join(root, 'app/src/main/res');

const GREEN = { T: ['#E6F3EB', '#CBE6D6'], A: ['#2E7D5B', '#174A37'], B: ['#9ED3B3', '#5FA580'], H: '#EEF7F1' };

const num = (v) => {
  const n = Number(v);
  return Number.isInteger(n) ? String(n) : String(+n.toFixed(4));
};
const attrs = (s) => {
  const o = {};
  s.replace(/([a-zA-Z-:]+)="([^"]*)"/g, (_, k, v) => { o[k] = v; });
  return o;
};
const ink = (v) => {
  if (!v || v === 'none') return null;
  const m = v.match(/url\(#(\w+)\)/);
  if (m) return { t: 'T', a: 'A', b: 'B' }[m[1]] || (() => { throw new Error('unknown grad ' + v); })();
  if (v.toUpperCase() === '#EEF7F1') return 'H';
  throw new Error('unknown colour ' + v);
};
const rectPath = (a) => {
  const x = +a.x || 0, y = +a.y || 0, w = +a.width, h = +a.height;
  let r = Math.min(+(a.rx || a.ry || 0), w / 2, h / 2);
  if (!r) return `M${num(x)},${num(y)}H${num(x + w)}V${num(y + h)}H${num(x)}Z`;
  return `M${num(x + r)},${num(y)}H${num(x + w - r)}A${num(r)},${num(r)} 0 0 1 ${num(x + w)},${num(y + r)}` +
    `V${num(y + h - r)}A${num(r)},${num(r)} 0 0 1 ${num(x + w - r)},${num(y + h)}H${num(x + r)}` +
    `A${num(r)},${num(r)} 0 0 1 ${num(x)},${num(y + h - r)}V${num(y + r)}A${num(r)},${num(r)} 0 0 1 ${num(x + r)},${num(y)}Z`;
};
const circlePath = (a) => {
  const cx = +a.cx, cy = +a.cy, r = +a.r;
  return `M${num(cx - r)},${num(cy)}A${num(r)},${num(r)} 0 1 0 ${num(cx + r)},${num(cy)}A${num(r)},${num(r)} 0 1 0 ${num(cx - r)},${num(cy)}Z`;
};

function parse(svg) {
  const body = svg.replace(/<defs>[\s\S]*?<\/defs>/, '');
  const parts = [];
  let groupFill = null;
  const re = /<(\/?)(g|rect|circle|path)\b([^>]*?)(\/?)>/g;
  let m;
  while ((m = re.exec(body))) {
    const [, close, tag, rest] = m;
    if (tag === 'g') { groupFill = close ? null : attrs(rest).fill; continue; }
    const a = attrs(rest);
    const d = tag === 'rect' ? rectPath(a) : tag === 'circle' ? circlePath(a) : a.d;
    const fillAttr = a.fill !== undefined ? a.fill : groupFill;
    let rotate = 0;
    if (a.transform) {
      const r = a.transform.match(/rotate\(([-\d.]+)\s+48\s+48\)/);
      if (!r) throw new Error('unsupported transform ' + a.transform);
      rotate = +r[1];
    }
    parts.push({
      d,
      fill: ink(fillAttr),
      stroke: ink(a.stroke),
      sw: a['stroke-width'] ? +a['stroke-width'] : 0,
      alpha: a.opacity ? +a.opacity : 1,
      cap: a['stroke-linecap'] === 'round',
      join: a['stroke-linejoin'] === 'round',
      rotate,
    });
  }
  return parts;
}

// ---------- Kotlin ----------
const files = fs.readdirSync(iconsDir).filter((f) => f.endsWith('.svg')).sort();
const entries = files.map((f) => {
  const name = path.basename(f, '.svg');
  return { name, parts: parse(fs.readFileSync(path.join(iconsDir, f), 'utf8')) };
});
const kInk = (i) => (i ? `VkInk.${i}` : 'null');
let kt = `// GENERATED from design/icons/*.svg — do not edit by hand.
// Regenerate: node tools/svg2vk.js <projectRoot>
package com.vraj.khetihisab.ui.theme

internal enum class VkInk { T, A, B, H }

internal class VkIconPart(
    val d: String,
    val fill: VkInk?,
    val stroke: VkInk? = null,
    val strokeWidth: Float = 0f,
    val alpha: Float = 1f,
    val roundCap: Boolean = false,
    val roundJoin: Boolean = false,
    val rotate: Float = 0f,
)

/** The ${entries.length} two-tone \`ic_vk_*\` icons (96×96 viewport). Draw with [VkIconImage]; never tint. */
enum class VkIcon(internal val resName: String, internal val parts: List<VkIconPart>) {
`;
for (const e of entries) {
  kt += `    ${e.name.toUpperCase()}(\n        "ic_vk_${e.name}",\n        listOf(\n`;
  for (const p of e.parts) {
    const extra = [];
    if (p.stroke) extra.push(`stroke = ${kInk(p.stroke)}`, `strokeWidth = ${p.sw}f`);
    if (p.alpha !== 1) extra.push(`alpha = ${p.alpha}f`);
    if (p.cap) extra.push('roundCap = true');
    if (p.join) extra.push('roundJoin = true');
    if (p.rotate) extra.push(`rotate = ${p.rotate}f`);
    kt += `            VkIconPart("${p.d}", ${kInk(p.fill)}${extra.length ? ', ' + extra.join(', ') : ''}),\n`;
  }
  kt += `        ),\n    ),\n`;
}
kt += '}\n';
fs.writeFileSync(outKt, kt);

// ---------- Android vector drawables (green) ----------
const argb = (hex, alpha = 1) => '#' + Math.round(alpha * 255).toString(16).padStart(2, '0').toUpperCase() + hex.slice(1).toUpperCase();
const gradXml = (attr, colors, x1, y1, x2, y2, indent) =>
  `${indent}<aapt:attr name="${attr}">\n${indent}    <gradient android:type="linear" android:startX="${x1}" android:startY="${y1}" android:endX="${x2}" android:endY="${y2}">\n` +
  colors.map((c, i) => `${indent}        <item android:offset="${i / (colors.length - 1)}" android:color="${argb(c)}" />\n`).join('') +
  `${indent}    </gradient>\n${indent}</aapt:attr>\n`;
const inkXml = (attr, i, pal, indent) => (i === 'H'
  ? null
  : gradXml(attr, pal[i], 0, 0, 96, 96, indent));
function pathXml(p, pal, indent) {
  let open = `${indent}<path\n${indent}    android:pathData="${p.d}"`;
  if (p.fill === 'H') open += `\n${indent}    android:fillColor="${argb(pal.H)}"`;
  if (p.stroke === 'H') open += `\n${indent}    android:strokeColor="${argb(pal.H)}"`;
  if (p.stroke) open += `\n${indent}    android:strokeWidth="${p.sw}"`;
  if (p.cap) open += `\n${indent}    android:strokeLineCap="round"`;
  if (p.join) open += `\n${indent}    android:strokeLineJoin="round"`;
  if (p.alpha !== 1) {
    if (p.fill) open += `\n${indent}    android:fillAlpha="${p.alpha}"`;
    if (p.stroke) open += `\n${indent}    android:strokeAlpha="${p.alpha}"`;
  }
  const kids = [];
  if (p.fill && p.fill !== 'H') kids.push(inkXml('android:fillColor', p.fill, pal, indent + '    '));
  if (p.stroke && p.stroke !== 'H') kids.push(inkXml('android:strokeColor', p.stroke, pal, indent + '    '));
  if (!kids.length) return open + ' />\n';
  return open + '>\n' + kids.join('') + `${indent}</path>\n`;
}
const header = (w, h, vw, vh) => `<?xml version="1.0" encoding="utf-8"?>
<!-- GENERATED from design sources by svg2vk.js. Compose screens use VkIcon (theme-aware) instead. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="${w}dp"
    android:height="${h}dp"
    android:viewportWidth="${vw}"
    android:viewportHeight="${vh}">
`;
for (const e of entries) {
  let x = header(40, 40, 96, 96);
  for (const p of e.parts) {
    if (p.rotate) {
      x += `    <group android:rotation="${p.rotate}" android:pivotX="48" android:pivotY="48">\n${pathXml(p, GREEN, '        ')}    </group>\n`;
    } else x += pathXml(p, GREEN, '    ');
  }
  x += '</vector>\n';
  fs.writeFileSync(path.join(resDir, 'drawable', `ic_vk_${e.name}.xml`), x);
}

// ---------- Adaptive launcher icon from logo-mark.svg ----------
// Logo viewBox 192. Sprout + ledger lines span x 38..154, y 36..169 → scale into the 66dp safe zone.
const s = 0.43;
const tx = +(54 - 96 * s).toFixed(3);
const ty = +(54 - 102.5 * s).toFixed(3);
const logoGrad = (attr, c1, c2, x1, y1, x2, y2, ind) => gradXml(attr, [c1, c2], x1, y1, x2, y2, ind);
const fgPaths = (mono, ind) => {
  const fill = (c1, c2, x1, y1, x2, y2) => mono ? null : logoGrad('android:fillColor', c1, c2, x1, y1, x2, y2, ind + '    ');
  const one = (d, extra, g) => {
    let o = `${ind}<path\n${ind}    android:pathData="${d}"${extra}`;
    if (mono) return o + ' />\n';
    return o + '>\n' + g + `${ind}</path>\n`;
  };
  const monoFill = mono ? `\n${ind}    android:fillColor="#FFFFFFFF"` : '';
  const monoStroke = mono ? `\n${ind}    android:strokeColor="#FFFFFFFF"` : '';
  return [
    one('M96,148V92', `${monoStroke}\n${ind}    android:strokeWidth="9"\n${ind}    android:strokeLineCap="round"`,
      mono ? '' : logoGrad('android:strokeColor', '#E6F3EB', '#CBE6D6', 52, 0, 140, 0, ind + '    ')),
    one('M96,108C60,108 38,84 38,52c34,-2 58,18 58,56z', monoFill, fill('#E6F3EB', '#9ED3B3', 40, 40, 96, 104)),
    one('M96,98c0,-34 22,-56 58,-58 0,36 -22,58 -58,58z', `${monoFill}\n${ind}    android:fillAlpha="0.96"`, fill('#CBE6D6', '#5FA580', 152, 36, 96, 96)),
    one(rectPath({ x: 50, y: 146, width: 92, height: 9, rx: 4.5 }), monoFill, fill('#E6F3EB', '#CBE6D6', 52, 0, 140, 0)),
    one(rectPath({ x: 66, y: 162, width: 60, height: 7, rx: 3.5 }), `${monoFill}\n${ind}    android:fillAlpha="0.7"`, fill('#E6F3EB', '#CBE6D6', 52, 0, 140, 0)),
  ].join('');
};
const fgXml = (mono) => header(108, 108, 108, 108) +
  `    <group android:scaleX="${s}" android:scaleY="${s}" android:translateX="${tx}" android:translateY="${ty}">\n` +
  fgPaths(mono, '        ') + '    </group>\n</vector>\n';
fs.writeFileSync(path.join(resDir, 'drawable', 'ic_launcher_foreground.xml'), fgXml(false));
fs.writeFileSync(path.join(resDir, 'drawable', 'ic_launcher_monochrome.xml'), fgXml(true));
const bgXml = header(108, 108, 108, 108) +
  `    <path android:pathData="M0,0h108v108h-108z">\n${gradXml('android:fillColor', ['#2E7D5B', '#123D2D'], 0, 0, 108, 108, '        ')}    </path>\n` +
  `    <path android:pathData="M0,0h108v108h-108z">\n` +
  `        <aapt:attr name="android:fillColor">\n            <gradient android:type="radial" android:centerX="54" android:centerY="38" android:gradientRadius="60">\n` +
  `                <item android:offset="0" android:color="#599ED3B3" />\n                <item android:offset="1" android:color="#009ED3B3" />\n` +
  `            </gradient>\n        </aapt:attr>\n    </path>\n</vector>\n`;
fs.writeFileSync(path.join(resDir, 'drawable', 'ic_launcher_background.xml'), bgXml);
const adaptive = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
`;
fs.mkdirSync(path.join(resDir, 'mipmap-anydpi-v26'), { recursive: true });
fs.writeFileSync(path.join(resDir, 'mipmap-anydpi-v26', 'ic_launcher.xml'), adaptive);
fs.writeFileSync(path.join(resDir, 'mipmap-anydpi-v26', 'ic_launcher_round.xml'), adaptive);

// ---------- Logo for in-app use (Compose ImageVector data) ----------
console.log('icons:', entries.map((e) => e.name).join(', '));
console.log('count:', entries.length);
