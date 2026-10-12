// SPA local baseada em hash: #/painel e #/lancamentos. Sem framework; os dados vêm da API REST da mesma origem.

import { api } from './api.js';
import { renderDashboard } from './dashboard.js';
import { renderEntries } from './entries.js';
import { openEntryDialog } from './entry-form.js';
import { reference, setReference } from './reference.js';
import { errorState, showToast } from './ui.js';

const ROUTES = { painel: renderDashboard, lancamentos: renderEntries };
const root = document.getElementById('app');

const ctx = {
    openCreate: () => openEntryDialog({
        onSaved: () => {
            showToast('Lançamento salvo.');
            render();
        },
    }),
    rerender: () => render(),
};

boot();

async function boot() {
    try {
        setReference(await api('GET', '/reference-data'));
    } catch (error) {
        root.replaceChildren(errorState(error));
        return;
    }
    document.getElementById('new-entry-button').addEventListener('click', ctx.openCreate);
    document.getElementById('new-entry-button').disabled = false;
    markFeatureTabs();
    window.addEventListener('hashchange', render);
    render();
}

function parseRoute() {
    const [path, query = ''] = location.hash.replace(/^#\/?/, '').split('?');
    return { name: ROUTES[path] ? path : 'painel', params: new URLSearchParams(query) };
}

function render() {
    const { name, params } = parseRoute();
    for (const link of document.querySelectorAll('[data-route]')) {
        if (link.dataset.route === name) {
            link.setAttribute('aria-current', 'page');
        } else {
            link.removeAttribute('aria-current');
        }
    }
    ROUTES[name](root, params, ctx);
}

function markFeatureTabs() {
    const { assistant, voice } = reference.features;
    document.querySelector('#tab-assistant small').textContent = assistant ? 'habilitado' : 'indisponível';
    document.querySelector('#tab-voice small').textContent = voice ? 'habilitado' : 'indisponível';
}
