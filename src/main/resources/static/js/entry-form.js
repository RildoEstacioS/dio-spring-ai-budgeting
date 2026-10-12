// Diálogos de criar/editar, excluir e confirmar lançamento previsto.

import { api, ApiError } from './api.js';
import { centsFromApi, centsToJsonNumber, formatCentsAsBrl, formatCentsForInput, parseBrlToCents } from './money.js';
import { formatDate, isFuture, maxEntryDateIso, MIN_ENTRY_DATE, todayIso } from './dates.js';
import { reference } from './reference.js';
import { el, money } from './ui.js';

const MAX_CENTS = 99_999_999_999n; // R$ 999.999.999,99
const AMOUNT_HINT = 'Use vírgula para os centavos, por exemplo 80,00.';
const FIELDS = ['type', 'description', 'amount', 'occurredAt', 'category', 'paymentMethod', 'recurrence', 'status', 'notes'];

let form;
let current = { entry: null, onSaved: null };

// ---------- Criar / editar ----------

export function openEntryDialog({ entry = null, onSaved }) {
    form ??= buildEntryDialog();
    current = { entry, onSaved };
    fillForm(entry);
    form.dialog.showModal();
    form.inputs.description.focus();
}

function buildEntryDialog() {
    const inputs = {};
    const errors = {};

    // control é o que aparece na tela; input é o elemento de formulário (diferente só no campo de valor).
    const field = (name, label, control, hint, input = control) => {
        inputs[name] = input;
        errors[name] = el('p', { class: 'field-error', id: `entry-${name}-error`, hidden: true });
        input.setAttribute('aria-describedby', `entry-${name}-error${hint ? ` ${hint.id}` : ''}`);
        return el('div', { class: 'field' },
            el('label', { for: input.id, text: label }), control, hint ?? null, errors[name]);
    };

    const typeGroup = el('div', { class: 'segmented', role: 'radiogroup', 'aria-label': 'Tipo' },
        ...reference.types.map((type) => el('label', { class: 'segmented-option' },
            el('input', { type: 'radio', name: 'entry-type', value: type.value, onchange: () => refreshCategories() }),
            el('span', { text: type.label }))));
    errors.type = el('p', { class: 'field-error', id: 'entry-type-error', hidden: true });

    const amountHint = el('p', { class: 'field-hint', id: 'entry-amount-hint', text: AMOUNT_HINT });
    const dateHint = el('p', { class: 'field-hint field-hint-planned', id: 'entry-date-hint', hidden: true,
        text: 'Data futura: o lançamento será registrado como previsto.' });

    const amountInput = el('input', { id: 'entry-amount', type: 'text', inputmode: 'decimal', autocomplete: 'off',
        placeholder: '0,00', oninput: () => updateAmountHint() });
    const dateInput = el('input', { id: 'entry-occurredAt', type: 'date', min: MIN_ENTRY_DATE,
        oninput: () => updateStatusLock(), onchange: () => updateStatusLock() });

    const generalError = el('p', { class: 'form-error', role: 'alert', hidden: true });
    const submit = el('button', { type: 'submit', class: 'button button-primary', text: 'Salvar' });
    const title = el('h2', { id: 'entry-dialog-title', text: 'Novo lançamento' });

    const formNode = el('form', { class: 'entry-form', novalidate: true, onsubmit: handleSubmit },
        el('header', { class: 'dialog-header' }, title,
            el('button', { type: 'button', class: 'icon-button', 'aria-label': 'Fechar', text: '×',
                onclick: () => form.dialog.close() })),
        el('div', { class: 'dialog-body' },
            el('fieldset', { class: 'field' }, el('legend', { text: 'Tipo' }), typeGroup, errors.type),
            field('description', 'Descrição', el('input', { id: 'entry-description', type: 'text', maxlength: 255,
                autocomplete: 'off', placeholder: 'Ex.: Mercado da semana' })),
            el('div', { class: 'field-row' },
                field('amount', 'Valor (R$)', el('div', { class: 'money-input' },
                    el('span', { 'aria-hidden': 'true', text: 'R$' }), amountInput), amountHint, amountInput),
                field('occurredAt', 'Data', dateInput, dateHint)),
            el('div', { class: 'field-row' },
                field('category', 'Categoria', el('select', { id: 'entry-category' })),
                field('paymentMethod', 'Meio de pagamento', select('entry-paymentMethod', reference.paymentMethods))),
            el('div', { class: 'field-row' },
                field('recurrence', 'Recorrência', select('entry-recurrence', reference.recurrences),
                    el('p', { class: 'field-hint', id: 'entry-recurrence-hint',
                        text: 'Apenas registra a característica; nenhum lançamento é criado automaticamente.' })),
                field('status', 'Situação', select('entry-status', reference.statuses))),
            field('notes', 'Observação (opcional)', el('textarea', { id: 'entry-notes', maxlength: 500, rows: 2 })),
            generalError),
        el('footer', { class: 'dialog-footer' },
            el('button', { type: 'button', class: 'button button-secondary', text: 'Cancelar',
                onclick: () => form.dialog.close() }),
            submit));

    const dialog = el('dialog', { class: 'dialog entry-dialog', 'aria-labelledby': 'entry-dialog-title' }, formNode);
    document.body.append(dialog);
    return { dialog, inputs, errors, typeGroup, amountHint, dateHint, generalError, submit, title };
}

function select(id, options) {
    return el('select', { id }, ...options.map((option) => el('option', { value: option.value, text: option.label })));
}

function selectedType() {
    return form.typeGroup.querySelector('input:checked')?.value ?? '';
}

function refreshCategories(preferred) {
    const select = form.inputs.category;
    const previous = preferred ?? select.value;
    const options = reference.categoriesFor(selectedType());
    select.replaceChildren(el('option', { value: '', text: 'Selecione' }),
        ...options.map((category) => el('option', { value: category.value, text: category.label, title: category.description })));
    select.value = options.some((category) => category.value === previous) ? previous : '';
}

function updateAmountHint() {
    const cents = parseBrlToCents(form.inputs.amount.value);
    form.amountHint.textContent = cents !== null && cents > 0n
        ? `${formatCentsAsBrl(cents)} será enviado à API como ${cents} centavos.`
        : AMOUNT_HINT;
}

function updateStatusLock() {
    const date = form.inputs.occurredAt.value;
    const future = Boolean(date) && isFuture(date);
    if (future) {
        form.inputs.status.value = 'PLANNED';
    }
    form.inputs.status.disabled = future;
    form.dateHint.hidden = !future;
}

function fillForm(entry) {
    clearErrors();
    form.title.textContent = entry ? 'Editar lançamento' : 'Novo lançamento';
    form.inputs.occurredAt.max = maxEntryDateIso();

    const type = entry?.type ?? 'EXPENSE';
    for (const radio of form.typeGroup.querySelectorAll('input')) {
        radio.checked = radio.value === type;
    }
    form.inputs.description.value = entry?.description ?? '';
    form.inputs.amount.value = entry ? formatCentsForInput(centsFromApi(entry.amountInCents)) : '';
    form.inputs.occurredAt.value = entry?.occurredAt ?? todayIso();
    refreshCategories(entry?.category ?? '');
    form.inputs.paymentMethod.value = entry?.paymentMethod ?? 'PIX';
    form.inputs.recurrence.value = entry?.recurrence ?? 'NONE';
    form.inputs.status.value = entry?.status ?? 'CONFIRMED';
    form.inputs.notes.value = entry?.notes ?? '';
    updateAmountHint();
    updateStatusLock();
}

async function handleSubmit(event) {
    event.preventDefault();
    clearErrors();

    const values = {
        type: selectedType(),
        description: form.inputs.description.value.trim(),
        cents: parseBrlToCents(form.inputs.amount.value),
        occurredAt: form.inputs.occurredAt.value,
        category: form.inputs.category.value,
        paymentMethod: form.inputs.paymentMethod.value,
        recurrence: form.inputs.recurrence.value,
        status: form.inputs.status.value,
        notes: form.inputs.notes.value.trim(),
    };

    const clientErrors = validate(values);
    if (clientErrors.length) {
        showErrors(clientErrors);
        return;
    }

    const body = {
        type: values.type,
        description: values.description,
        amount: centsToJsonNumber(values.cents),
        occurredAt: values.occurredAt,
        category: values.category,
        paymentMethod: values.paymentMethod,
        recurrence: values.recurrence,
        status: values.status,
        notes: values.notes || null,
    };

    form.submit.disabled = true;
    form.submit.textContent = 'Salvando…';
    try {
        const saved = current.entry
            ? await api('PUT', `/transactions/${current.entry.id}`, body)
            : await api('POST', '/transactions', body);
        form.dialog.close();
        current.onSaved?.(saved);
    } catch (error) {
        handleApiError(error);
    } finally {
        form.submit.disabled = false;
        form.submit.textContent = 'Salvar';
    }
}

function validate(values) {
    const errors = [];
    if (!values.type) {
        errors.push({ field: 'type', message: 'Escolha receita ou despesa.' });
    }
    if (!values.description) {
        errors.push({ field: 'description', message: 'Informe uma descrição.' });
    }
    if (values.cents === null) {
        errors.push({ field: 'amount', message: 'Valor inválido. Use o formato 80,00, com vírgula para os centavos.' });
    } else if (values.cents <= 0n) {
        errors.push({ field: 'amount', message: 'O valor deve ser maior que zero.' });
    } else if (values.cents > MAX_CENTS) {
        errors.push({ field: 'amount', message: `O valor máximo é ${formatCentsAsBrl(MAX_CENTS)}.` });
    }
    if (!values.occurredAt) {
        errors.push({ field: 'occurredAt', message: 'Informe a data.' });
    } else if (values.occurredAt < MIN_ENTRY_DATE || values.occurredAt > maxEntryDateIso()) {
        errors.push({ field: 'occurredAt', message: 'A data deve estar entre 01/01/2000 e 12 meses à frente.' });
    }
    if (!values.category) {
        errors.push({ field: 'category', message: 'Selecione uma categoria.' });
    }
    return errors;
}

function handleApiError(error) {
    if (error instanceof ApiError && error.fieldErrors.length) {
        showErrors(error.fieldErrors);
        return;
    }
    form.generalError.textContent = error.message || 'Não foi possível salvar o lançamento.';
    form.generalError.hidden = false;
}

function showErrors(errors) {
    const unknown = [];
    for (const { field, message } of errors) {
        const target = form.errors[field];
        if (target) {
            target.textContent = message;
            target.hidden = false;
            form.inputs[field]?.setAttribute('aria-invalid', 'true');
        } else {
            unknown.push(message);
        }
    }
    if (unknown.length) {
        form.generalError.textContent = unknown.join(' ');
        form.generalError.hidden = false;
    }
    const first = errors.map((e) => form.inputs[e.field]).find(Boolean);
    first?.focus();
}

function clearErrors() {
    for (const name of FIELDS) {
        form.errors[name].hidden = true;
        form.errors[name].textContent = '';
        form.inputs[name]?.removeAttribute('aria-invalid');
    }
    form.generalError.hidden = true;
}

// ---------- Excluir e confirmar ----------

let confirmDialog;

function buildConfirmDialog() {
    const title = el('h2', { id: 'confirm-dialog-title' });
    const text = el('p', { class: 'confirm-text' });
    const summary = el('dl', { class: 'confirm-summary' });
    const error = el('p', { class: 'form-error', role: 'alert', hidden: true });
    const confirm = el('button', { type: 'button', class: 'button' });
    const dialog = el('dialog', { class: 'dialog confirm-dialog', 'aria-labelledby': 'confirm-dialog-title' },
        el('div', { class: 'dialog-body' }, title, text, summary, error),
        el('footer', { class: 'dialog-footer' },
            el('button', { type: 'button', class: 'button button-secondary', text: 'Cancelar', onclick: () => dialog.close() }),
            confirm));
    document.body.append(dialog);
    return { dialog, title, text, summary, error, confirm };
}

function openConfirm({ titleText, message, entry, buttonText, buttonClass, action, onDone }) {
    confirmDialog ??= buildConfirmDialog();
    const { dialog, title, text, summary, error, confirm } = confirmDialog;
    title.textContent = titleText;
    text.textContent = message;
    summary.replaceChildren(
        el('dt', { text: 'Descrição' }), el('dd', { text: entry.description }),
        el('dt', { text: 'Valor' }), el('dd', { text: money(entry.amountInCents) }),
        el('dt', { text: 'Data' }), el('dd', { text: formatDate(entry.occurredAt) }),
        el('dt', { text: 'Categoria' }), el('dd', { text: reference.categoryLabel(entry.category) }));
    error.hidden = true;
    confirm.className = `button ${buttonClass}`;
    confirm.textContent = buttonText;
    confirm.disabled = false;
    confirm.onclick = async () => {
        confirm.disabled = true;
        try {
            await action();
            dialog.close();
            onDone?.();
        } catch (e) {
            error.textContent = e.message || 'Não foi possível concluir a ação.';
            error.hidden = false;
            confirm.disabled = false;
        }
    };
    dialog.showModal();
    dialog.querySelector('.button-secondary').focus();
}

export function openDeleteDialog(entry, onDeleted) {
    openConfirm({
        titleText: 'Excluir lançamento?',
        message: 'Esta ação não pode ser desfeita.',
        entry,
        buttonText: 'Excluir definitivamente',
        buttonClass: 'button-danger',
        action: () => api('DELETE', `/transactions/${entry.id}`),
        onDone: onDeleted,
    });
}

export function openConfirmPlannedDialog(entry, onConfirmed) {
    openConfirm({
        titleText: 'Marcar como realizado?',
        message: 'O lançamento previsto passará a contar nos valores realizados.',
        entry,
        buttonText: 'Marcar como realizado',
        buttonClass: 'button-primary',
        action: () => api('PUT', `/transactions/${entry.id}`, {
            type: entry.type,
            description: entry.description,
            amount: entry.amountInCents,
            occurredAt: entry.occurredAt,
            category: entry.category,
            paymentMethod: entry.paymentMethod,
            recurrence: entry.recurrence,
            status: 'CONFIRMED',
            notes: entry.notes,
        }),
        onDone: onConfirmed,
    });
}
