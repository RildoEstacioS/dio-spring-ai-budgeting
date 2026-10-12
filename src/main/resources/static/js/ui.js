// Utilitários de DOM. Todo texto vindo da API entra via textContent (nunca innerHTML).

import { centsFromApi, formatCentsAsBrl } from './money.js';

export function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(props)) {
        if (value === undefined || value === null || value === false) {
            continue;
        }
        if (key === 'class') {
            node.className = value;
        } else if (key === 'text') {
            node.textContent = value;
        } else if (key === 'dataset') {
            Object.assign(node.dataset, value);
        } else if (key.startsWith('on') && typeof value === 'function') {
            node.addEventListener(key.slice(2), value);
        } else if (value === true) {
            node.setAttribute(key, '');
        } else {
            node.setAttribute(key, String(value));
        }
    }
    for (const child of children.flat()) {
        if (child === null || child === undefined || child === false) {
            continue;
        }
        node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return node;
}

/** Formata um campo *InCents da API como BRL. */
export function money(cents) {
    return formatCentsAsBrl(centsFromApi(cents));
}

/** Valor com sinal pelo tipo: receitas "+", despesas "−". O amount é sempre positivo na API. */
export function signedMoney(entry) {
    return `${entry.type === 'INCOME' ? '+' : '−'} ${money(entry.amountInCents)}`;
}

export function badge(text, kind) {
    return el('span', { class: `badge badge-${kind}`, text });
}

export function statusBadge(status) {
    return status === 'PLANNED' ? badge('Previsto', 'planned') : badge('Realizado', 'confirmed');
}

export function typeBadge(type) {
    return type === 'INCOME' ? badge('Receita', 'income') : badge('Despesa', 'expense');
}

export function emptyState(title, text, action) {
    return el('div', { class: 'empty-state' },
        el('p', { class: 'empty-title', text: title }),
        el('p', { class: 'empty-text', text }),
        action ?? null);
}

export function errorState(error) {
    return el('div', { class: 'empty-state is-error', role: 'alert' },
        el('p', { class: 'empty-title', text: error.problem?.title || 'Algo deu errado' }),
        el('p', { class: 'empty-text', text: error.message || 'Tente novamente.' }));
}

let toastTimer;

export function showToast(message, kind = 'success') {
    const toast = document.getElementById('toast');
    toast.className = `toast toast-${kind}`;
    toast.textContent = message;
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.hidden = true;
    }, 4000);
}

export function pluralize(count, singular, plural) {
    return `${count} ${count === 1 ? singular : plural}`;
}
