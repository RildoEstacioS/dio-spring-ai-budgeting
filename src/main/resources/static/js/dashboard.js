// Painel: valores calculados no servidor (GET /dashboard/summary); aqui só formatamos a partir dos centavos.

import { api } from './api.js';
import { formatDate, formatRange } from './dates.js';
import { reference } from './reference.js';
import { el, emptyState, errorState, money, pluralize, signedMoney, statusBadge } from './ui.js';

const PRESETS = [
    { value: 'THIS_MONTH', label: 'Este mês' },
    { value: 'LAST_MONTH', label: 'Mês anterior' },
    { value: 'LAST_30_DAYS', label: 'Últimos 30 dias' },
    { value: 'CUSTOM', label: 'Personalizado' },
];

const state = { preset: 'THIS_MONTH', start: '', end: '' };

export async function renderDashboard(root, params, ctx) {
    const content = el('div', { class: 'dashboard-content', 'aria-live': 'polite', 'aria-busy': 'true' },
        el('p', { class: 'loading', text: 'Carregando painel…' }));

    root.replaceChildren(el('section', { class: 'page', 'aria-labelledby': 'dashboard-title' },
        el('div', { class: 'page-header' }, el('h2', { id: 'dashboard-title', text: 'Painel' })),
        periodFilter(() => renderDashboard(root, params, ctx)),
        content));

    if (state.preset === 'CUSTOM' && (!state.start || !state.end)) {
        content.replaceChildren(emptyState('Escolha o intervalo', 'Informe as datas inicial e final e clique em Aplicar.'));
        content.setAttribute('aria-busy', 'false');
        return;
    }

    try {
        const summary = await api('GET', `/dashboard/summary?${query()}`);
        content.replaceChildren(...renderSummary(summary, ctx));
    } catch (error) {
        content.replaceChildren(errorState(error));
    }
    content.setAttribute('aria-busy', 'false');
}

function query() {
    if (state.preset === 'CUSTOM') {
        return new URLSearchParams({ start: state.start, end: state.end }).toString();
    }
    return new URLSearchParams({ period: state.preset }).toString();
}

function periodFilter(reload) {
    const startInput = el('input', { type: 'date', id: 'period-start', value: state.start });
    const endInput = el('input', { type: 'date', id: 'period-end', value: state.end });
    const custom = el('form', { class: 'custom-period', hidden: state.preset !== 'CUSTOM',
        onsubmit: (event) => {
            event.preventDefault();
            state.start = startInput.value;
            state.end = endInput.value;
            reload();
        } },
    el('label', { for: 'period-start', text: 'De' }), startInput,
    el('label', { for: 'period-end', text: 'até' }), endInput,
    el('button', { type: 'submit', class: 'button button-secondary', text: 'Aplicar' }));

    return el('div', { class: 'period-filter' },
        el('div', { class: 'chips', role: 'group', 'aria-label': 'Período' },
            ...PRESETS.map((preset) => el('button', {
                type: 'button', class: 'chip', text: preset.label,
                'aria-pressed': String(state.preset === preset.value),
                onclick: () => {
                    state.preset = preset.value;
                    reload();
                },
            }))),
        custom);
}

function renderSummary(summary, ctx) {
    const nodes = [
        el('p', { class: 'period-info' },
            el('strong', { text: `${summary.label}: ` }), formatRange(summary.period),
            el('span', { class: 'muted', text: ` · comparado com ${formatRange(summary.previousPeriod)}` })),
        el('div', { class: 'cards' },
            card('Receitas realizadas', money(summary.income.totalInCents), summary.comparison.income, 'income'),
            card('Despesas realizadas', money(summary.expense.totalInCents), summary.comparison.expense, 'expense'),
            card('Saldo realizado', money(summary.balance.totalInCents), summary.comparison.balance,
                summary.balance.totalInCents < 0 ? 'negative' : 'balance'),
            el('article', { class: 'card' },
                el('h3', { class: 'card-label', text: 'Lançamentos realizados' }),
                el('p', { class: 'card-value', text: String(summary.entryCount) }),
                el('p', { class: 'card-hint', text: 'Receitas e despesas confirmadas no período' }))),
    ];

    const isEmpty = summary.entryCount === 0 && summary.forecast.plannedCount === 0;
    if (isEmpty) {
        nodes.push(emptyState('Nenhum lançamento neste período',
            'Registre o primeiro lançamento para acompanhar receitas, despesas e saldo.',
            el('button', { type: 'button', class: 'button button-primary', text: 'Registrar primeiro lançamento',
                onclick: ctx.openCreate })));
    } else {
        nodes.push(el('div', { class: 'dashboard-grid' },
            categoriesPanel(summary),
            insightsPanel(summary),
            topExpensesPanel(summary),
            recentPanel(summary)));
    }

    nodes.push(forecastPanel(summary), upcomingFeatures());
    return nodes;
}

function card(label, value, comparison, kind) {
    return el('article', { class: `card card-${kind}` },
        el('h3', { class: 'card-label', text: label }),
        el('p', { class: 'card-value', text: value }),
        el('p', { class: 'card-hint', text: deltaText(comparison) }));
}

function deltaText(comparison) {
    if (comparison.deltaPercent === null || comparison.deltaPercent === undefined) {
        return 'Sem base de comparação no período anterior';
    }
    const arrow = comparison.deltaInCents > 0 ? '▲' : comparison.deltaInCents < 0 ? '▼' : '=';
    return `${arrow} ${formatPercent(comparison.deltaPercent)} vs período anterior (${money(comparison.previousInCents)})`;
}

/** Percentual já calculado no servidor (1 casa decimal), apenas exibido em pt-BR: 19 → "19,0%". */
function formatPercent(value) {
    return `${Math.abs(Number(value)).toFixed(1).replace('.', ',')}%`;
}

function panel(title, ...children) {
    return el('section', { class: 'panel' }, el('h3', { class: 'panel-title', text: title }), ...children);
}

function categoriesPanel(summary) {
    if (!summary.expensesByCategory.length) {
        return panel('Despesas por categoria', el('p', { class: 'muted', text: 'Nenhuma despesa realizada no período.' }));
    }
    return panel('Despesas por categoria', el('ul', { class: 'bars' },
        ...summary.expensesByCategory.map((item) => el('li', { class: 'bar-item' },
            el('div', { class: 'bar-label' },
                el('span', { text: item.label }),
                el('span', { class: 'bar-value', text: `${money(item.totalInCents)} · ${formatPercent(item.sharePercent)}` })),
            el('div', { class: 'bar-track', 'aria-hidden': 'true' },
                el('div', { class: 'bar-fill', style: `width: ${Number(item.sharePercent)}%` })),
            el('span', { class: 'sr-only', text: pluralize(item.count, 'lançamento', 'lançamentos') })))));
}

function insightsPanel(summary) {
    const list = summary.insights.length
        ? el('ul', { class: 'insights' }, ...summary.insights.map((insight) => el('li', { text: insight.message })))
        : el('p', { class: 'muted', text: 'Sem destaques para este período.' });
    return panel('Destaques', list,
        el('p', { class: 'panel-note', text: 'Calculados localmente a partir dos lançamentos realizados, sem IA.' }));
}

function topExpensesPanel(summary) {
    if (!summary.topExpenses.length) {
        return panel('Maiores despesas', el('p', { class: 'muted', text: 'Nenhuma despesa realizada no período.' }));
    }
    return panel('Maiores despesas', el('ol', { class: 'entry-list' },
        ...summary.topExpenses.map((entry) => el('li', {},
            el('div', { class: 'entry-main' },
                el('span', { class: 'entry-description', text: entry.description }),
                el('span', { class: 'entry-meta', text: `${reference.categoryLabel(entry.category)} · ${formatDate(entry.occurredAt)}` })),
            el('span', { class: 'entry-amount expense', text: money(entry.amountInCents) })))));
}

function recentPanel(summary) {
    if (!summary.recentEntries.length) {
        return panel('Últimos lançamentos', el('p', { class: 'muted', text: 'Nenhum lançamento realizado no período.' }));
    }
    return panel('Últimos lançamentos', el('ul', { class: 'entry-list' },
        ...summary.recentEntries.map((entry) => el('li', {},
            el('div', { class: 'entry-main' },
                el('span', { class: 'entry-description', text: entry.description }),
                el('span', { class: 'entry-meta', text: `${formatDate(entry.occurredAt)} · ${reference.categoryLabel(entry.category)}` })),
            el('span', { class: `entry-amount ${entry.type === 'INCOME' ? 'income' : 'expense'}`, text: signedMoney(entry) })))),
    el('a', { class: 'panel-link', href: '#/lancamentos', text: 'Ver todos os lançamentos' }));
}

function forecastPanel(summary) {
    const forecast = summary.forecast;
    const children = [];

    if (forecast.overduePlannedCount > 0) {
        children.push(el('p', { class: 'alert', role: 'status' },
            `${pluralize(forecast.overduePlannedCount, 'lançamento previsto', 'lançamentos previstos')} com data passada aguardando confirmação (em qualquer período). `,
            el('a', { href: '#/lancamentos?status=PLANNED', text: 'Revisar previstos' })));
    }

    if (forecast.plannedCount === 0) {
        children.push(el('p', { class: 'muted', text: 'Nenhum lançamento previsto neste período.' }));
    } else {
        children.push(
            el('div', { class: 'forecast-grid' },
                metric('Receitas previstas', money(forecast.plannedIncome.totalInCents)),
                metric('Despesas previstas', money(forecast.plannedExpense.totalInCents)),
                metric('Saldo projetado', money(forecast.projectedBalance.totalInCents), 'Saldo realizado + receitas previstas − despesas previstas')),
            el('ul', { class: 'entry-list' },
                ...forecast.upcoming.map((entry) => el('li', { class: 'is-planned' },
                    el('div', { class: 'entry-main' },
                        el('span', { class: 'entry-description' }, entry.description, ' ', statusBadge(entry.status)),
                        el('span', { class: 'entry-meta', text: `${formatDate(entry.occurredAt)} · ${reference.categoryLabel(entry.category)}` })),
                    el('span', { class: 'entry-amount planned', text: signedMoney(entry) })))));
    }

    return el('section', { class: 'panel forecast-panel', 'aria-labelledby': 'forecast-title' },
        el('h3', { class: 'panel-title', id: 'forecast-title' }, 'Previsto no período ', statusBadge('PLANNED')),
        el('p', { class: 'panel-note', text: 'Lançamentos previstos não entram nos valores realizados acima.' }),
        ...children);
}

function metric(label, value, hint) {
    return el('div', { class: 'metric' },
        el('p', { class: 'metric-label', text: label }),
        el('p', { class: 'metric-value', text: value }),
        hint ? el('p', { class: 'metric-hint', text: hint }) : null);
}

function upcomingFeatures() {
    const features = reference.features;
    return el('section', { class: 'upcoming', 'aria-labelledby': 'upcoming-title' },
        el('h3', { class: 'panel-title', id: 'upcoming-title', text: 'Próximos recursos' }),
        el('div', { class: 'upcoming-grid' },
            futureCard('Orçamentos', 'Em breve', 'Comparar previsto e realizado por categoria.'),
            futureCard('Metas', 'Em breve', 'Acompanhar o progresso de objetivos financeiros.'),
            futureCard('Assistente', features.assistant ? 'Habilitado' : 'Indisponível',
                'Perguntas em texto respondidas com dados dos seus lançamentos. Requer um provedor de IA configurado no servidor.'),
            futureCard('Voz', features.voice ? 'Habilitado' : 'Indisponível',
                'O endpoint POST /transactions/ai foi projetado para comandos de voz, mas ainda não foi validado e exige provedor de IA.')));
}

function futureCard(title, status, text) {
    return el('article', { class: 'future-card', 'aria-disabled': 'true' },
        el('p', { class: 'future-title' }, title, ' ', el('span', { class: 'badge badge-muted', text: status })),
        el('p', { class: 'future-text', text }));
}
