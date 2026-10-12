// Datas como texto ISO (AAAA-MM-DD), comparadas lexicalmente, sem conversões de fuso.

export const MIN_ENTRY_DATE = '2000-01-01';

export function isoDate(year, month, day) {
    return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/** Data de hoje no relógio do navegador. */
export function todayIso() {
    const now = new Date();
    return isoDate(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

/** Hoje + 12 meses (limite das datas futuras), ajustando 29/02 para o último dia do mês. */
export function maxEntryDateIso() {
    const [year, month, day] = todayIso().split('-').map(Number);
    const lastDay = new Date(year + 1, month, 0).getDate();
    return isoDate(year + 1, month, Math.min(day, lastDay));
}

export function isFuture(iso) {
    return iso > todayIso();
}

/** "2026-10-12" → "12/10/2026". */
export function formatDate(iso) {
    if (!iso) {
        return '';
    }
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
}

export function formatRange(period) {
    return `${formatDate(period.start)} a ${formatDate(period.end)}`;
}
