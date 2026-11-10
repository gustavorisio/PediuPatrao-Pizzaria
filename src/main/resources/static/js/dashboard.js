(() => {
    'use strict';
    const root = document.getElementById('dashboard');
    if (!root) return;
    const form = document.getElementById('dash-filters');
    const field = name => form.elements.namedItem(name);
    const el = id => document.getElementById(`dash-${id}`);
    const labels = {RECEBIDO: 'Recebido', EM_PREPARACAO: 'Em preparação', PRONTO: 'Pronto',
        SAIU_PARA_ENTREGA: 'Saiu para entrega', RETIRADO: 'Retirado', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado'};
    const money = value => new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'}).format(value);
    const number = value => new Intl.NumberFormat('pt-BR').format(value);
    const text = (id, value) => { el(id).textContent = value; };
    let busy = false;
    let applied = null;
    let filtersLoaded = false;
    const localDateTime = value => {
        const [date, time] = value.split('T');
        return `${date.split('-').reverse().join('/')} ${time.slice(0, 8)}`;
    };
    function month() {
        const parts = new Intl.DateTimeFormat('en-CA', {timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit'}).formatToParts(new Date());
        const values = Object.fromEntries(parts.map(p => [p.type, p.value]));
        field('fim').value = `${values.year}-${values.month}-${values.day}`;
        field('inicio').value = `${values.year}-${values.month}-01`;
        field('status').value = '';
        field('agrupamento').value = 'dia';
    }
    function row(target, values) {
        const tr = document.createElement('tr');
        values.forEach(value => { const td = document.createElement('td'); td.textContent = value; tr.append(td); });
        target.append(tr);
    }
    function table(id, rows, empty) {
        const body = el(id); body.replaceChildren();
        rows.forEach(values => row(body, values));
        if (!rows.length) row(body, [empty]);
    }
    function renderChart(series) {
        const ns = 'http://www.w3.org/2000/svg';
        const svg = document.createElementNS(ns, 'svg');
        svg.setAttribute('viewBox', '0 0 720 230');
        svg.setAttribute('preserveAspectRatio', 'none');
        const maximum = Math.max(1, ...series.map(p => Number(p.faturamento)));
        const add = (tag, attrs, content) => {
            const node = document.createElementNS(ns, tag);
            Object.entries(attrs).forEach(([key, value]) => node.setAttribute(key, value));
            if (content !== undefined) node.textContent = content;
            svg.append(node); return node;
        };
        for (let i = 0; i <= 4; i++) {
            const y = 15 + i * 45;
            add('line', {x1: 70, x2: 710, y1: y, y2: y, stroke: '#e3eae4'});
            add('text', {x: 0, y: y + 4, fill: '#526863', 'font-size': 11}, number(Math.round(maximum * (1 - i / 4))));
        }
        const width = 640 / Math.max(1, series.length);
        series.forEach((point, i) => {
            const height = Number(point.faturamento) / maximum * 180;
            const bar = add('rect', {x: 70 + i * width + width * .12, y: 195 - height, width: Math.max(.5, width * .76), height, rx: 2, fill: '#297258'});
            const title = document.createElementNS(ns, 'title');
            title.textContent = `${point.periodo}: ${money(point.faturamento)} · ${point.pedidos} pedidos`;
            bar.append(title);
            if (i === 0 || i === series.length - 1 || i % Math.max(1, Math.ceil(series.length / 5)) === 0)
                add('text', {x: 70 + i * width + width / 2, y: 220, 'text-anchor': 'middle', fill: '#526863', 'font-size': 10}, point.periodo.slice(5) || point.periodo);
        });
        el('chart').replaceChildren(svg);
        el('chart').setAttribute('aria-label', `Faturamento em ${series.length} períodos. Os valores estão na tabela abaixo.`);
    }
    function render(data) {
        const kpi = data.indicadores;
        text('revenue', money(kpi.faturamento)); text('orders', number(kpi.pedidos));
        text('valid', `${number(kpi.vendas)} pedidos válidos`); text('ticket', money(kpi.ticketMedio));
        text('open', number(kpi.emAndamento)); text('canceled', `${number(kpi.cancelados)} cancelados no período`);
        el('empty').hidden = kpi.pedidos > 0;
        renderChart(data.vendas);
        table('sales-table', data.vendas.map(p => [p.periodo, number(p.pedidos), money(p.faturamento)]), 'Sem vendas.');
        table('products', data.produtos.map(p => [p.descricao, number(p.quantidade), money(p.valorBruto)]), 'Nenhum item vendido neste período.');
        table('recent', data.pedidosRecentes.map(p => [p.numeroPedido == null ? 'Sem número' : `#${p.numeroPedido}`,
            localDateTime(p.entrada), labels[p.status] || 'Não informado', money(p.valorTotal)]), 'Nenhum pedido encontrado.');
        const status = el('status'); status.replaceChildren();
        data.situacoes.forEach(s => {
            const box = document.createElement('div'); box.className = 'dash-status-row';
            const label = document.createElement('div'); label.className = 'dash-status-label';
            const name = document.createElement('span'); name.textContent = labels[s.status] || s.status;
            const count = document.createElement('strong'); count.textContent = number(s.quantidade);
            label.append(name, count);
            const track = document.createElement('div'); track.className = 'dash-track';
            const fill = document.createElement('div'); fill.className = 'dash-fill';
            fill.style.width = `${kpi.pedidos ? s.quantidade / kpi.pedidos * 100 : 0}%`;
            if (s.status === 'CANCELADO') fill.style.background = '#bd6352';
            track.append(fill); box.append(label, track); status.append(box);
        });
        text('feedback', `${data.inicio} a ${data.fim} • ${labels[data.status] || 'Todas as situações'} • Atualizado às ${data.atualizadoEm.slice(11, 19)} (Brasília)`);
    }
    async function request(path) {
        const response = await fetch(`${root.dataset.api}/${path}`, {headers: {Accept: 'application/json'}, credentials: 'same-origin', signal: AbortSignal.timeout(20000)});
        if (response.status === 401) throw new Error('Sua sessão expirou. Entre novamente para consultar os dados.');
        if (response.status === 403) throw new Error('Acesso permitido somente para administrador ou gerente.');
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Não foi possível consultar os dados. Tente novamente.');
        }
        if (!response.headers.get('content-type')?.includes('application/json')) throw new Error('Sua sessão expirou. Entre novamente.');
        return response.json();
    }
    async function load(useApplied = false) {
        if (busy) return;
        if (!useApplied && !form.reportValidity()) return;
        const params = useApplied && applied ? new URLSearchParams(applied) : new URLSearchParams(new FormData(form));
        const days = (Date.parse(params.get('fim')) - Date.parse(params.get('inicio'))) / 86400000;
        if (!Number.isFinite(days) || days < 0 || days > 365) {
            el('error').hidden = false;
            text('error', 'Selecione um período de 1 a 366 dias, com início anterior ou igual ao fim.'); return;
        }
        busy = true; el('results').setAttribute('aria-busy', 'true'); el('error').hidden = true;
        root.querySelectorAll('button').forEach(button => { button.disabled = true; });
        text('feedback', 'Consultando os pedidos…');
        try {
            if (!filtersLoaded) {
                const filters = await request('filtros');
                filters.situacoes.forEach(status => { const option = document.createElement('option'); option.value = status; option.textContent = labels[status] || status; field('status').append(option); });
                filtersLoaded = true;
            }
            const data = await request(`resumo?${params}`);
            render(data); applied = params.toString(); el('results').classList.remove('dash-stale');
        } catch (error) {
            el('error').hidden = false;
            text('error', error.name === 'TimeoutError' ? 'A consulta demorou demais. Tente atualizar novamente.' : error.message || 'Falha de conexão. Tente novamente.');
            text('feedback', 'Atualização não concluída. Os dados anteriores, se exibidos, estão desatualizados.');
            el('results').classList.add('dash-stale');
        } finally {
            busy = false; el('results').setAttribute('aria-busy', 'false');
            root.querySelectorAll('button').forEach(button => { button.disabled = false; });
        }
    }
    async function init() {
        month();
        await load();
    }
    form.addEventListener('submit', event => { event.preventDefault(); load(); });
    el('refresh').addEventListener('click', () => load(true));
    el('month').addEventListener('click', () => { month(); load(); });
    window.setInterval(() => { if (el('auto').checked && !document.hidden) load(true); }, 60000);
    init();
})();
