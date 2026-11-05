/* Kheti Hisab launch site. One renderer draws every app screen from the compact specs in S;
   the film, tour, security demo, PDF demo and feature picker all reuse it. Sample data only. */
(() => {
'use strict';
const $ = (s, r = document) => r.querySelector(s), $$ = (s, r = document) => [...r.querySelectorAll(s)];
const store = { get(k, d) { try { return localStorage.getItem('kh-' + k) ?? d } catch { return d } }, set(k, v) { try { localStorage.setItem('kh-' + k, v) } catch {} } };
const THEMES = [['green', '#2E7D5B', '#174A37'], ['mati', '#B5562E', '#7A3218'], ['akash', '#3F51B5', '#1A237E'], ['kesar', '#9A6408', '#5E3B00'], ['night', '#245E47', '#0E2A20']];
const THEME_NAMES = { en: ['Green', 'Mati', 'Akash', 'Kesar', 'Night'], gu: ['લીલો', 'માટી', 'આકાશ', 'કેસર', 'રાત્રિ'], hi: ['हरा', 'माटी', 'आकाश', 'केसर', 'रात्रि'] };
let lang = store.get('lang', 'gu'), theme = store.get('theme', 'mati'), eye = false;
if (!L[lang]) lang = 'gu';
if (!THEMES.some(t => t[0] === theme)) theme = 'mati';
const U = () => L[lang].u, T = k => L[lang].s[k] ?? k;
const rs = n => (n < 0 ? '−' : '') + '₹' + Math.abs(n).toLocaleString('en-IN');
const icon = (n, c = '') => n ? `<img class="ic ${c}" src="assets/icons/${n}.svg" alt="" loading="lazy">` : '';
// '=x' literal · 'a|b' label + b · 'n~a' n + label · arrays join with ·
const t = k => k == null ? '' : Array.isArray(k) ? k.map(t).join(' · ') : typeof k === 'number' ? rs(k)
  : k[0] === '=' ? k.slice(1) : k.includes('|') ? t(k.split('|')[0]) + ' ' + k.split('|')[1]
  : k.includes('~') ? k.split('~')[0] + ' ' + t(k.split('~')[1]) : U()[k] ?? k;
const go = g => g ? ` data-go="${g}"` : '';

/* ---------- the 30 app screens: [id, group, icon, blocks] ---------- */
const S = [
['lock','start','lock',[['logo'],['lbl','enterPin'],['pin'],['btn','fingerprint','home','ton']]],
['home','start','farm',[['hh'],['hero','netProfit',248600,'thisYear',1],['tiles',[['farm','farms','farms'],['family','bhagiya','bhagiya'],['animal','milk','milk'],['personal','personal','personal'],['utility','general','general'],['shopping','shopping','shopping'],['asset','assets','assets'],['report','reports','reports']]],['row','bill','creditDue','creditSub',18500,'out','credit'],['btn2',['addIncome','entry'],['addExpense','entry']]]],
['update','start','settings',[['hh'],['hero','netProfit',248600,'thisYear',1],['dlg']]],
['entry','money','income',[['bar','newEntry'],['seg',['income','expense'],1],['lbl','section'],['opts',[['farm','farm'],['animal','animal'],['family','bhagiya'],['utility','general'],['personal','personal']],0],['lbl','type'],['opts',[[0,'fertilizer'],[0,'seed'],[0,'labour'],[0,'diesel'],[0,'newType']],0],['amt',2000],['lbl','pay'],['seg',['paidNow','toPay'],0],['tiles',[['mic','voice','voice'],['calculator','apmc','apmc'],['camera','billPhoto']],3],['btn','save','home']]],
['apmc','money','calculator',[['bar','apmc','entry'],['fld','crop','cotton'],['fld','mann','=42'],['fld','extraKg','=8'],['fld','rate','=₹1,450'],['calc'],['btn','useAmount','entry']]],
['voice','money','mic',[['bar','voice','entry'],['mic'],['note','filled'],['row','expense','fertilizer','farm|3',2000,'out'],['row','bill','credit','agro',null,'tag:toPay'],['btn','save','home']]],
['credit','money','bill',[['bar','credit'],['stats',[['toPay',18500,'out'],['toReceive',42000,'in']]],['row','bill','agro','fertilizer',12000,'out'],['row','bill','seedShop','seed',6500,'out'],['row','income','trader','cotton',42000,'in'],['btn','settle','home','ton']]],
['personal','money','personal',[['bar','personal'],['note','personalNote'],['hero','balance',18200,'thisMonth'],['row','income','salary','thisMonth',45000,'in'],['row','expense','household','ago',21800,'out'],['row','expense','school','thisMonth',5000,'out']]],
['general','money','utility',[['bar','general'],['bars',[40,55,35,70,50,62]],['row','utility','light','thisMonth',3450,'out'],['row','utility','water','thisMonth',800,'out'],['row','utility','cctv','thisMonth',499,'out']]],
['farms','farm','farm',[['bar','farms'],['seg',['season','year'],0],['row','farm','farm|1','12~vigha',96400,'in','farmDetail'],['row','farm','farm|2','8~vigha',54200,'in','farmDetail'],['row','farm','farm|3','15~vigha',118200,'in','farmDetail'],['row','farm','farm|4','6~vigha',-8600,'out','farmDetail']]],
['farmDetail','farm','farm',[['bar','farm|3','farms'],['hero','profit',118200,['cotton','15~vigha']],['stats',[['perVigha',7880,'in'],['vigha','=15','']]],['bars',[30,80,45,25,60,90]],['row','income','cotton','apmc',125400,'in','apmc'],['row','expense','fertilizer','season',4200,'out'],['row','expense','labour','season',3000,'out']]],
['bhagiya','farm','family',[['bar','bhagiya'],['seg',['managerA','managerB'],0],['hero','totalAdvance',85000,'thisYear'],['row','family','advance','=05-11-2026',5000,'out'],['row','family','advance','=21-10-2026',10000,'out'],['wa','waAdv']]],
['milk','farm','animal',[['bar','milk'],['seg',['morning','evening'],0],['row','animal','cow|1',['6.5~litres','fat|4.2%'],338,'in'],['row','animal','buffalo|1',['8~litres','fat|7.1%'],520,'in'],['row','animal','buffalo|2',['7.5~litres','fat|6.8%'],488,'in'],['stats',[['dairyDue',40380,''],['dairyPaid',39600,''],['diff',780,'out']]]]],
['calendar','panchang','panchang',[['bar','calendar'],['moon'],['cal'],['legend']]],
['festival','panchang','diya',[['bar','alert','settings'],['moon'],['fest'],['tg','eveAlert',1],['tg','alert',1]]],
['assets','assets','asset',[['bar','assets'],['stats',[['total','=12',''],['available','=10','in'],['lent','=2','out']]],['row','asset','tractor','available',null,'tag:available'],['row','asset','sprayer','neighbour',null,'tag:lent','lend'],['row','asset','rotavator','available',null,'tag:available'],['btn2',['addAsset',null],['lend','lend']]]],
['lend','assets','lend',[['bar','lend','assets'],['photo'],['fld','borrower','neighbour'],['fld','assets','sprayer'],['fld','handover','=28-10-2026'],['fld','returnIn','days7'],['wa','waLend'],['btn','saveSend','assets']]],
['shopping','assets','shopping',[['bar','shopping'],['seg',['farmTab','homeTab'],0],['chk','urea',1],['chk','drip',0],['chk','seed',0],['note','toolToAsset']]],
['reminders','assets','reminder',[['bar','reminders'],['row','reminder','remPump','=10-11-2026',null,'tag:high'],['row','reminder','remInsure','=15-11-2026',null,'tag:normal'],['chk','done',1]]],
['widgets','assets','reminder',[['wid']]],
['reports','reports','report',[['bar','reports','settings'],['fld','period','thisMonth'],...[1,2,3,4,5,6,7].map(i => ['row','report','r' + i,null,null,'','pdf']),['btn','pdfMake','pdf']]],
['pdf','reports','report',[['bar','r2','reports'],['pdf'],['share']]],
['backup','reports','backup',[['bar','backup','settings'],['note','lastBackup'],['row','backup','drive',null,null,'tag:on'],['row','message','waSelf'],['row','message','mailOne'],['btn','backupNow'],['btn','restore',null,'ton']]],
['settings','settings','settings',[['bar','settings'],['tiles',[['profile','theme','themes'],['lock','security','security'],['backup','backup','backup'],['panchang','alert','festival'],['farm','masterData','master'],['message','templates','templates'],['report','reports','reports'],['family','profiles','profiles'],['help','help','help'],['settings','update','update']],3]]],
['themes','settings','settings',[['bar','theme','settings'],['themes']]],
['security','settings','lock',[['bar','security','settings'],['tg','pinLock',1],['tg','fingerprint',1],['tg','noShots',1],['tg','autoLock',1],['btn','changePin','lock','ton']]],
['master','settings','farm',[['bar','masterData','settings'],['row','farm','farmVigha',['8~farms','74~vigha']],['row','farm','crops',['cotton','groundnut','wheat']],['row','expense','expTypes',['fertilizer','seed','labour']],['row','family','bhagiya',['managerA','managerB']]]],
['templates','settings','message',[['bar','templates','settings'],['wa','waAdv'],['wa','waLend'],['btn','save','settings']]],
['profiles','settings','profile',[['bar','profiles','settings'],['row','profile','owner','=98XXX XXX01'],['row','family','managerA','=98XXX XXX02'],['row','family','managerB','=98XXX XXX03']]],
['help','settings','help',[['bar','help','settings'],['row','help','faq1'],['row','help','faq2'],['row','help','faq3'],['note','neverAuto']]]
];
const IDX = Object.fromEntries(S.map((s, i) => [s[0], i]));
const GROUPS = ['start', 'money', 'farm', 'panchang', 'assets', 'reports', 'settings'];
const NO_DOCK = new Set(['lock', 'update', 'widgets']);
const SC = id => L[lang].sc[id];

/* ---------- block renderer ---------- */
const moonSvg = `<svg class="moon" viewBox="0 0 60 60" aria-hidden="true"><defs><radialGradient id="mg" cx=".35" cy=".35"><stop offset="0" stop-color="#FFF7DE"/><stop offset="1" stop-color="#E2C27A"/></radialGradient></defs><circle cx="30" cy="30" r="26" fill="#3A3220"/><path d="M30 4a26 26 0 0 1 0 52a14 26 0 0 0 0-52z" fill="url(#mg)"/></svg>`;
function block(b) {
  const [k, a, c, d, e, f, g] = b, u = U();
  switch (k) {
    case 'logo': return `<div class="m-logo"><img src="assets/icons/farm.svg" alt=""><b>${u.appName}</b></div>`;
    case 'hh': return `<div class="m-hh"><div><small>${u.hello}</small><b>${u.appName}</b></div><button class="m-chip hot" data-go="calendar">${u.today} · 5/11</button><button class="m-eye hot" data-act="eye" aria-label="eye">${eye ? '◉' : '◎'}</button></div>`;
    case 'bar': return `<div class="m-bar">${c !== null ? `<button class="m-back hot"${go(c || 'home')} aria-label="back">‹</button>` : ''}<b>${t(a)}</b></div>`;
    case 'hero': return `<div class="m-hero"><small>${t(a)}</small><strong>${e && !eye ? '₹XXXXX.XX' : rs(c)}</strong><small>${t(d)}${e ? ' · ' + u.tapEye : ''}</small></div>`;
    case 'tiles': return `<div class="m-tiles c${c || 4}">${a.map(([i, l, g2]) => `<button class="m-tile${g2 ? ' hot' : ''}"${go(g2)}>${icon(i)}<span>${t(l)}</span></button>`).join('')}</div>`;
    case 'row': { const tag = f && f.startsWith('tag:') ? f.slice(4) : ''; return `<div class="m-row${g ? ' hot' : ''}"${go(g)}>${icon(a)}<span class="m-rt"><b>${t(c)}</b>${d != null ? `<small>${t(d)}</small>` : ''}</span>${e != null ? `<em class="${f}">${f === 'in' ? '+' : ''}${rs(e)}</em>` : ''}${tag ? `<i class="m-tag ${tag}">${tag === 'on' ? '✓' : t(tag)}</i>` : ''}</div>`; }
    case 'seg': return `<div class="m-seg">${a.map((l, i) => `<button class="${i === c ? 'on' : ''}" data-act="seg">${t(l)}</button>`).join('')}</div>`;
    case 'lbl': return `<p class="m-lbl">${t(a)}</p>`;
    case 'opts': return `<div class="m-opts">${a.map(([i, l], n) => `<button class="${n === c ? 'on' : ''}" data-act="opt">${icon(i)}<span>${t(l)}</span></button>`).join('')}</div>`;
    case 'amt': return `<div class="m-amt"><small>${u.amount}</small><b>${rs(a)}</b></div>`;
    case 'fld': return `<div class="m-fld"><small>${t(a)}</small><b>${t(c)}</b></div>`;
    case 'btn': return `<button class="m-btn ${c || ''}${a && b[2] ? ' hot' : ''}"${go(b[2])}>${t(a)}</button>`;
    case 'btn2': return `<div class="m-two">${[a, c].map(([l, g2], i) => `<button class="m-btn ${i ? 'ton' : ''}${g2 ? ' hot' : ''}"${go(g2)}>${t(l)}</button>`).join('')}</div>`;
    case 'pin': return `<div class="m-dots">${'<i></i>'.repeat(4)}</div><div class="m-pad">${[1,2,3,4,5,6,7,8,9,'',0,'⌫'].map(n => n === '' ? '<span></span>' : `<button data-act="pin" data-n="${n}">${n}</button>`).join('')}</div><p class="m-hint">${u.tryPin}</p>`;
    case 'cal': { const inc = [2,9,14,18,23,27], exp = [3,7,11,15,20,24,28]; let h = u.wk.split(' ').map(w => `<b>${w}</b>`).join(''); for (let i = 1; i <= 30; i++) h += `<span class="${inc.includes(i) ? 'in' : exp.includes(i) ? 'out' : ''}${i === 5 ? ' now' : ''}">${i}</span>`; return `<div class="m-cal"><p>${u.month}</p><div>${h}</div></div>`; }
    case 'legend': return `<div class="m-legend"><span><i class="in"></i>${u.incomeDay}</span><span><i class="out"></i>${u.expenseDay}</span></div>`;
    case 'bars': return `<div class="m-bars">${a.map((h, i) => `<i style="--h:${h}%;--d:${i * 60}ms"></i>`).join('')}</div>`;
    case 'note': return `<p class="m-note">${t(a)}</p>`;
    case 'wa': return `<div class="m-wa"><p>${t(a)}</p><small>${u.neverAuto}</small></div>`;
    case 'pdf': return `<div class="m-pdf"><div class="m-lh"><img src="assets/icons/farm.svg" alt=""><span><b>${u.yourFarm}</b><small>${u.r2} · ${u.thisMonth}</small></span></div>${[['farm|1',96400],['farm|2',54200],['farm|3',118200],['farm|4',-8600]].map(([l, v]) => `<p><span>${t(l)}</span><b>${rs(v)}</b></p>`).join('')}<p class="tot"><span>${u.total}</span><b>${rs(260200)}</b></p></div>`;
    case 'share': return `<div class="m-two">${['share', 'email', 'print'].map(l => `<button class="m-btn ton" data-act="share">${u[l]}</button>`).join('')}</div>`;
    case 'themes': return `<div class="m-themes">${THEMES.map(([id, g1, g2], i) => `<button class="${id === theme ? 'on' : ''}" data-act="theme" data-t="${id}" style="--a:${g1};--b:${g2}"><i></i>${THEME_NAMES[lang][i]}</button>`).join('')}</div>`;
    case 'tg': return `<div class="m-tg"><span>${t(a)}</span><button class="sw ${c ? 'on' : ''}" data-act="tg" aria-label="${t(a)}"></button></div>`;
    case 'chk': return `<button class="m-chk ${c ? 'on' : ''}" data-act="chk"><i></i>${t(a)}</button>`;
    case 'moon': return `<button class="m-panch hot" data-go="calendar">${moonSvg}<span><b>${u.tithiNow}</b><small>${u.sunrise}</small></span></button>`;
    case 'fest': return `<div class="m-fest">${icon('diya')}<span><b>${u.festTomorrow}</b><small>${u.belief}</small></span></div>`;
    case 'calc': return `<div class="m-calc"><p><span>${u.totalMann}</span><b>42 + 8 ÷ 20 = 42.4</b></p><p><span>${u.total}</span><strong>${rs(61480)}</strong></p></div>`;
    case 'mic': return `<div class="m-mic"><button data-act="share" aria-label="mic">${icon('mic')}</button><small>${u.listening}</small><p>${u.voiceSample}</p></div>`;
    case 'stats': return `<div class="m-stats">${a.map(([l, v, tone]) => `<div><small>${t(l)}</small><b class="${tone}">${t(v)}</b></div>`).join('')}</div>`;
    case 'photo': return `<div class="m-photo">${icon('camera')}<small>${u.photo}</small></div>`;
    case 'dlg': return `<div class="m-scrim"><div class="m-dlg">${icon('settings')}<b>${u.updTitle}</b><p>${u.updBody}</p><button class="m-btn hot" data-go="home">${u.updBtn}</button></div></div>`;
    case 'wid': return `<div class="m-home"><div class="m-w gold">${moonSvg}<span><b>${u.widToday}</b><small>${u.tithiNow}</small></span></div><div class="m-w hot" data-go="shopping">${icon('shopping')}<span><b>${u.shopping}</b><small>${u.urea}</small></span></div><div class="m-w hot" data-go="reminders">${icon('reminder')}<span><b>${u.reminders}</b><small>${u.remPump}</small></span></div><div class="m-apps">${['farm','panchang','message','camera'].map(i => icon(i)).join('')}</div></div>`;
  }
  return '';
}
const dock = id => `<nav class="m-dock">${[['home','farm','dash'],['calendar','panchang','cal'],['reports','report','reports'],['settings','settings','more']].map(([g2, i, l]) => `<button class="hot${g2 === id ? ' on' : ''}"${go(g2)}>${icon(i)}<span>${U()[l]}</span></button>`).join('')}</nav>`;
const screen = id => { const s = S[IDX[id]]; return `<div class="m-scr" data-id="${id}"><div class="m-body">${s[3].map(block).join('')}</div>${NO_DOCK.has(id) ? '' : dock(id)}</div>`; };

/* ---------- interactions inside any rendered screen ---------- */
let pinBuf = '';
function bindDevice(root, onGo, live) {
  root.addEventListener('click', e => {
    const el = e.target.closest('[data-go],[data-act]'); if (!el || !live()) return;
    const act = el.dataset.act;
    if (act === 'seg') { $$('button', el.parentNode).forEach(x => x.classList.toggle('on', x === el)); return; }
    if (act === 'opt') { $$('button', el.parentNode).forEach(x => x.classList.toggle('on', x === el)); return; }
    if (act === 'tg' || act === 'chk') { el.classList.toggle('on'); return; }
    if (act === 'eye') { eye = !eye; onGo(root.dataset.cur); return; }
    if (act === 'theme') { setTheme(el.dataset.t); return; }
    if (act === 'share') { toast(T('toastShare')); return; }
    if (act === 'pin') {
      const dots = $$('.m-dots i', root);
      pinBuf = el.dataset.n === '⌫' ? pinBuf.slice(0, -1) : (pinBuf + el.dataset.n).slice(0, 4);
      dots.forEach((d, i) => d.classList.toggle('on', i < pinBuf.length));
      if (pinBuf.length === 4) { pinBuf = ''; $('.m-dots', root).classList.add('ok'); toast(T('unlocked')); setTimeout(() => onGo('home'), 550); }
      return;
    }
    if (el.dataset.go) onGo(el.dataset.go);
  });
}

/* ---------- toast ---------- */
let toastT;
function toast(msg) { const el = $('#toast'); el.textContent = msg; el.classList.add('show'); clearTimeout(toastT); toastT = setTimeout(() => el.classList.remove('show'), 2600); }

/* ---------- launch film ---------- */
const FILM = ['home', 'entry', 'farmDetail', 'calendar', 'lend', 'pdf', 'security', 'themes'];
const film = { i: 0, playing: true, timer: 0, dur: 5200 };
function filmShow(i) {
  film.i = (i + FILM.length) % FILM.length;
  const [h, p] = L[lang].f[film.i], saveEye = eye; eye = true;
  $('#filmScreen').innerHTML = screen(FILM[film.i]); eye = saveEye;
  $('#filmH').textContent = h; $('#filmP').textContent = p;
  $('#filmCount').textContent = String(film.i + 1).padStart(2, '0') + ' / ' + String(FILM.length).padStart(2, '0');
  $$('#filmDots i').forEach((d, n) => d.classList.toggle('on', n === film.i));
  const stage = $('#film'); stage.classList.remove('anim'); void stage.offsetWidth; stage.classList.add('anim');
  filmArm();
}
function filmArm() {
  clearTimeout(film.timer); const bar = $('#filmBar');
  bar.style.transition = 'none'; bar.style.width = '0';
  if (!film.playing) return;
  void bar.offsetWidth; bar.style.transition = `width ${film.dur}ms linear`; bar.style.width = '100%';
  film.timer = setTimeout(() => filmShow(film.i + 1), film.dur);
}
function filmPlay(on) { film.playing = on; $('#filmPlay').textContent = on ? '❚❚ ' + T('pause') : '▶ ' + T('play'); $('#film').classList.toggle('paused', !on); filmArm(); }

/* ---------- tour ---------- */
const tour = { i: IDX.home, view: 'm', auto: false, timer: 0 };
function tourGo(id) {
  tour.i = typeof id === 'number' ? (id + S.length) % S.length : IDX[id] ?? 0;
  const s = S[tour.i], sc = SC(s[0]), dev = $('#device');
  dev.dataset.cur = s[0];
  dev.className = 'device ' + (tour.view === 'w' ? 'web' : 'phone');
  const web = tour.view === 'w' ? `<div class="w-top"><i></i><i></i><i></i><span>${U().appName} · ${sc[0]}</span></div><aside class="w-side">${GROUPS.map(g => `<button class="${s[1] === g ? 'on' : ''}" data-go="${S.find(x => x[1] === g)[0]}">${icon(S.find(x => x[1] === g)[2])}<span>${T('g' + g)}</span></button>`).join('')}</aside>` : '<div class="notch"></div>';
  dev.innerHTML = web + `<div class="d-screen">${screen(s[0])}</div>`;
  $('#stTitle').textContent = sc[0]; $('#stPurpose').textContent = sc[1];
  $('#stSteps').innerHTML = sc.slice(2).map(x => `<li>${x}</li>`).join('');
  $('#stCount').textContent = String(tour.i + 1).padStart(2, '0') + ' / ' + S.length;
  $('#stIcon').src = `assets/icons/${s[2]}.svg`;
  $$('#tourList button').forEach(b => b.classList.toggle('on', b.dataset.id === s[0]));
  const cur = $('#tourList button.on'), lst = $('#tourList');
  if (cur && (cur.offsetTop < lst.scrollTop || cur.offsetTop > lst.scrollTop + lst.clientHeight - 50)) lst.scrollTop = cur.offsetTop - 90;
  $('#tourSelect').value = s[0];
  if (tour.auto) { clearTimeout(tour.timer); tour.timer = setTimeout(() => tourGo(tour.i + 1), 6000); }
}
function tourList() {
  $('#tourList').innerHTML = GROUPS.map(g => `<p>${T('g' + g)}</p>` + S.filter(s => s[1] === g).map(s => `<button data-id="${s[0]}">${icon(s[2])}<span>${SC(s[0])[0]}</span><small>${String(IDX[s[0]] + 1).padStart(2, '0')}</small></button>`).join('')).join('');
  $('#tourSelect').innerHTML = GROUPS.map(g => `<optgroup label="${T('g' + g)}">` + S.filter(s => s[1] === g).map(s => `<option value="${s[0]}">${String(IDX[s[0]] + 1).padStart(2, '0')} · ${SC(s[0])[0]}</option>`).join('') + '</optgroup>').join('');
}

/* ---------- features, USP and chooser ---------- */
const FEATS = ['entry', 'apmc', 'voice', 'credit', 'farmDetail', 'bhagiya', 'milk', 'calendar', 'lend', 'shopping', 'reports', 'backup'];
const MODS = ['entry', 'apmc', 'voice', 'credit', 'farmDetail', 'bhagiya', 'milk', 'personal', 'calendar', 'lend', 'shopping', 'reminders', 'reports', 'backup', 'themes', 'security'];
let picks = new Set((store.get('picks', 'entry,farmDetail,calendar,reports')).split(',').filter(x => MODS.includes(x)));
let usp = 0, report = 2;
function renderFeatures() {
  $('#featGrid').innerHTML = FEATS.map((id, i) => `<button class="feat rv" data-id="${id}" style="--d:${i * 40}ms">${icon(S[IDX[id]][2])}<b>${SC(id)[0]}</b><span>${SC(id)[1]}</span><i>→</i></button>`).join('');
}
function renderUsp() {
  $$('#uspTabs button').forEach((b, i) => b.classList.toggle('on', i === usp));
  const p = $('#uspPanel'), u = U();
  if (usp === 0) p.innerHTML = `<div><ol class="steps4">${['c1', 'c2', 'c3', 'c4'].map(k => `<li>${T(k)}</li>`).join('')}</ol><p class="hint">${T('customDemo')}</p><div class="pickgrid">${MODS.map(id => `<button class="${picks.has(id) ? 'on' : ''}" data-pick="${id}">${icon(S[IDX[id]][2])}<span>${SC(id)[0]}</span></button>`).join('')}</div></div><div class="mini"><div class="m-scr"><div class="m-body">${block(['hh'])}<div class="m-tiles c3">${[...picks].map(id => `<span class="m-tile pop">${icon(S[IDX[id]][2])}<span>${SC(id)[0]}</span></span>`).join('') || `<p class="m-note">${T('empty')}</p>`}</div></div></div></div>`;
  if (usp === 1) { p.innerHTML = `<ul class="seclist">${[1, 2, 3, 4, 5, 6].map(n => `<li>${icon(['lock', 'camera', 'lock', 'farm', 'backup', 'settings'][n - 1])}<span>${T('sec' + n)}</span></li>`).join('')}</ul><div class="mini" id="pinDemo" data-cur="lock"><p class="mini-cap">${T('pinDemo')}</p>${screen('lock')}</div>`; bindDevice($('#pinDemo'), id => { if (id === 'home') { $('#pinDemo').innerHTML = `<p class="mini-cap">${T('unlocked')}</p>` + screen('security'); } }, () => true); }
  if (usp === 2) { p.innerHTML = `<div><p class="hint">${T('pdfPick')}</p><div class="pickgrid two">${[1, 2, 3, 4, 5, 6, 7].map(i => `<button class="${i === report ? 'on' : ''}" data-rep="${i}">${icon('report')}<span>${u['r' + i]}</span></button>`).join('')}</div><div class="m-two" style="margin-top:14px">${['share', 'email', 'print'].map(l => `<button class="m-btn ton" data-act="share">${u[l]}</button>`).join('')}</div></div><div class="mini paper">${block(['pdf']).replace(u.r2, u['r' + report])}</div>`; }
  if (usp === 3) p.innerHTML = `<div><p class="lead">${T('u4b')}</p><p class="hint">${picks.size} ${T('selected')}</p><a class="cta" href="#choose">${T('ctaChoose')} →</a></div><div class="mini"><div class="m-scr"><div class="m-body">${[...picks].slice(0, 6).map(id => block(['row', S[IDX[id]][2], '=' + SC(id)[0], '=' + SC(id)[1]])).join('')}</div></div></div>`;
}
function renderChooser() {
  $('#modGrid').innerHTML = MODS.map(id => `<label class="mod ${picks.has(id) ? 'on' : ''}"><input type="checkbox" value="${id}" ${picks.has(id) ? 'checked' : ''}>${icon(S[IDX[id]][2])}<span><b>${SC(id)[0]}</b><small>${SC(id)[1]}</small></span></label>`).join('');
  renderSummary();
}
function listText() { const extra = $('#customReq').value.trim(); return `${T('listHead')}:\n` + [...picks].map(id => '• ' + SC(id)[0]).join('\n') + (extra ? '\n• ' + extra : ''); }
function renderSummary() {
  $('#pickCount').textContent = picks.size;
  $('#pickList').innerHTML = picks.size ? [...picks].map(id => `<li>${icon(S[IDX[id]][2])}${SC(id)[0]}</li>`).join('') : `<li class="none">${T('empty')}</li>`;
  $('#waShare').href = 'https://wa.me/?text=' + encodeURIComponent(listText());
  store.set('picks', [...picks].join(','));
}
function togglePick(id) { picks.has(id) ? picks.delete(id) : picks.add(id); renderChooser(); if (usp === 0 || usp === 3) renderUsp(); }

/* ---------- language & theme ---------- */
function setTheme(id) {
  theme = id; store.set('theme', id); document.documentElement.dataset.theme = id;
  $('#themeSel').value = id;
  tourGo(tour.i); if (FILM[film.i] === 'themes') $('#filmScreen').innerHTML = screen('themes');
}
function setLang(l) {
  lang = l; store.set('lang', l);
  document.documentElement.lang = l; document.body.className = 'l-' + l;
  $$('[data-s]').forEach(el => { el.innerHTML = T(el.dataset.s); });
  $$('[data-sp]').forEach(el => { el.placeholder = T(el.dataset.sp); });
  $$('[data-sa]').forEach(el => { el.setAttribute('aria-label', T(el.dataset.sa)); el.title = T(el.dataset.sa); });
  $('#langSel').value = l; $$('.brand-name').forEach(el => { el.textContent = U().appName; });
  $('#themeSel').innerHTML = THEMES.map(([id], i) => `<option value="${id}">${THEME_NAMES[l][i]}</option>`).join(''); $('#themeSel').value = theme;
  document.title = U().appName + ' — ' + T('h1a').replace(/,$/, '') + ' ' + T('h1b');
  tourList(); tourGo(tour.i); renderFeatures(); renderUsp(); renderChooser(); filmShow(film.i); filmPlay(film.playing);
  $('#marquee').innerHTML = (MODS.map(id => `<span>${icon(S[IDX[id]][2])}${SC(id)[0]}</span>`).join('')).repeat(2);
  reveal();
}

/* ---------- scroll reveal & counters ---------- */
let io;
function reveal() {
  if (!('IntersectionObserver' in window)) { $$('.rv').forEach(x => x.classList.add('in')); return; }
  io = io || new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target); } }), { threshold: .12 });
  $$('.rv:not(.in)').forEach(x => io.observe(x));
}
function counters() {
  $$('[data-count]').forEach(el => { const n = +el.dataset.count; let v = 0; const step = () => { v = Math.min(n, v + Math.ceil(n / 24)); el.textContent = v; if (v < n) requestAnimationFrame(step); }; step(); });
}

/* ---------- wire up ---------- */
document.documentElement.dataset.theme = theme;
$('#langSel').addEventListener('change', e => setLang(e.target.value));
$('#themeSel').addEventListener('change', e => setTheme(e.target.value));
$('#filmPlay').addEventListener('click', () => filmPlay(!film.playing));
$('#filmPrev').addEventListener('click', () => filmShow(film.i - 1));
$('#filmNext').addEventListener('click', () => filmShow(film.i + 1));
$('#filmReplay').addEventListener('click', () => { filmPlay(true); filmShow(0); });
$('#filmFull').addEventListener('click', () => { const f = $('#film'); document.fullscreenElement ? document.exitFullscreen() : f.requestFullscreen?.(); });
$('#filmDots').innerHTML = FILM.map((_, i) => `<i data-i="${i}"></i>`).join('');
$('#filmDots').addEventListener('click', e => { const i = e.target.dataset.i; if (i != null) filmShow(+i); });
document.addEventListener('visibilitychange', () => { if (document.hidden) clearTimeout(film.timer); else filmArm(); });
bindDevice($('#device'), tourGo, () => true);
$('#tourList').addEventListener('click', e => { const b = e.target.closest('button'); if (b) tourGo(b.dataset.id); });
$('#tourSelect').addEventListener('change', e => tourGo(e.target.value));
$('#stPrev').addEventListener('click', () => tourGo(tour.i - 1));
$('#stNext').addEventListener('click', () => tourGo(tour.i + 1));
$('#viewSeg').addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; tour.view = b.dataset.v; $$('#viewSeg button').forEach(x => x.classList.toggle('on', x === b)); tourGo(tour.i); });
$('#autoTg').addEventListener('click', e => { tour.auto = !tour.auto; e.currentTarget.classList.toggle('on', tour.auto); clearTimeout(tour.timer); if (tour.auto) tour.timer = setTimeout(() => tourGo(tour.i + 1), 6000); });
document.addEventListener('keydown', e => { if (e.target.closest('input,textarea,select')) return; const r = $('#tour').getBoundingClientRect(); if (r.top < innerHeight / 2 && r.bottom > innerHeight / 2) { if (e.key === 'ArrowRight') tourGo(tour.i + 1); if (e.key === 'ArrowLeft') tourGo(tour.i - 1); } });
$('#featGrid').addEventListener('click', e => { const b = e.target.closest('[data-id]'); if (!b) return; tourGo(b.dataset.id); $('#tour').scrollIntoView({ behavior: 'smooth' }); });
$('#uspTabs').addEventListener('click', e => { const b = e.target.closest('button'); if (!b) return; usp = +b.dataset.u; renderUsp(); });
$('#uspPanel').addEventListener('click', e => { const p = e.target.closest('[data-pick]'), r = e.target.closest('[data-rep]'), s = e.target.closest('[data-act=share]'); if (p) togglePick(p.dataset.pick); if (r) { report = +r.dataset.rep; renderUsp(); } if (s && !e.target.closest('#pinDemo')) toast(T('toastShare')); });
$('#modGrid').addEventListener('change', e => { if (e.target.value) togglePick(e.target.value); });
$('#customReq').value = store.get('custom', '');
$('#customReq').addEventListener('input', e => { store.set('custom', e.target.value); renderSummary(); });
$('#copyList').addEventListener('click', async () => { try { await navigator.clipboard.writeText(listText()); } catch { const ta = Object.assign(document.createElement('textarea'), { value: listText() }); document.body.append(ta); ta.select(); document.execCommand('copy'); ta.remove(); } toast(T('copied') + ' ✓'); });
$('#menuBtn').addEventListener('click', () => document.body.classList.toggle('nav-open'));
$$('.nav a').forEach(a => a.addEventListener('click', () => document.body.classList.remove('nav-open')));
setLang(lang); counters();
})();
