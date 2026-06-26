'use strict';
const API = '/api';
let token = localStorage.getItem('admin_token') || null;
let me = JSON.parse(localStorage.getItem('admin_me') || 'null');

const $ = s => document.querySelector(s);
const el = (t, c, h) => { const e = document.createElement(t); if (c) e.className = c; if (h != null) e.innerHTML = h; return e; };
const esc = s => (s == null ? '' : String(s).replace(/[&<>"]/g, m => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[m])));
const fmtDate = s => { if (!s) return '—'; const d = new Date(s); return isNaN(d) ? '—' : d.toLocaleString('pt-PT'); };

// ---------- Ícones (SVG, estilo profissional) ----------
const SVG = {
  dashboard:'<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>',
  users:'<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  film:'<rect x="2" y="2" width="20" height="20" rx="2.5"/><path d="M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5"/>',
  radio:'<circle cx="12" cy="12" r="2"/><path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48 0a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"/>',
  tag:'<path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><circle cx="7" cy="7" r="1.2"/>',
  file:'<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>',
  upload:'<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="M17 8l-5-5-5 5M12 3v12"/>',
  edit:'<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4z"/>',
  trash:'<path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>',
  plus:'<path d="M12 5v14M5 12h14"/>',
  search:'<circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>',
  logout:'<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
  refresh:'<path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>',
  activity:'<path d="M22 12h-4l-3 9L9 3l-3 9H2"/>',
  shield:'<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  user:'<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  lock:'<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
  settings:'<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-2.82 1.17V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>',
  x:'<path d="M18 6L6 18M6 6l12 12"/>',
  chart:'<path d="M12 20V10M18 20V4M6 20v-4"/>',
  database:'<ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>',
  archive:'<rect x="2" y="4" width="20" height="5" rx="1"/><path d="M4 9v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V9M10 13h4"/>',
  chevL:'<path d="M15 18l-6-6 6-6"/>',
  chevR:'<path d="M9 18l6-6-6-6"/>',
  sun:'<circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>',
  moon:'<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>'
};
function icon(n){ return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${SVG[n] || ''}</svg>`; }
function injectIcons(root) { (root || document).querySelectorAll('[data-icon]').forEach(e => { e.innerHTML = icon(e.dataset.icon); }); }
function iconBtn(name, cls, title) { const b = el('button', 'btn btn-sm btn-icon ' + (cls || 'btn-ghost'), icon(name)); b.title = title || ''; return b; }
injectIcons(document);

// ---------- Tema (claro / escuro) ----------
function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('admin_theme', theme);
  const ico = theme === 'light' ? 'moon' : 'sun';
  document.querySelectorAll('.theme-toggle .ic').forEach(e => e.innerHTML = icon(ico));
}
document.querySelectorAll('.theme-toggle').forEach(b => b.onclick = () => {
  const cur = document.documentElement.getAttribute('data-theme') || 'dark';
  applyTheme(cur === 'dark' ? 'light' : 'dark');
});
applyTheme(localStorage.getItem('admin_theme') || 'dark');

// ---------- API ----------
async function api(path, method = 'GET', body) {
  const opts = { method, headers: {} };
  if (token) opts.headers['Authorization'] = 'Bearer ' + token;
  if (body !== undefined) { opts.headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(body); }
  const res = await fetch(API + path, opts);
  if (res.status === 401 || res.status === 403) {
    if ($('#app-view').style.display !== 'none') { toast('Sessão expirada ou sem permissão'); logout(); }
    throw new Error('unauth');
  }
  if (!res.ok) { let msg = 'Erro ' + res.status; try { const j = await res.json(); msg = j.mensagem || msg; } catch (e) {} throw new Error(msg); }
  if (res.status === 204) return null;
  const ct = res.headers.get('content-type') || '';
  return ct.includes('application/json') ? res.json() : null;
}

// ---------- Toast / Modal ----------
let toastT;
function toast(msg) { const t = $('#toast'); t.textContent = msg; t.classList.add('show'); clearTimeout(toastT); toastT = setTimeout(() => t.classList.remove('show'), 2600); }
function openModal(title, bodyEl, wide) { $('#modal-title').textContent = title; const b = $('#modal-body'); b.innerHTML = ''; b.appendChild(bodyEl); document.querySelector('#modal .modal').classList.toggle('wide', !!wide); $('#modal').style.display = 'flex'; }
function closeModal() { $('#modal').style.display = 'none'; $('#modal-body').innerHTML = ''; }
$('#modal-close').onclick = closeModal;
$('#modal').onclick = e => { if (e.target.id === 'modal') closeModal(); };

// ---------- Login ----------
$('#login-form').onsubmit = async e => {
  e.preventDefault();
  const err = $('#login-error'); err.style.display = 'none';
  $('#login-btn').disabled = true; $('#login-btn').textContent = 'A entrar...';
  try {
    const r = await fetch(API + '/auth/login', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: $('#login-email').value.trim(), password: $('#login-password').value })
    });
    if (!r.ok) throw new Error('Email ou palavra-passe incorretos');
    const data = await r.json();
    const role = data.utilizador && data.utilizador.role;
    if (role !== 'ADMIN') { throw new Error('Não tem autorização. Este painel é exclusivo para administradores.'); }
    token = data.token; me = data.utilizador;
    localStorage.setItem('admin_token', token);
    localStorage.setItem('admin_me', JSON.stringify(me));
    enterApp();
  } catch (ex) {
    err.textContent = ex.message; err.style.display = 'block';
  } finally {
    $('#login-btn').disabled = false; $('#login-btn').textContent = 'Entrar';
  }
};

function logout() {
  token = null; me = null;
  localStorage.removeItem('admin_token'); localStorage.removeItem('admin_me');
  $('#app-view').style.display = 'none'; $('#login-view').style.display = 'flex';
}
$('#logout-btn').onclick = logout;

// ---------- O meu perfil (admin) ----------
$('#profile-btn').onclick = () => {
  const f = el('div');
  f.innerHTML =
    `<label>Nome</label><input id="pf-nome" value="${esc(me.nome)}">` +
    `<label>Email</label><input id="pf-email" type="email" value="${esc(me.email)}">` +
    `<label>Nova palavra-passe</label><input id="pf-pass" type="password" placeholder="••••••••">` +
    `<div class="hint">Preenche a senha apenas se a quiseres alterar. Ao mudar o email terás de iniciar sessão novamente.</div>`;
  const save = el('button', 'btn btn-primary btn-block', 'Guardar perfil');
  save.onclick = async () => {
    const body = { nome: $('#pf-nome').value.trim(), email: $('#pf-email').value.trim() };
    const pass = $('#pf-pass').value;
    if (pass) { if (pass.length < 6) { toast('A senha deve ter pelo menos 6 caracteres'); return; } body.password = pass; }
    if (!body.nome || !body.email) { toast('Nome e email são obrigatórios'); return; }
    const emailMudou = body.email.toLowerCase() !== (me.email || '').toLowerCase();
    try {
      const upd = await api(`/utilizadores/${me.id}`, 'PUT', body);
      me = upd; localStorage.setItem('admin_me', JSON.stringify(me));
      $('#me-name').textContent = me.nome; $('#me-avatar').textContent = (me.nome || 'A').charAt(0).toUpperCase();
      closeModal();
      if (emailMudou) { toast('Email alterado. Inicia sessão novamente.'); setTimeout(logout, 1600); }
      else toast('Perfil atualizado');
    } catch (e) { toast(e.message); }
  };
  f.appendChild(save); openModal('O meu perfil', f);
};

// ---------- Upload de documentário (admin) ----------
$('#upload-btn').onclick = async () => {
  let cats = [];
  try { cats = await api('/categorias'); } catch (e) {}
  const f = el('div');
  f.innerHTML =
    `<label>Ficheiro de vídeo</label><input id="up-file" type="file" accept="video/*">` +
    `<label>Título</label><input id="up-titulo" placeholder="Título do documentário">` +
    `<label>Descrição</label><textarea id="up-desc" rows="2" placeholder="Opcional"></textarea>` +
    `<label>Ano</label><input id="up-ano" type="number" placeholder="2024">` +
    `<label>Categoria</label><select id="up-cat"><option value="">— Sem categoria —</option>${cats.map(c => `<option value="${c.id}">${esc(c.nome)}</option>`).join('')}</select>` +
    `<div id="up-progress" class="hint" style="margin-top:12px"></div>`;
  const save = el('button', 'btn btn-primary btn-block', 'Carregar documentário');
  save.onclick = () => doUpload(save);
  f.appendChild(save); openModal('Carregar documentário', f);
};
async function doUpload(btn) {
  const file = $('#up-file').files[0];
  const titulo = $('#up-titulo').value.trim();
  if (!file) { toast('Escolhe um ficheiro de vídeo'); return; }
  if (!titulo) { toast('O título é obrigatório'); return; }
  const fd = new FormData();
  fd.append('ficheiro', file);
  fd.append('titulo', titulo);
  fd.append('descricao', $('#up-desc').value.trim());
  if ($('#up-ano').value) fd.append('ano', $('#up-ano').value);
  if ($('#up-cat').value) fd.append('categoriaId', $('#up-cat').value);
  btn.disabled = true; btn.textContent = 'A carregar...';
  $('#up-progress').textContent = 'A enviar o ficheiro (pode demorar)...';
  try {
    const res = await fetch(API + '/documentarios/upload', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }, body: fd });
    if (!res.ok) { let m = 'Erro ' + res.status; try { const j = await res.json(); m = j.mensagem || m; } catch (e) {} throw new Error(m); }
    closeModal(); toast('Documentário enviado! A processar em background.'); show('docs');
  } catch (e) { $('#up-progress').textContent = e.message; btn.disabled = false; btn.textContent = 'Carregar documentário'; }
}

function enterApp() {
  $('#login-view').style.display = 'none'; $('#app-view').style.display = 'flex';
  $('#me-name').textContent = me ? me.nome : 'Admin';
  $('#me-avatar').textContent = me && me.nome ? me.nome.charAt(0).toUpperCase() : 'A';
  show('dashboard');
}

// ---------- Navegação ----------
const TITLES = { dashboard: 'Dashboard', users: 'Utilizadores', docs: 'Documentários', lives: 'Lives ao vivo', categories: 'Categorias', certs: 'Certificados', logs: 'Logs' };
document.querySelectorAll('.nav-item').forEach(n => n.onclick = () => show(n.dataset.section));
let current = 'dashboard';
function show(sec) {
  current = sec;
  document.querySelectorAll('.nav-item').forEach(n => n.classList.toggle('active', n.dataset.section === sec));
  document.querySelectorAll('.section').forEach(s => s.style.display = 'none');
  $('#section-' + sec).style.display = 'block';
  $('#section-title').textContent = TITLES[sec];
  load(sec);
}
$('#refresh-btn').onclick = () => load(current);
function load(sec) {
  if (sec === 'dashboard') loadDashboard();
  else if (sec === 'users') loadUsers(0);
  else if (sec === 'docs') loadDocs(0);
  else if (sec === 'lives') loadLives();
  else if (sec === 'categories') loadCategories();
  else if (sec === 'certs') loadCerts();
  else if (sec === 'logs') loadLogs(0);
}

// ---------- Certificados (PKI / mTLS) ----------
async function loadCerts() {
  const t = $('#certs-table'); t.innerHTML = '<tr><td class="empty">A carregar...</td></tr>';
  try {
    const list = await api('/certificados');
    if (!list.length) { t.innerHTML = '<tr><td class="empty">Nenhum certificado registado ainda. Emite um com <code>pki/emitir_dispositivo.sh</code> e regista-o aqui (POST /api/certificados).</td></tr>'; return; }
    t.innerHTML = '<tr><th>Dispositivo</th><th>Dono</th><th>Serial</th><th>Validade</th><th>Última utilização</th><th>Estado</th><th></th></tr>';
    list.forEach(c => {
      const r = el('tr');
      const badge = c.estado === 'ATIVO' ? '<span class="badge ok">Ativo</span>'
        : c.estado === 'REVOGADO' ? '<span class="badge off">Revogado</span>'
        : '<span class="badge PROCESSANDO">Expirado</span>';
      r.innerHTML = `<td><b>${esc(c.deviceId)}</b></td><td>${esc(c.dono || '—')}</td>`
        + `<td style="font-family:monospace;font-size:11px">${esc((c.serial || '').slice(0, 16))}…</td>`
        + `<td>${fmtDate(c.validadeAte)}</td><td>${fmtDate(c.ultimaUtilizacao)}</td><td>${badge}</td>`;
      const td = el('td');
      if (c.estado === 'ATIVO') { const b = el('button', 'btn btn-danger btn-sm', 'Revogar'); b.onclick = () => revokeCert(c); td.appendChild(b); }
      r.appendChild(td); t.appendChild(r);
    });
  } catch (e) { t.innerHTML = `<tr><td class="empty">${esc(e.message)}</td></tr>`; }
}
function revokeCert(c) {
  confirmModal(`Revogar o certificado de "${c.deviceId}"? O dispositivo deixará de se poder ligar.`, async () => {
    try { await api(`/certificados/${c.id}/revogar`, 'POST'); toast('Certificado revogado'); loadCerts(); }
    catch (e) { toast(e.message); }
  });
}
$('#new-cert-btn').onclick = () => {
  const f = el('div');
  f.innerHTML =
    `<label>ID do dispositivo (CN)</label><input id="ct-dev" placeholder="dispositivo-001">` +
    `<label>Dono (nome ou email)</label><input id="ct-dono" placeholder="joao@exemplo.com">` +
    `<label>Número de série</label><input id="ct-serial" placeholder="41567340699331897A2F...">` +
    `<label>Fingerprint SHA-256</label><input id="ct-fp" placeholder="78:61:21:C2:..."><div class="hint">Estes dados aparecem no fim do script <code>emitir_dispositivo.sh</code>.</div>`;
  const save = el('button', 'btn btn-primary btn-block', 'Registar certificado');
  save.onclick = async () => {
    const body = { deviceId: $('#ct-dev').value.trim(), dono: $('#ct-dono').value.trim(), serial: $('#ct-serial').value.trim(), fingerprint: $('#ct-fp').value.trim() };
    if (!body.deviceId || !body.serial) { toast('ID do dispositivo e série são obrigatórios'); return; }
    try { await api('/certificados', 'POST', body); closeModal(); toast('Certificado registado'); loadCerts(); }
    catch (e) { toast(e.message); }
  };
  f.appendChild(save); openModal('Registar certificado de dispositivo', f);
};
$('#download-ca-btn').onclick = async () => {
  try {
    const res = await fetch(API + '/certificados/ca', { headers: { 'Authorization': 'Bearer ' + token } });
    if (!res.ok) throw new Error('Erro ' + res.status + ' (a CA existe em pki/ca/ca.crt?)');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'arquivo-digital-ca.crt'; a.click();
    URL.revokeObjectURL(url);
    toast('CA descarregada — instala nos dispositivos');
  } catch (e) { toast(e.message); }
};

// ---------- Dashboard ----------
async function loadDashboard() {
  const grid = $('#stats-grid'); grid.innerHTML = '<div class="empty">A carregar...</div>';
  try {
    const [users, docs, cats, logs] = await Promise.all([
      api('/utilizadores?pagina=0&tamanho=1'),
      api('/documentarios/admin/todos?pagina=0&tamanho=1'),
      api('/categorias'),
      api('/logs?pagina=0&tamanho=1')
    ]);
    grid.innerHTML = '';
    grid.appendChild(stat('users', users.totalElementos, 'Utilizadores'));
    grid.appendChild(stat('film', docs.totalElementos, 'Documentários'));
    grid.appendChild(stat('tag', cats.length, 'Categorias'));
    grid.appendChild(stat('file', logs.totalElementos, 'Registos de log'));
  } catch (e) { grid.innerHTML = '<div class="empty">Erro ao carregar.</div>'; }

  renderEstatisticas();

  const tv = $('#topvistos'); tv.innerHTML = '';
  try {
    const top = await api('/documentarios/mais-vistos');
    if (!top.length) { tv.innerHTML = '<div class="empty">Sem dados.</div>'; return; }
    const t = el('table');
    t.innerHTML = '<tr><th>#</th><th>Título</th><th>Categoria</th><th>Estrelas</th><th>Visualizações</th></tr>';
    top.forEach((d, i) => {
      const estrelas = (d.mediaEstrelas != null ? d.mediaEstrelas : 0).toFixed(1);
      const r = el('tr');
      r.innerHTML = `<td>${i+1}</td><td>${esc(d.titulo)}</td><td>${esc(d.categoria ? d.categoria.nome : '—')}</td><td>★ ${estrelas}</td><td>${d.visualizacoes||0}</td>`;
      t.appendChild(r);
    });
    tv.appendChild(t);
  } catch (e) { tv.innerHTML = '<div class="empty">Erro.</div>'; }
}
function stat(name, v, l) { return el('div', 'stat', `<span class="ic">${icon(name)}</span><div class="v">${v}</div><div class="l">${esc(l)}</div>`); }

async function renderEstatisticas() {
  const box = $('#dash-extra'); box.innerHTML = '';
  try {
    const s = await api('/documentarios/admin/estatisticas');
    // Cartões de compressão
    const grid = el('div', 'stats-grid');
    grid.appendChild(el('div', 'stat hero', `<span class="ic">${icon('database')}</span><div class="v">${esc(s.totalPoupadoLegivel)}</div><div class="l">Espaço poupado pela compressão</div>`));
    grid.appendChild(stat('archive', esc(s.totalOriginalLegivel), 'Tamanho original total'));
    grid.appendChild(stat('film', esc(s.totalComprimidoLegivel), 'Tamanho comprimido total'));
    grid.appendChild(stat('chart', esc(s.taxaMediaCompressao), 'Taxa média de compressão'));
    box.appendChild(grid);
    // Gráficos
    const charts = el('div', 'charts');
    charts.appendChild(chartCard('Documentários por estado', s.porEstado));
    charts.appendChild(chartCard('Documentários por categoria', s.porCategoria));
    box.appendChild(charts);
  } catch (e) { /* silencioso */ }
}
function chartCard(title, mapObj) {
  const card = el('div', 'card');
  const entries = Object.entries(mapObj || {});
  const max = Math.max(1, ...entries.map(e => e[1]));
  let html = `<h3>${esc(title)}</h3>`;
  if (!entries.length) html += '<div class="empty">Sem dados.</div>';
  entries.forEach(([k, v]) => {
    html += `<div class="bar-row"><div class="bar-label"><b>${esc(k)}</b><span>${v}</span></div>` +
            `<div class="bar-track"><div class="bar-fill" style="width:${(v/max*100).toFixed(0)}%"></div></div></div>`;
  });
  card.innerHTML = html; return card;
}

// ---------- Utilizadores ----------
let usersData = [];
async function loadUsers(page) {
  const t = $('#users-table'); t.innerHTML = '<tr><td class="empty">A carregar...</td></tr>';
  try {
    const p = await api(`/utilizadores?pagina=${page}&tamanho=50`);
    usersData = p.conteudo;
    renderUsers();
    pager($('#users-pager'), p, loadUsers);
  } catch (e) { t.innerHTML = `<tr><td class="empty">${esc(e.message)}</td></tr>`; }
}
function renderUsers() {
  const q = ($('#users-search').value || '').toLowerCase();
  const t = $('#users-table');
  t.innerHTML = '<tr><th>Nome</th><th>Email</th><th>Role</th><th>Estado</th><th>Criado</th><th>Ações</th></tr>';
  const rows = usersData.filter(u => !q || (u.nome + ' ' + u.email).toLowerCase().includes(q));
  if (!rows.length) { t.innerHTML += '<tr><td class="empty" colspan="6">Nenhum resultado.</td></tr>'; return; }
  rows.forEach(u => {
    const r = el('tr');
    r.innerHTML =
      `<td>${esc(u.nome)}</td><td>${esc(u.email)}</td>` +
      `<td><span class="badge ${u.role==='ADMIN'?'admin':'user'}">${u.role}</span></td>` +
      `<td><span class="badge ${u.ativo?'ok':'off'}">${u.ativo?'Ativo':'Inativo'}</span></td>` +
      `<td>${fmtDate(u.dataCriacao)}</td>`;
    const td = el('td'); const wrap = el('div', 'row-actions');
    const a = el('button', 'btn btn-ghost btn-sm', 'Atividade'); a.onclick = () => userActivity(u);
    const e = el('button', 'btn btn-ghost btn-sm', 'Editar'); e.onclick = () => editUser(u);
    const d = el('button', 'btn btn-danger btn-sm', 'Eliminar'); d.onclick = () => delUser(u);
    wrap.append(a, e, d); td.appendChild(wrap); r.appendChild(td); t.appendChild(r);
  });
}
$('#users-search').oninput = () => renderUsers();
$('#new-user-btn').onclick = () => {
  const f = el('div');
  f.innerHTML =
    `<label>Nome</label><input id="nu-nome" placeholder="Nome completo">` +
    `<label>Email</label><input id="nu-email" type="email" placeholder="email@exemplo.com">` +
    `<label>Palavra-passe</label><input id="nu-pass" type="password" placeholder="mín. 6 caracteres">` +
    `<label>Role</label><select id="nu-role"><option value="USER">USER</option><option value="ADMIN">ADMIN</option></select>`;
  const save = el('button', 'btn btn-primary btn-block', 'Criar');
  save.onclick = async () => {
    const body = { nome: $('#nu-nome').value.trim(), email: $('#nu-email').value.trim(), password: $('#nu-pass').value, role: $('#nu-role').value };
    if (!body.nome || !body.email || body.password.length < 6) { toast('Preenche os campos (password ≥ 6)'); return; }
    try { await api('/utilizadores', 'POST', body); closeModal(); toast('Utilizador criado'); loadUsers(0); }
    catch (e) { toast(e.message); }
  };
  f.appendChild(save); openModal('Novo utilizador', f);
};
function editUser(u) {
  const f = el('div');
  f.innerHTML =
    `<label>Nome</label><input id="eu-nome" value="${esc(u.nome)}">` +
    `<label>Email</label><input id="eu-email" type="email" value="${esc(u.email)}">` +
    `<label>Nova palavra-passe</label><input id="eu-pass" type="password" placeholder="••••••••"><div class="hint">Preenche apenas para alterar a senha (mín. 6 caracteres).</div>` +
    `<label>Role</label><select id="eu-role"><option value="USER"${u.role==='USER'?' selected':''}>USER</option><option value="ADMIN"${u.role==='ADMIN'?' selected':''}>ADMIN</option></select>` +
    `<label>Estado</label><select id="eu-ativo"><option value="true"${u.ativo?' selected':''}>Ativo</option><option value="false"${!u.ativo?' selected':''}>Inativo</option></select>`;
  const save = el('button', 'btn btn-primary btn-block', 'Guardar alterações');
  save.onclick = async () => {
    const body = { nome: $('#eu-nome').value.trim(), email: $('#eu-email').value.trim(), role: $('#eu-role').value, ativo: $('#eu-ativo').value === 'true' };
    const pass = $('#eu-pass').value;
    if (pass) { if (pass.length < 6) { toast('A senha deve ter pelo menos 6 caracteres'); return; } body.password = pass; }
    if (!body.nome || !body.email) { toast('Nome e email são obrigatórios'); return; }
    try {
      await api(`/utilizadores/${u.id}`, 'PUT', body);
      closeModal(); toast('Utilizador atualizado'); loadUsers(0);
    } catch (e) { toast(e.message); }
  };
  f.appendChild(save); openModal('Editar utilizador', f);
}
function delUser(u) {
  if (u.id === (me && me.id)) { toast('Não podes eliminar a tua própria conta'); return; }
  confirmModal(`Eliminar o utilizador "${u.nome}"? Esta ação é irreversível.`, async () => {
    try { await api(`/utilizadores/${u.id}`, 'DELETE'); toast('Utilizador eliminado'); loadUsers(0); }
    catch (e) { toast(e.message); }
  });
}

// ---------- Atividade do utilizador ----------
async function userActivity(u) {
  const body = el('div', '', '<div class="empty">A carregar...</div>');
  openModal('Atividade — ' + u.nome, body, true);
  try {
    const [act, logs, sessoes] = await Promise.all([
      api(`/utilizadores/${u.id}/atividade`),
      api(`/logs/utilizador/${u.id}?pagina=0&tamanho=50`),
      api(`/utilizadores/${u.id}/sessoes`)
    ]);
    body.innerHTML = '';
    body.appendChild(actSec('Ações no sistema (uploads, edições, eliminações, logins...)',
      (logs.conteudo || []).map(l => actRow(`<span class="badge user">${esc(l.acao)}</span> ${esc(l.detalhe || '')}`, l.timestamp))));
    body.appendChild(actSec('Likes e Dislikes',
      act.avaliacoes.map(a => actRow(`<span class="badge ${a.valor===1?'ok':'off'}">${a.valor === 1 ? 'Like' : 'Dislike'}</span> ${esc(a.titulo)}`, a.data))));
    body.appendChild(actSec('Minha Lista (ver mais tarde)',
      act.lista.map(a => actRow(esc(a.titulo), a.data))));
    body.appendChild(actSec('Histórico (já assistidos)',
      act.historico.map(a => actRow(esc(a.titulo), a.data))));
    // Sessões + revogar
    const sessSec = actSec('Sessões',
      (sessoes || []).map(s => actRow(`<span class="dot" style="background:${s.ativa ? '#22c55e' : '#64748b'}"></span> ${esc(s.ipOrigem || 'IP?')} <span style="color:var(--muted)">${esc((s.userAgent || '').slice(0, 34))}</span>`, s.dataCriacao)));
    if ((sessoes || []).some(s => s.ativa)) {
      const rev = el('button', 'btn btn-danger btn-sm', 'Revogar todas as sessões');
      rev.style.marginTop = '8px';
      rev.onclick = () => confirmModal(`Revogar todas as sessões de ${u.nome}? Terá de iniciar sessão de novo.`, async () => {
        try { await api(`/utilizadores/${u.id}/sessoes`, 'DELETE'); toast('Sessões revogadas'); userActivity(u); } catch (e) { toast(e.message); }
      });
      sessSec.appendChild(rev);
    }
    body.appendChild(sessSec);
  } catch (e) { body.innerHTML = `<div class="empty">${esc(e.message)}</div>`; }
}
const actRow = (txt, date) => `<div class="act-row"><span>${txt}</span><span class="act-date">${fmtDate(date)}</span></div>`;
function actSec(title, rows) {
  const s = el('div', 'act-sec');
  s.innerHTML = `<h4>${title} <span class="act-count">${rows.length}</span></h4>` +
    (rows.length ? rows.join('') : '<div class="act-empty">Nada registado.</div>');
  return s;
}

// ---------- Documentários ----------
let docsData = [];
async function loadDocs(page) {
  const t = $('#docs-table'); t.innerHTML = '<tr><td class="empty">A carregar...</td></tr>';
  try {
    const p = await api(`/documentarios/admin/todos?pagina=${page}&tamanho=50`);
    docsData = p.conteudo;
    renderDocs();
    pager($('#docs-pager'), p, loadDocs);
  } catch (e) { t.innerHTML = `<tr><td class="empty">${esc(e.message)}</td></tr>`; }
}
function renderDocs() {
  const q = ($('#docs-search').value || '').toLowerCase();
  const f = $('#docs-filter').value;
  const t = $('#docs-table');
  t.innerHTML = '<tr><th></th><th>Título</th><th>Estado</th><th>Categoria</th><th>Autor</th><th>Visto</th><th>Ações</th></tr>';
  const rows = docsData.filter(d => (!q || (d.titulo || '').toLowerCase().includes(q)) && (!f || d.status === f));
  if (!rows.length) { t.innerHTML += '<tr><td class="empty" colspan="7">Nenhum resultado.</td></tr>'; return; }
  rows.forEach(d => {
    const r = el('tr');
    const thumb = d.urlThumbnail ? `<img class="thumb" src="${esc(d.urlThumbnail)}">` : `<div class="thumb"></div>`;
    r.innerHTML =
      `<td>${thumb}</td><td>${esc(d.titulo)}</td>` +
      `<td><span class="badge ${d.status}">${d.status}</span></td>` +
      `<td>${esc(d.categoria ? d.categoria.nome : '—')}</td>` +
      `<td>${esc(d.nomeUtilizador || '—')}</td><td>${d.visualizacoes||0}</td>`;
    const td = el('td'); const wrap = el('div', 'row-actions');
    if (d.status === 'PRONTO') { const w = el('button', 'btn btn-ghost btn-sm', 'Assistir'); w.onclick = () => watchDoc(d); wrap.appendChild(w); }
    if (d.status === 'PRONTO') { const rep = el('button', 'btn btn-ghost btn-sm', 'Relatório'); rep.onclick = () => report(d); wrap.appendChild(rep); }
    if (d.status === 'PRONTO' || d.status === 'ERRO') { const re = el('button', 'btn btn-ghost btn-sm', 'Reprocessar'); re.onclick = () => reprocess(d); wrap.appendChild(re); }
    const del = el('button', 'btn btn-danger btn-sm', 'Eliminar'); del.onclick = () => delDoc(d);
    wrap.appendChild(del); td.appendChild(wrap); r.appendChild(td); t.appendChild(r);
  });
}
$('#docs-search').oninput = () => renderDocs();
$('#docs-filter').onchange = () => renderDocs();
function reprocess(d) {
  confirmModal(`Reprocessar "${d.titulo}"? Vai recomprimir o vídeo e regenerar capa/legendas.`, async () => {
    try { await api(`/documentarios/${d.id}/recomprimir`, 'POST'); toast('Reprocessamento iniciado em background'); setTimeout(() => loadDocs(0), 1000); }
    catch (e) { toast(e.message); }
  });
}
function watchDoc(d) {
  const f = el('div');
  const sub = d.urlLegendas ? `<track kind="subtitles" srclang="pt" label="Português" src="${esc(d.urlLegendas)}" default>` : '';
  f.innerHTML =
    `<video controls autoplay playsinline style="width:100%;border-radius:10px;background:#000;max-height:62vh">` +
    `<source src="${esc(d.urlStreaming)}" type="video/mp4">${sub}O teu navegador não suporta vídeo.</video>` +
    (d.descricao ? `<p class="hint" style="margin-top:12px;line-height:1.5">${esc(d.descricao)}</p>` : '');
  openModal(d.titulo, f, true);
}

// ---------- Lives ativas ----------
async function loadLives() {
  const t = $('#lives-table'); t.innerHTML = '<tr><td class="empty">A carregar...</td></tr>';
  try {
    const lives = await api('/lives');
    t.innerHTML = '<tr><th>Título</th><th>Transmissor</th><th>Espectadores</th><th>Iniciada</th></tr>';
    if (!lives.length) { t.innerHTML += '<tr><td class="empty" colspan="4">Nenhuma transmissão ao vivo neste momento.</td></tr>'; return; }
    lives.forEach(l => {
      const r = el('tr');
      r.innerHTML = `<td><span class="badge live"><span class="dot" style="background:#fca5a5"></span> AO VIVO</span> &nbsp;${esc(l.titulo)}</td>` +
        `<td>${esc(l.nomeBroadcaster || '—')}</td><td>${l.numEspectadores || 0}</td><td>${fmtDate(l.iniciadaEm)}</td>`;
      t.appendChild(r);
    });
  } catch (e) { t.innerHTML = `<tr><td class="empty">${esc(e.message)}</td></tr>`; }
}
async function report(d) {
  try {
    const r = await api(`/documentarios/${d.id}/relatorio-compressao`);
    const b = el('div');
    b.innerHTML =
      `<div class="report-hero"><span>${esc(r.tamanhoOriginalLegivel)}</span><span class="arrow">→</span><span style="color:var(--primary)">${esc(r.tamanhoComprimidoLegivel)}</span></div>` +
      row('Taxa de compressão', r.taxaCompressao) +
      row('Espaço poupado', r.espacoPoupadoLegivel) +
      row('Qualidade percebida', r.qualidadePercebida) +
      row('Tempo de processamento', r.tempoProcessamentoLegivel) +
      row('Codec de vídeo', r.codecVideo) +
      row('Codec de áudio', r.codecAudio);
    openModal('Relatório de Compressão — ' + d.titulo, b);
  } catch (e) { toast(e.message); }
}
const row = (a, b) => `<div class="report-row"><span>${esc(a)}</span><span>${esc(b||'—')}</span></div>`;
function delDoc(d) {
  confirmModal(`Eliminar o documentário "${d.titulo}"? Os ficheiros serão apagados.`, async () => {
    try { await api(`/documentarios/${d.id}`, 'DELETE'); toast('Documentário eliminado'); loadDocs(0); }
    catch (e) { toast(e.message); }
  });
}

// ---------- Categorias ----------
async function loadCategories() {
  const t = $('#cats-table'); t.innerHTML = '<tr><td class="empty">A carregar...</td></tr>';
  try {
    const cats = await api('/categorias');
    t.innerHTML = '<tr><th>ID</th><th>Nome</th><th>Descrição</th><th>Ações</th></tr>';
    cats.forEach(c => {
      const r = el('tr');
      r.innerHTML = `<td>${c.id}</td><td>${esc(c.nome)}</td><td>${esc(c.descricao || '—')}</td>`;
      const td = el('td'); const wrap = el('div', 'row-actions');
      const e = el('button', 'btn btn-ghost btn-sm', 'Editar'); e.onclick = () => catModal(c);
      const d = el('button', 'btn btn-danger btn-sm', 'Eliminar'); d.onclick = () => delCat(c);
      wrap.append(e, d); td.appendChild(wrap); r.appendChild(td); t.appendChild(r);
    });
  } catch (e) { t.innerHTML = `<tr><td class="empty">${esc(e.message)}</td></tr>`; }
}
$('#new-cat-btn').onclick = () => catModal(null);
function catModal(c) {
  const f = el('div');
  f.innerHTML =
    `<label>Nome</label><input id="c-nome" value="${c ? esc(c.nome) : ''}" placeholder="Ex: História">` +
    `<label>Descrição</label><textarea id="c-desc" rows="3" placeholder="Descrição opcional">${c ? esc(c.descricao || '') : ''}</textarea>`;
  const save = el('button', 'btn btn-primary btn-block', c ? 'Guardar' : 'Criar');
  save.onclick = async () => {
    const body = { nome: $('#c-nome').value.trim(), descricao: $('#c-desc').value.trim() };
    if (!body.nome) { toast('O nome é obrigatório'); return; }
    try {
      if (c) await api('/categorias/' + c.id, 'PUT', body);
      else await api('/categorias', 'POST', body);
      closeModal(); toast(c ? 'Categoria atualizada' : 'Categoria criada'); loadCategories();
    } catch (e) { toast(e.message); }
  };
  f.appendChild(save); openModal(c ? 'Editar categoria' : 'Nova categoria', f);
}
function delCat(c) {
  confirmModal(`Eliminar a categoria "${c.nome}"?`, async () => {
    try { await api('/categorias/' + c.id, 'DELETE'); toast('Categoria eliminada'); loadCategories(); }
    catch (e) { toast(e.message); }
  });
}

// ---------- Logs ----------
let logsData = [];
async function loadLogs(page) {
  const t = $('#logs-table'); t.innerHTML = '<tr><td class="empty">A carregar...</td></tr>';
  try {
    const p = await api(`/logs?pagina=${page}&tamanho=50`);
    logsData = p.conteudo;
    renderLogs();
    pager($('#logs-pager'), p, loadLogs);
  } catch (e) { t.innerHTML = `<tr><td class="empty">${esc(e.message)}</td></tr>`; }
}
function renderLogs() {
  const q = ($('#logs-search').value || '').toLowerCase();
  const t = $('#logs-table');
  t.innerHTML = '<tr><th>Data</th><th>Utilizador</th><th>Dispositivo</th><th>Ação</th><th>Detalhe</th><th>IP</th></tr>';
  const rows = logsData.filter(l => !q || ((l.nomeUtilizador || '') + ' ' + (l.acao || '') + ' ' + (l.detalhe || '') + ' ' + (l.dispositivo || '')).toLowerCase().includes(q));
  if (!rows.length) { t.innerHTML += '<tr><td class="empty" colspan="6">Sem registos.</td></tr>'; return; }
  rows.forEach(l => {
    const r = el('tr');
    const disp = l.dispositivo ? `<span class="badge admin">${esc(l.dispositivo)}</span>` : '<span style="color:var(--muted)">—</span>';
    r.innerHTML = `<td>${fmtDate(l.timestamp)}</td><td>${esc(l.nomeUtilizador || '—')}</td><td>${disp}</td><td><span class="badge user">${esc(l.acao)}</span></td><td>${esc(l.detalhe || '—')}</td><td>${esc(l.ip || '—')}</td>`;
    t.appendChild(r);
  });
}
$('#logs-search').oninput = () => renderLogs();
$('#clear-logs-btn').onclick = () => confirmModal('Limpar TODOS os logs? Esta ação é irreversível.', async () => {
  try { await api('/logs', 'DELETE'); toast('Logs eliminados'); loadLogs(0); } catch (e) { toast(e.message); }
});

// ---------- Helpers ----------
function pager(node, p, fn) {
  node.innerHTML = '';
  if (!p || p.totalPaginas <= 1) return;
  const prev = el('button', 'btn btn-ghost btn-sm', icon('chevL') + ' Anterior'); prev.disabled = p.primeira; prev.onclick = () => fn(p.paginaActual - 1);
  const next = el('button', 'btn btn-ghost btn-sm', 'Seguinte ' + icon('chevR')); next.disabled = p.ultima; next.onclick = () => fn(p.paginaActual + 1);
  node.append(`Página ${p.paginaActual + 1} de ${p.totalPaginas}`, prev, next);
}
function confirmModal(msg, onYes) {
  const f = el('div');
  f.innerHTML = `<p style="margin-bottom:18px;line-height:1.5">${esc(msg)}</p>`;
  const wrap = el('div', 'row-actions');
  const no = el('button', 'btn btn-ghost', 'Cancelar'); no.onclick = closeModal;
  const yes = el('button', 'btn btn-danger', 'Confirmar'); yes.onclick = () => { closeModal(); onYes(); };
  wrap.append(no, yes); f.appendChild(wrap); openModal('Confirmar', f);
}

// ---------- Arranque ----------
if (token && me && me.role === 'ADMIN') enterApp();
else { $('#login-view').style.display = 'flex'; }
