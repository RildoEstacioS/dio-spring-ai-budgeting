// Histórico de lançamentos: filtros, totais do servidor, paginação, edição, exclusão e confirmação de previstos.

import { api } from './api.js';
import { formatDate, todayIso } from './dates.js';
import { openConfirmPlannedDialog, openDeleteDialog, openEntryDialog } from './entry-form.js';
import { reference } from './reference.js';
import { el, emptyState, errorState, money, pluralize, showToast, signedMoney, statusBadge } from './ui.js';

const PAGE_SIZE = 20;
const state = {
    filters: { start: '', end: '', type: '', category: '', status: '' },
    page: 0,
    items: [],
    totalItems: 0,
    totalPages: 0,
    totals: null,
};

let view;

export async function renderEntries(root, params, ctx) {
    if (params.has('status')) {
        state.filters.status = params.get('status');
    }

    view = {
        totals: el('div', { class: 'totals-bar', 'aria-live': 'polite' }),
        list: el('div', { class: 'entries-list', 'aria-busy': 'true' }, el('p', { class: 'loading', text: 'Carregando lançamentos…' })),
        more: el('button', { type: 'button', class: 'button button-secondary load-more', text: 'Carregar mais', hidden: true,
            onclick: () => load(false) }),
        ctx,
    };

    root.replaceChildren(el('section', { class: 'page', 'aria-labelledby': 'entries-title' },
        el('div', { class: 'page-header' },
            el('h2', { id: 'entries-title', text: 'Lançamentos' }),
            el('button', { type: 'button', class: 'button button-primary', text: '+ Novo lançamento', onclick: ctx.openCreate })),
        filtersForm(),
        view.totals,
        view.list,
        view.more));

    await load(true);
}

function filtersForm() {
    const f = state.filters;
    const categorySelect = el('select', { id: 'filter-category' });
    const fillCategories = () => {
        const options = reference.categoriesFor(f.type);
        categorySelect.replaceChildren(el('option', { value: '', text: 'Todas' }),
            ...options.map((category) => el('option', { value: category.value, text: category.label })));
        if (!options.some((category) => category.value === f.category)) {
            f.category = '';
        }
        categorySelect.value = f.category;
    };
    fillCategories();

    const onChange = (key) => (event) => {
        f[key] = event.target.value;
        if (key === 'type') {
            fillCategories();
        }
        load(true);
    };
    categorySelect.addEventListener('change', onChange('category'));

    const typeSelect = el('select', { id: 'filter-type', onchange: onChange('type') },
        el('option', { value: '', text: 'Todos' }),
        ...reference.types.map((type) => el('option', { value: type.value, text: type.label })));
    typeSelect.value = f.type;

    const statusSelect = el('select', { id: 'filter-status', onchange: onChange('status') },
        el('option', { value: '', text: 'Todas' }),
        ...reference.statuses.map((status) => el('option', { value: status.value, text: status.label })));
    statusSelect.value = f.status;

    return el('form', { class: 'filters-form', 'aria-label': 'Filtros', onsubmit: (event) => event.preventDefault() },
        filterField('filter-start', 'De', el('input', { type: 'date', id: 'filter-start', value: f.start, onchange: onChange('start') })),
        filterField('filter-end', 'Até', el('input', { type: 'date', id: 'filter-end', value: f.end, onchange: onChange('end') })),
        filterField('filter-type', 'Tipo', typeSelect),
        filterField('filter-category', 'Categoria', categorySelect),
        filterField('filter-status', 'Situação', statusSelect),
        el('button', { type: 'button', class: 'button button-secondary', text: 'Limpar filtros', onclick: () => {
            Object.assign(state.filters, { start: '', end: '', type: '', category: '', status: '' });
            if (location.hash === '#/lancamentos') {
                view.ctx.rerender();
            } else {
                location.hash = '#/lancamentos';
            }
        } }));
}

function filterField(id, label, control) {
    return el('div', { class: 'filter-field' }, el('label', { for: id, text: label }), control);
}

async function load(reset) {
    const page = reset ? 0 : state.page + 1;
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
    for (const [key, value] of Object.entries(state.filters)) {
        if (value) {
            params.set(key, value);
        }
    }

    view.list.setAttribute('aria-busy', 'true');
    view.more.disabled = true;
    try {
        const result = await api('GET', `/transactions?${params}`);
        state.page = result.page;
        state.items = reset ? result.items : state.items.concat(result.items);
        state.totalItems = result.totalItems;
        state.totalPages = result.totalPages;
        state.totals = result.totals;
        render();
    } catch (error) {
        view.list.replaceChildren(errorState(error));
        view.totals.replaceChildren();
        view.more.hidden = true;
    } finally {
        view.list.setAttribute('aria-busy', 'false');
        view.more.disabled = false;
    }
}

function render() {
    renderTotals();

    if (!state.items.length) {
        const filtered = Object.values(state.filters).some(Boolean);
        view.list.replaceChildren(filtered
            ? emptyState('Nenhum lançamento encontrado', 'Ajuste ou limpe os filtros para ver outros lançamentos.')
            : emptyState('Nenhum lançamento ainda', 'Registre sua primeira receita ou despesa.',
                el('button', { type: 'button', class: 'button button-primary', text: 'Registrar primeiro lançamento',
                    onclick: view.ctx.openCreate })));
        view.more.hidden = true;
        return;
    }

    view.list.replaceChildren(el('table', { class: 'entries-table' },
        el('caption', { class: 'sr-only', text: 'Lançamentos filtrados' }),
        el('thead', {}, el('tr', {},
            ...['Data', 'Descrição', 'Categoria', 'Pagamento', 'Situação', 'Valor', 'Ações']
                .map((title) => el('th', { scope: 'col', class: title === 'Valor' ? 'num' : null, text: title })))),
        el('tbody', {}, ...state.items.map(row))),
    el('p', { class: 'list-footer', text: `Exibindo ${state.items.length} de ${pluralize(state.totalItems, 'lançamento', 'lançamentos')}` }));

    view.more.hidden = state.page + 1 >= state.totalPages;
}

function renderTotals() {
    const { confirmed, planned } = state.totals;
    const block = (title, totals, kind) => el('div', { class: `totals-block ${kind}` },
        el('p', { class: 'totals-title', text: `${title} · ${pluralize(totals.count, 'lançamento', 'lançamentos')}` }),
        el('dl', {},
            el('dt', { text: 'Receitas' }), el('dd', { class: 'income', text: money(totals.incomeInCents) }),
            el('dt', { text: 'Despesas' }), el('dd', { class: 'expense', text: money(totals.expenseInCents) }),
            el('dt', { text: 'Saldo' }), el('dd', { class: totals.balanceInCents < 0 ? 'expense' : 'income', text: money(totals.balanceInCents) })));

    view.totals.replaceChildren(block('Realizado', confirmed, 'is-confirmed'),
        planned.count > 0 ? block('Previsto', planned, 'is-planned') : null);
}

function row(entry) {
    const planned = entry.status === 'PLANNED';
    const canConfirm = planned && entry.occurredAt <= todayIso();
    const reload = (message) => () => {
        showToast(message);
        view.ctx.rerender();
    };

    return el('tr', { class: planned ? 'is-planned' : null },
        el('td', { 'data-label': 'Data', text: formatDate(entry.occurredAt) }),
        el('td', { 'data-label': 'Descrição', class: 'cell-description' },
            el('span', { text: entry.description }),
            entry.recurrence !== 'NONE' ? el('span', { class: 'badge badge-muted', text: reference.recurrenceLabel(entry.recurrence) }) : null,
            entry.notes ? el('span', { class: 'cell-notes', text: entry.notes }) : null),
        el('td', { 'data-label': 'Categoria', text: reference.categoryLabel(entry.category) }),
        el('td', { 'data-label': 'Pagamento', text: reference.paymentLabel(entry.paymentMethod) }),
        el('td', { 'data-label': 'Situação' }, statusBadge(entry.status)),
        el('td', { 'data-label': 'Valor', class: `num entry-amount ${planned ? 'planned' : entry.type === 'INCOME' ? 'income' : 'expense'}`,
            text: signedMoney(entry) }),
        el('td', { 'data-label': 'Ações', class: 'cell-actions' },
            canConfirm ? el('button', { type: 'button', class: 'link-button', text: 'Marcar como realizado',
                onclick: () => openConfirmPlannedDialog(entry, reload('Lançamento marcado como realizado.')) }) : null,
            el('button', { type: 'button', class: 'link-button', text: 'Editar', 'aria-label': `Editar ${entry.description}`,
                onclick: () => openEntryDialog({ entry, onSaved: reload('Lançamento atualizado.') }) }),
            el('button', { type: 'button', class: 'link-button danger', text: 'Excluir', 'aria-label': `Excluir ${entry.description}`,
                onclick: () => openDeleteDialog(entry, reload('Lançamento excluído.')) })));
}
