/* ===========================================================================
   Steam Catalog - logica do front-end (JavaScript puro, sem frameworks).
   Conversa com a API REST exposta pelo ApiServer.
   =========================================================================== */

// ----------------------------------------------------------------- helpers API
async function api(method, path, body) {
    const opts = { method, headers: {} };
    if (body !== undefined) {
        opts.headers['Content-Type'] = 'application/json';
        opts.body = JSON.stringify(body);
    }
    const res = await fetch('/api' + path, opts);
    const text = await res.text();
    const data = text ? JSON.parse(text) : {};
    if (!res.ok) {
        throw new Error(data.error || ('Erro ' + res.status));
    }
    return data;
}

const $ = (id) => document.getElementById(id);
const esc = (s) => (s == null ? '' : String(s).replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])));

function toast(message, type = 'ok', title) {
    const t = document.createElement('div');
    t.className = 'toast ' + type;
    t.innerHTML = (title ? `<b>${esc(title)}</b>` : '') + esc(message);
    $('toasts').appendChild(t);
    setTimeout(() => { t.style.opacity = '0'; setTimeout(() => t.remove(), 300); }, 3800);
}

function money(v) {
    if (v === 0) return '<span class="price free">Gratuito</span>';
    return `<span class="price paid">US$ ${Number(v).toFixed(2)}</span>`;
}
function scoreClass(s) { return s >= 8 ? 'score-high' : s >= 5 ? 'score-mid' : 'score-low'; }

// --------------------------------------------------------------- navegacao
document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => showView(btn.dataset.view));
});

function showView(name) {
    document.querySelectorAll('.nav-btn').forEach(b =>
        b.classList.toggle('active', b.dataset.view === name));
    document.querySelectorAll('.view').forEach(v =>
        v.classList.toggle('active', v.id === 'view-' + name));
    if (name === 'developers') searchDevelopers();
    if (name === 'publishers') searchPublishers();
    if (name === 'import') refreshImportStatus();
}

// --------------------------------------------------------------- estatisticas
async function loadStats() {
    try {
        const s = await api('GET', '/stats');
        $('stat-games').textContent = s.games;
        $('stat-developers').textContent = s.developers;
        $('stat-publishers').textContent = s.publishers;
    } catch (e) { /* silencioso */ }
}

// =============================================================== GAMES
function gameFilters() {
    const params = new URLSearchParams();
    const add = (k, id) => { const v = $(id).value.trim(); if (v !== '') params.set(k, v); };
    add('name', 'f-name');
    add('type', 'f-type');
    add('minPrice', 'f-minPrice');
    add('maxPrice', 'f-maxPrice');
    add('minScore', 'f-minScore');
    add('maxRank', 'f-maxRank');
    add('tag', 'f-tag');
    add('developer', 'f-developer');
    add('publisher', 'f-publisher');
    params.set('limit', '48');
    return params.toString();
}

async function searchGames() {
    const grid = $('games-grid');
    grid.innerHTML = '<p class="empty">Carregando...</p>';
    try {
        const data = await api('GET', '/games?' + gameFilters());
        $('games-info').textContent = data.total + ' jogo(s) encontrado(s)';
        if (!data.items.length) {
            grid.innerHTML = '<p class="empty">Nenhum jogo encontrado com esses critérios.</p>';
            return;
        }
        grid.innerHTML = data.items.map(g => gameCard(g)).join('');
    } catch (e) {
        grid.innerHTML = '';
        toast(e.message, 'err', 'Falha na busca');
    }
}

function gameCard(g, fromEntity) {
    const img = g.thumbnail
        ? `<img class="card-img" loading="lazy" src="${esc(g.thumbnail)}" onerror="this.outerHTML='<div class=card-noimg>▶</div>'">`
        : `<div class="card-noimg">▶</div>`;
    const tags = (g.tags || []).slice(0, 3).map(t => `<span class="tag">${esc(t)}</span>`).join('');
    const click = fromEntity ? `openGameDetail(${g.appid}, true)` : `openGameDetail(${g.appid})`;
    return `<div class="card" onclick='${click}'>
        ${img}
        <div class="card-body">
            <div class="card-title">${esc(g.name)}</div>
            <div class="card-dev">${esc(g.developer)}</div>
            <div class="tag-row">${tags}</div>
            <div class="card-foot">
                ${money(g.price)}
                <span class="badge-score ${scoreClass(g.reviewScore)}">${g.reviewScore}/10</span>
            </div>
        </div>
    </div>`;
}

function clearGameFilters() {
    ['f-name','f-type','f-minPrice','f-maxPrice','f-minScore','f-maxRank','f-tag','f-developer','f-publisher']
        .forEach(id => $(id).value = '');
    searchGames();
}

async function openGameDetail(appid, fromEntity) {
    try {
        const g = await api('GET', '/games/' + appid);
        const back = (fromEntity && lastEntityGames)
            ? `<button class="btn btn-ghost" onclick="reopenEntityGames()">← Voltar para ${esc(lastEntityGames.name)}</button>`
            : '';
        const hero = g.thumbnail
            ? `<div class="modal-hero"><img src="${esc(g.thumbnail)}" onerror="this.style.display='none'"><button class="modal-x" onclick="closeModal()">✕</button></div>`
            : `<div class="modal-hero"><div class="card-noimg" style="border-radius:8px 8px 0 0">▶</div><button class="modal-x" onclick="closeModal()">✕</button></div>`;
        const tags = (g.tags || []).map(t => `<span class="tag">${esc(t)}</span>`).join(' ');
        $('modal-content').innerHTML = `
            ${hero}
            <div class="modal-body">
                <h2>${esc(g.name)}</h2>
                <div class="modal-meta">appid ${g.appid} · ${esc(g.type)} · lançado em ${esc(g.releaseDate || '—')}</div>
                <div class="detail-grid">
                    <div class="box"><label>Preço</label><span>${g.price === 0 ? 'Gratuito' : 'US$ ' + g.price.toFixed(2)}</span></div>
                    <div class="box"><label>Score</label><span>${g.reviewScore}/10</span></div>
                    <div class="box"><label>Rank</label><span>#${g.rank}</span></div>
                    <div class="box"><label>👍 / 👎</label><span style="font-size:13px">${g.positiveReview} / ${g.negativeReview}</span></div>
                </div>
                <div class="detail-grid">
                    <div class="box"><label>Developer</label><span style="font-size:14px">${esc(g.developer)}</span></div>
                    <div class="box"><label>Publisher</label><span style="font-size:14px">${esc(g.publisher)}</span></div>
                </div>
                <div class="tag-row" style="margin-bottom:16px">${tags}</div>
                <div class="modal-desc">${esc(g.description || 'Sem descrição.')}</div>
                <div class="modal-actions">
                    ${back}
                    <button class="btn btn-blue" onclick='openGameForm(${JSON.stringify(g)})'>Editar</button>
                    <button class="btn btn-danger" onclick="deleteGame(${g.appid})">Excluir</button>
                </div>
            </div>`;
        openModal();
    } catch (e) {
        toast(e.message, 'err');
    }
}

async function deleteGame(appid) {
    if (!confirm('Excluir o jogo ' + appid + '?')) return;
    try {
        await api('DELETE', '/games/' + appid);
        toast('Jogo excluído.', 'ok');
        closeModal(); searchGames(); loadStats();
    } catch (e) { toast(e.message, 'err', 'Não foi possível excluir'); }
}

function openGameForm(game) {
    const g = game || {};
    const editing = !!game;
    $('modal-content').innerHTML = `
        <div class="modal-body">
            <h2>${editing ? 'Editar jogo' : 'Novo jogo'}</h2>
            <div class="modal-meta">Campos com <span class="req">*</span> são obrigatórios. Developer e Publisher precisam já existir.</div>
            <div class="form-grid">
                <div class="field"><label>Appid <span class="req">*</span></label>
                    <input id="g-appid" type="number" value="${g.appid || ''}" ${editing ? 'readonly' : ''}></div>
                <div class="field"><label>Tipo <span class="req">*</span></label>
                    <input id="g-type" value="${esc(g.type || 'game')}"></div>
                <div class="field full"><label>Nome <span class="req">*</span></label>
                    <input id="g-name" value="${esc(g.name || '')}"></div>
                <div class="field"><label>Preço (US$)</label>
                    <input id="g-price" type="number" step="0.01" value="${g.price != null ? g.price : 0}"></div>
                <div class="field"><label>Data (yyyy-MM-dd)</label>
                    <input id="g-releaseDate" placeholder="2024-01-30" value="${esc(g.releaseDate || '')}"></div>
                <div class="field"><label>Review score (0-10)</label>
                    <input id="g-reviewScore" type="number" min="0" max="10" value="${g.reviewScore != null ? g.reviewScore : 0}"></div>
                <div class="field"><label>Rank</label>
                    <input id="g-rank" type="number" value="${g.rank != null ? g.rank : 0}"></div>
                <div class="field"><label>Reviews positivos</label>
                    <input id="g-positiveReview" type="number" value="${g.positiveReview != null ? g.positiveReview : 0}"></div>
                <div class="field"><label>Reviews negativos</label>
                    <input id="g-negativeReview" type="number" value="${g.negativeReview != null ? g.negativeReview : 0}"></div>
                <div class="field"><label>Developer <span class="req">*</span></label>
                    <input id="g-developer" value="${esc(g.developer || '')}"></div>
                <div class="field"><label>Publisher <span class="req">*</span></label>
                    <input id="g-publisher" value="${esc(g.publisher || '')}"></div>
                <div class="field full"><label>URL da capa (thumbnail)</label>
                    <input id="g-thumbnail" value="${esc(g.thumbnail || '')}"></div>
                <div class="field full"><label>Tags (separadas por vírgula)</label>
                    <input id="g-tags" value="${esc((g.tags || []).join(', '))}"></div>
                <div class="field full"><label>Descrição</label>
                    <textarea id="g-description">${esc(g.description || '')}</textarea></div>
            </div>
            <div class="modal-actions" style="margin-top:18px">
                <button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
                <button class="btn btn-green" onclick="saveGame(${editing})">${editing ? 'Salvar alterações' : 'Cadastrar'}</button>
            </div>
        </div>`;
    openModal();
}

async function saveGame(editing) {
    const num = (id) => { const v = $(id).value; return v === '' ? 0 : Number(v); };
    const body = {
        appid: num('g-appid'),
        name: $('g-name').value.trim(),
        type: $('g-type').value.trim(),
        price: num('g-price'),
        releaseDate: $('g-releaseDate').value.trim() || null,
        reviewScore: num('g-reviewScore'),
        rank: num('g-rank'),
        positiveReview: num('g-positiveReview'),
        negativeReview: num('g-negativeReview'),
        developer: $('g-developer').value.trim(),
        publisher: $('g-publisher').value.trim(),
        thumbnail: $('g-thumbnail').value.trim() || null,
        description: $('g-description').value.trim(),
        tags: $('g-tags').value.split(',').map(s => s.trim()).filter(Boolean)
    };
    try {
        if (editing) {
            await api('PUT', '/games/' + body.appid, body);
            toast('Jogo atualizado.', 'ok');
        } else {
            await api('POST', '/games', body);
            toast('Jogo cadastrado.', 'ok');
        }
        closeModal(); searchGames(); loadStats();
    } catch (e) { toast(e.message, 'err', 'Não foi possível salvar'); }
}

// =============================================================== DEVELOPERS / PUBLISHERS
function entityCfg(kind) {
    return kind === 'developer'
        ? { path: '/developers', table: 'developers-table', nameId: 'd-name', countryId: 'd-country', label: 'Developer' }
        : { path: '/publishers', table: 'publishers-table', nameId: 'p-name', countryId: 'p-country', label: 'Publisher' };
}

async function searchEntity(kind) {
    const cfg = entityCfg(kind);
    const params = new URLSearchParams();
    if ($(cfg.nameId).value.trim()) params.set('name', $(cfg.nameId).value.trim());
    if ($(cfg.countryId).value.trim()) params.set('country', $(cfg.countryId).value.trim());
    params.set('limit', '100');
    const tbody = document.querySelector('#' + cfg.table + ' tbody');
    tbody.innerHTML = '<tr><td colspan="3" class="muted">Carregando...</td></tr>';
    try {
        const data = await api('GET', cfg.path + '?' + params.toString());
        if (!data.items.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="muted">Nenhum registro.</td></tr>';
            return;
        }
        tbody.innerHTML = data.items.map(e => `
            <tr>
                <td><a class="entity-link" onclick='openEntityGames("${kind}", ${JSON.stringify(e.name)})'>${esc(e.name)}</a></td>
                <td>${esc(e.country || '—')}</td>
                <td class="col-actions"><div class="row-actions">
                    <button class="btn btn-green btn-sm" onclick='openEntityGames("${kind}", ${JSON.stringify(e.name)})'>Ver jogos</button>
                    <button class="btn btn-blue btn-sm" onclick='openEntityForm("${kind}", ${JSON.stringify(e)})'>Editar</button>
                    <button class="btn btn-danger btn-sm" onclick='deleteEntity("${kind}", "${e.id}", ${JSON.stringify(e.name)})'>Excluir</button>
                </div></td>
            </tr>`).join('');
    } catch (e) {
        tbody.innerHTML = '';
        toast(e.message, 'err');
    }
}
const searchDevelopers = () => searchEntity('developer');
const searchPublishers = () => searchEntity('publisher');

// guarda o ultimo "ver jogos" aberto, para o botao "voltar" do detalhe do jogo
let lastEntityGames = null;

/**
 * Abre, num modal, a lista de jogos vinculados a um developer/publisher,
 * mostrando os dados relacionados (developer de cada jogo, preco, score, tags)
 * e um resumo agregado.
 */
async function openEntityGames(kind, name) {
    lastEntityGames = { kind, name };
    const param = (kind === 'developer') ? 'developer' : 'publisher';
    const label = (kind === 'developer') ? 'Developer' : 'Publisher';
    $('modal-content').classList.add('wide');
    $('modal-content').innerHTML = `<div class="modal-body"><h2>${esc(name)}</h2>
        <div class="modal-meta">Carregando jogos...</div></div>`;
    openModal();
    try {
        const data = await api('GET',
            `/games?${param}=${encodeURIComponent(name)}&limit=500`);
        const games = data.items;
        const total = data.total;
        const free = games.filter(g => g.price === 0).length;
        const paid = total - free;
        const avg = total ? (games.reduce((s, g) => s + g.reviewScore, 0) / total).toFixed(1) : '0';
        // para o developer: quantos publishers distintos (e vice-versa) -> dado relacionado
        const otherKey = (kind === 'developer') ? 'publisher' : 'developer';
        const otherLabel = (kind === 'developer') ? 'publishers' : 'developers';
        const distinct = new Set(games.map(g => g[otherKey])).size;

        const cards = total
            ? `<div class="grid">${games.map(g => gameCard(g, true)).join('')}</div>`
            : `<p class="empty">Nenhum jogo vinculado a este ${label.toLowerCase()}.</p>`;

        $('modal-content').innerHTML = `
            <div class="modal-body">
                <div class="entity-head">
                    <div>
                        <span class="entity-tag">${label}</span>
                        <h2>${esc(name)}</h2>
                    </div>
                    <button class="modal-x" onclick="closeModal()">✕</button>
                </div>
                <div class="detail-grid">
                    <div class="box"><label>Jogos</label><span>${total}</span></div>
                    <div class="box"><label>Gratuitos / Pagos</label><span style="font-size:15px">${free} / ${paid}</span></div>
                    <div class="box"><label>Score médio</label><span>${avg}/10</span></div>
                    <div class="box"><label>${otherLabel} distintos</label><span>${distinct}</span></div>
                </div>
                <p class="result-info">Clique em um jogo para ver os detalhes.</p>
                ${cards}
                <div class="modal-actions" style="margin-top:18px">
                    <button class="btn btn-ghost" onclick="closeModal()">Fechar</button>
                </div>
            </div>`;
    } catch (e) {
        toast(e.message, 'err', 'Falha ao carregar jogos');
        closeModal();
    }
}

function reopenEntityGames() {
    if (lastEntityGames) {
        openEntityGames(lastEntityGames.kind, lastEntityGames.name);
    }
}

function openEntityForm(kind, entity) {
    const cfg = entityCfg(kind);
    const e = entity || {};
    const editing = !!entity;
    $('modal-content').innerHTML = `
        <div class="modal-body">
            <h2>${editing ? 'Editar' : 'Novo'} ${cfg.label}</h2>
            <div class="form-grid">
                <div class="field full"><label>Nome <span class="req">*</span></label>
                    <input id="e-name" value="${esc(e.name || '')}"></div>
                <div class="field full"><label>País</label>
                    <input id="e-country" value="${esc(e.country || '')}"></div>
            </div>
            <div class="modal-actions" style="margin-top:18px">
                <button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
                <button class="btn btn-green" onclick='saveEntity("${kind}", ${editing}, ${JSON.stringify(e.id || null)})'>
                    ${editing ? 'Salvar' : 'Cadastrar'}</button>
            </div>
        </div>`;
    openModal();
}

async function saveEntity(kind, editing, id) {
    const cfg = entityCfg(kind);
    const body = { name: $('e-name').value.trim(), country: $('e-country').value.trim() || null };
    try {
        if (editing) {
            await api('PUT', cfg.path + '/' + id, body);
            toast(cfg.label + ' atualizado.', 'ok');
        } else {
            await api('POST', cfg.path, body);
            toast(cfg.label + ' cadastrado.', 'ok');
        }
        closeModal(); searchEntity(kind); loadStats();
    } catch (e) { toast(e.message, 'err', 'Não foi possível salvar'); }
}

async function deleteEntity(kind, id, name) {
    const cfg = entityCfg(kind);
    if (!confirm('Excluir ' + cfg.label + ' "' + name + '"?')) return;
    try {
        await api('DELETE', cfg.path + '/' + id);
        toast(cfg.label + ' excluído.', 'ok');
        searchEntity(kind); loadStats();
    } catch (e) {
        toast(e.message, 'err', 'Exclusão bloqueada');
    }
}

// =============================================================== IMPORT
let importTimer = null;

async function startImport() {
    const body = {
        path: $('i-path').value.trim(),
        limit: Number($('i-limit').value) || 0
    };
    try {
        const r = await api('POST', '/import', body);
        if (r.started) {
            toast('Importação iniciada em segundo plano.', 'ok');
            pollImport();
        } else {
            toast('Já existe uma importação em andamento.', 'err');
        }
    } catch (e) { toast(e.message, 'err'); }
}

function pollImport() {
    clearInterval(importTimer);
    importTimer = setInterval(refreshImportStatus, 1000);
    refreshImportStatus();
}

async function refreshImportStatus() {
    try {
        const s = await api('GET', '/import/status');
        const pct = s.totalRead > 0 ? Math.min(100, Math.round(s.inserted / s.totalRead * 100)) : 0;
        $('import-status').innerHTML = `
            <span class="${s.running ? 'running' : 'idle'}">
                ${s.running ? '● IMPORTANDO...' : '○ ' + esc(s.message)}
            </span>
            <div class="progress-bar"><div style="width:${pct}%"></div></div>
            <div class="kv">
                <span>Lidos: <b>${s.totalRead}</b></span>
                <span>Inseridos: <b>${s.inserted}</b></span>
                <span>Ignorados: <b>${s.skipped}</b></span>
                <span>Devs novos: <b>${s.developersCreated}</b></span>
                <span>Pubs novos: <b>${s.publishersCreated}</b></span>
            </div>`;
        if (!s.running) {
            clearInterval(importTimer);
            importTimer = null;
            loadStats();
        }
    } catch (e) { /* silencioso */ }
}

// =============================================================== MODAL
function openModal() { $('modal').classList.add('open'); }
function closeModal() {
    $('modal').classList.remove('open');
    $('modal-content').classList.remove('wide');
}
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

// =============================================================== INIT
loadStats();
searchGames();
