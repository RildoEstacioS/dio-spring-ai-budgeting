// Dados de referência carregados de GET /reference-data (rótulos pt-BR, opções e recursos habilitados).

let data = null;

export function setReference(value) {
    data = value;
}

function labelIn(list, value) {
    return list.find((item) => item.value === value)?.label ?? value;
}

export const reference = {
    get types() {
        return data.types;
    },
    get paymentMethods() {
        return data.paymentMethods;
    },
    get recurrences() {
        return data.recurrences;
    },
    get statuses() {
        return data.statuses;
    },
    get features() {
        return data.features;
    },
    categoriesFor(type) {
        return data.categories.filter((category) => !type || category.type === type);
    },
    category(value) {
        return data.categories.find((category) => category.value === value);
    },
    categoryLabel(value) {
        return labelIn(data.categories, value);
    },
    typeLabel(value) {
        return labelIn(data.types, value);
    },
    paymentLabel(value) {
        return labelIn(data.paymentMethods, value);
    },
    recurrenceLabel(value) {
        return labelIn(data.recurrences, value);
    },
    statusLabel(value) {
        return labelIn(data.statuses, value);
    },
};
