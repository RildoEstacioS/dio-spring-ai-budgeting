import { apiAmountToCents, centsToJsonNumber, formatCentsAsBrl, parseBrlToCents } from './money.js';

// Espelho dos valores reais do enum dio.budgeting.domain.Category.
// A API não expõe as categorias: se o enum mudar, esta lista precisa ser atualizada.
const CATEGORIES = [
    { value: 'GROCERIES', label: 'Mercado' },
    { value: 'PHARMA', label: 'Farmácia' },
    { value: 'AUTO', label: 'Automóvel' },
];
const ALL = 'ALL';
const API_BASE = '/transactions';
const MAX_CENTS = 99_999_999_999n; // R$ 999.999.999,99
const AMOUNT_HINT = 'Use vírgula para os centavos, por exemplo 80,00.';

const state = { transactions: [], filter: ALL, loadError: null };
const $ = (id) => document.getElementById(id);

init();

function init() {
    renderCategoryOptions();
    renderFilters();

    $('expense-form').addEventListener('submit', handleSubmit);
    $('amount').addEventListener('input', updateAmountHint);
    $('refresh-button').addEventListener('click', loadTransactions);

    loadTransactions();
}

// ---------- API ----------

async function fetchCategory(category) {
    const response = await fetch(`${API_BASE}/${category}`, { headers: { Accept: 'application/json' } });
    if (!response.ok) {
        throw new Error(`GET ${API_BASE}/${category} respondeu HTTP ${response.status}`);
    }
    const items = await response.json();
    return items.map((item) => ({
        id: item.id,
        description: item.description,
        category: item.category,
        cents: apiAmountToCents(item.amount),
    }));
}

async function loadTransactions() {
    $('table-wrapper').setAttribute('aria-busy', 'true');
    try {
        const lists = await Promise.all(CATEGORIES.map((category) => fetchCategory(category.value)));
        state.transactions = lists.flat().sort(byAmountDescending);
        state.loadError = null;
    } catch (error) {
        state.transactions = [];
        state.loadError = error.message;
    } finally {
        $('table-wrapper').setAttribute('aria-busy', 'false');
        render();
    }
}

async function handleSubmit(event) {
    event.preventDefault();

    const description = $('description').value.trim();
    const category = $('category').value;
    const cents = parseBrlToCents($('amount').value);

    const invalid = validate(description, category, cents);
    if (invalid) {
        showFeedback('error', invalid.message);
        invalid.field.setAttribute('aria-invalid', 'true');
        invalid.field.focus();
        return;
    }

    setSubmitting(true);
    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify({ description, category, amount: centsToJsonNumber(cents) }),
        });
        if (!response.ok) {
            throw new Error(`a API respondeu HTTP ${response.status}`);
        }
        const created = await response.json();
        showFeedback('success',
            `Despesa "${created.description}" registrada: ${formatCentsAsBrl(apiAmountToCents(created.amount))} em ${labelOf(created.category)}.`);
        $('expense-form').reset();
        updateAmountHint();
        await loadTransactions();
    } catch (error) {
        showFeedback('error', `Não foi possível registrar a despesa: ${error.message}.`);
    } finally {
        setSubmitting(false);
    }
}

// ---------- Validação ----------

function validate(description, category, cents) {
    for (const id of ['description', 'category', 'amount']) {
        $(id).removeAttribute('aria-invalid');
    }
    if (!description) {
        return { field: $('description'), message: 'Informe uma descrição.' };
    }
    if (!CATEGORIES.some((item) => item.value === category)) {
        return { field: $('category'), message: 'Selecione uma categoria.' };
    }
    if (cents === null) {
        return { field: $('amount'), message: 'Valor inválido. Use o formato 80,00, com vírgula para os centavos.' };
    }
    if (cents <= 0n) {
        return { field: $('amount'), message: 'O valor deve ser maior que zero.' };
    }
    if (cents > MAX_CENTS) {
        return { field: $('amount'), message: `O valor máximo é ${formatCentsAsBrl(MAX_CENTS)}.` };
    }
    return null;
}

function updateAmountHint() {
    const cents = parseBrlToCents($('amount').value);
    $('amount-hint').textContent = cents !== null && cents > 0n
        ? `${formatCentsAsBrl(cents)} será enviado à API como ${cents} centavos.`
        : AMOUNT_HINT;
}

// ---------- Renderização ----------

function render() {
    renderCards();
    renderTable();
}

function renderCards() {
    if (state.loadError) {
        for (const id of ['card-total', 'card-pharma', 'card-groceries', 'card-count']) {
            $(id).textContent = '—';
        }
        return;
    }
    const all = summarize(state.transactions);
    const pharma = summarize(state.transactions, 'PHARMA');
    const groceries = summarize(state.transactions, 'GROCERIES');

    $('card-total').textContent = formatCentsAsBrl(all.cents);
    $('card-pharma').textContent = formatCentsAsBrl(pharma.cents);
    $('card-pharma-hint').textContent = countLabel(pharma.count);
    $('card-groceries').textContent = formatCentsAsBrl(groceries.cents);
    $('card-groceries-hint').textContent = countLabel(groceries.count);
    $('card-count').textContent = String(all.count);
}

function renderTable() {
    const visible = state.filter === ALL
        ? state.transactions
        : state.transactions.filter((item) => item.category === state.filter);

    $('expenses-body').replaceChildren(...visible.map(createRow));

    const isEmpty = visible.length === 0;
    $('expenses-table').hidden = isEmpty;
    $('empty-state').hidden = !isEmpty;
    $('empty-state').classList.toggle('is-error', Boolean(state.loadError));

    if (state.loadError) {
        $('empty-title').textContent = 'Não foi possível carregar as despesas';
        $('empty-text').textContent = `${state.loadError}. Verifique se a API está rodando e clique em Atualizar.`;
    } else if (isEmpty && state.filter === ALL) {
        $('empty-title').textContent = 'Nenhuma despesa registrada';
        $('empty-text').textContent = 'Use o formulário para adicionar a primeira despesa.';
    } else if (isEmpty) {
        $('empty-title').textContent = `Nenhuma despesa em ${labelOf(state.filter)}`;
        $('empty-text').textContent = 'Escolha outra categoria ou adicione uma despesa nesta.';
    }

    const filtered = summarize(visible);
    $('list-footer').textContent = state.loadError || isEmpty
        ? ''
        : `${countLabel(filtered.count)} · Total: ${formatCentsAsBrl(filtered.cents)}`;
}

function createRow(transaction) {
    const row = document.createElement('tr');

    const description = cell('Descrição');
    description.textContent = transaction.description;

    const category = cell('Categoria');
    const badge = document.createElement('span');
    badge.className = `badge badge-${transaction.category.toLowerCase()}`;
    badge.textContent = labelOf(transaction.category);
    category.append(badge);

    const amount = cell('Valor');
    amount.className = 'num';
    amount.textContent = formatCentsAsBrl(transaction.cents);

    row.append(description, category, amount);
    return row;
}

function cell(label) {
    const td = document.createElement('td');
    td.dataset.label = label;
    return td;
}

function renderCategoryOptions() {
    const select = $('category');
    for (const category of CATEGORIES) {
        select.append(new Option(category.label, category.value));
    }
}

function renderFilters() {
    const options = [{ value: ALL, label: 'Todas' }, ...CATEGORIES];
    const buttons = options.map((option) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'chip';
        button.textContent = option.label;
        button.dataset.value = option.value;
        button.setAttribute('aria-pressed', String(option.value === state.filter));
        button.addEventListener('click', () => selectFilter(option.value));
        return button;
    });
    $('filters').replaceChildren(...buttons);
}

function selectFilter(value) {
    state.filter = value;
    for (const button of $('filters').querySelectorAll('button')) {
        button.setAttribute('aria-pressed', String(button.dataset.value === value));
    }
    renderTable();
}

// ---------- Utilitários ----------

function summarize(transactions, category) {
    const items = category ? transactions.filter((item) => item.category === category) : transactions;
    return { cents: items.reduce((sum, item) => sum + item.cents, 0n), count: items.length };
}

function byAmountDescending(a, b) {
    if (a.cents !== b.cents) {
        return a.cents > b.cents ? -1 : 1;
    }
    return a.description.localeCompare(b.description, 'pt-BR');
}

function labelOf(value) {
    return CATEGORIES.find((item) => item.value === value)?.label ?? value;
}

function countLabel(count) {
    return count === 1 ? '1 lançamento' : `${count} lançamentos`;
}

function showFeedback(type, message) {
    const feedback = $('feedback');
    feedback.className = `feedback feedback-${type}`;
    feedback.textContent = message;
    feedback.hidden = false;
}

function setSubmitting(submitting) {
    const button = $('submit-button');
    button.disabled = submitting;
    button.textContent = submitting ? 'Enviando…' : 'Adicionar despesa';
}
